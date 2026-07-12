// shared.js – Firebase Admin Init, Konstanten, Helpers
const { HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { Resend } = require("resend");

// Einmaliger Init – wird beim ersten require() ausgeführt
admin.initializeApp();

const db = admin.firestore();

// Zirkuläre Abhängigkeit vermeiden: i18n wird lazy geladen
let _i18n;
function getI18n() {
  if (!_i18n) _i18n = require("./i18n");
  return _i18n;
}

/**
 * Escapes HTML special characters to prevent XSS.
 */
function escapeHtml(unsafe) {
  if (!unsafe || typeof unsafe !== "string") return "";
  return unsafe
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

/**
 * Normalisiert einen Sprach-String auf einen unterstützten IETF-Code.
 * Dialekte (gsw, swg, ksh) werden auf "de" gemappt; unbekannte Sprachen auf "en".
 */
function resolveLanguage(rawInput) {
  const { SUPPORTED_LANGS, DIALECT_TO_LANG } = getI18n();
  const raw = (rawInput || "de").toLowerCase();
  const requested = DIALECT_TO_LANG[raw] || raw.slice(0, 2);
  // Spezifischer Check für zh-CN
  if (raw === "zh-cn" || raw === "zh") return "zh-CN";
  return SUPPORTED_LANGS.includes(requested) ? requested : "en";
}

/**
 * Prüft ein einzelnes Rate-Limit-Fenster.
 * Gibt true zurück wenn das Limit erreicht ist, false wenn OK (und Zähler wird erhöht).
 */
async function checkSingleRateLimit(key, windowMs, maxAttempts) {
  const ref = admin.firestore().collection("_rate_limits").doc(key);
  const now = Date.now();
  return admin.firestore().runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    const data = doc.exists ? doc.data() : { count: 0, windowStart: now };
    if (now - data.windowStart > windowMs) {
      tx.set(ref, { count: 1, windowStart: now });
      return false;
    }
    if (data.count >= maxAttempts) return true;
    tx.set(ref, { count: data.count + 1, windowStart: data.windowStart }, { merge: true });
    return false;
  });
}

/**
 * Dual Rate-Limit auf E-Mail-Adresse, getrennt nach Typ (reset/verify/confirm).
 * Verhindert dass Verifikations-Mails das Reset-Limit beeinflussen.
 * max. 5 Versuche pro Stunde UND max. 10 pro Tag – je Typ unabhängig.
 */
async function checkEmailRateLimit(email, type = "email") {
  // Admin-Bypass über _admins-Collection (sicher, da serverseitig)
  const adminSnapshot = await admin.firestore()
    .collection("_admins")
    .where("email", "==", email.toLowerCase().trim())
    .limit(1)
    .get();

  if (!adminSnapshot.empty) {
    console.log(`Bypassing email rate limit for admin: ${email}`);
    return;
  }

  // Typ-spezifischer Key: reset/verify/confirm haben unabhängige Zähler
  const emailKey = email.toLowerCase().replace(/[^a-z0-9]/g, "_").slice(0, 70);
  const key = `${type}_${emailKey}`;

  // Stunden-Limit
  const hourLimited = await checkSingleRateLimit(`${key}_h`, 60 * 60 * 1000, 5);
  if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");

  // Tages-Limit
  const dayLimited = await checkSingleRateLimit(`${key}_d`, 24 * 60 * 60 * 1000, 10);
  if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
}

const NOTIFY_EMAIL = "daniel.notthoff@gmail.com";
// Security: PRIMARY_ADMIN_UID aus Firebase Secret Manager – nicht hartkodiert.
const primaryAdminUidSecret = defineSecret("PRIMARY_ADMIN_UID");
const BRAND_BLUE = "#1A3A5C";

/** Hilfsfunktion zum Mail-Versand via Resend (mit Secret-Handling) */
async function sendEmail(to, subject, html) {
  const { SENDER } = getI18n();
  const resendKey = process.env.RESEND_API_KEY;
  if (!resendKey) {
    console.error("RESEND_API_KEY secret is not set.");
    return;
  }
  const resend = new Resend(resendKey);
  const { error } = await resend.emails.send({
    from: SENDER.de,
    to: [to],
    subject: subject,
    html: html,
  });
  if (error) {
    console.error(`Fehler beim Senden der E-Mail an ${to}:`, error);
  }
}

/**
 * Liest alle FCM-Tokens eines Users und sendet eine Multicast-Nachricht.
 * Entfernt automatisch abgelaufene Tokens (HTTP 404 von FCM → Token ungültig).
 */
async function sendPushToUser(uid, payload) {
  const tokensSnap = await admin.firestore()
    .collection("users").doc(uid)
    .collection("fcmTokens")
    .get();

  if (tokensSnap.empty) return;

  // Token ist als Feld gespeichert (Dokument-ID ist SHA-256-Hash, da FCM-Token ':' enthält)
  const tokenDocs = tokensSnap.docs
    .map(d => ({ token: d.data().token, docId: d.id }))
    .filter(t => !!t.token);
  const tokens = tokenDocs.map(t => t.token);

  if (tokens.length === 0) return;

  let titleLocKey = "";
  let bodyLocKey = "";
  if (payload.type === "schedule_change") {
    titleLocKey = "notif_schedule_changed_title";
    bodyLocKey = "notif_schedule_changed_body";
  } else if (payload.type === "family_joined") {
    titleLocKey = "notif_member_joined_title";
    bodyLocKey = "notif_member_joined_body";
  } else if (payload.type === "family_left") {
    titleLocKey = "notif_member_left_title";
    bodyLocKey = "notif_member_left_body";
  }

  const message = {
    tokens,
    data: {
      type: payload.type || "info",
      title: payload.title || "",
      body: payload.body || "",
    },
    android: { priority: "high" },
    apns: {
      headers: {
        "apns-priority": "10"
      },
      payload: {
        aps: {
          alert: titleLocKey ? {
            "title-loc-key": titleLocKey,
            "loc-key": bodyLocKey
          } : undefined,
          "content-available": 1
        }
      }
    }
  };

  const response = await admin.messaging().sendEachForMulticast(message);

  // Abgelaufene Tokens direkt löschen – reduziert Firestore-Reads beim nächsten Push
  const deleteOps = [];
  response.responses.forEach((res, idx) => {
    if (!res.success) {
      const code = res.error?.code || "";
      if (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      ) {
        deleteOps.push(
          admin.firestore()
            .collection("users").doc(uid)
            .collection("fcmTokens").doc(tokenDocs[idx].docId)
            .delete()
        );
      }
    }
  });
  if (deleteOps.length > 0) await Promise.all(deleteOps);
}

async function sendPushToUsers(uids, payload) {
  await Promise.all(uids.map(uid => sendPushToUser(uid, payload)));
}

module.exports = {
  admin,
  db,
  escapeHtml,
  resolveLanguage,
  checkSingleRateLimit,
  checkEmailRateLimit,
  NOTIFY_EMAIL,
  BRAND_BLUE,
  primaryAdminUidSecret,
  sendEmail,
  sendPushToUser,
  sendPushToUsers,
};
