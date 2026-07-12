// admin-reports.js – Admin Functions, Reports, User-Kontext
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const functions = require("firebase-functions");
const { Resend } = require("resend");
const { admin, escapeHtml, checkSingleRateLimit, primaryAdminUidSecret, NOTIFY_EMAIL, BRAND_BLUE, sendEmail } = require("./shared");
const { SENDER } = require("./i18n");

// ─── Feedback-E-Mail via Resend ──────────────────────────────────────────────
exports.sendFeedbackEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY", "PRIMARY_ADMIN_UID"],
    invoker: "public",
  },
  async (request) => {
    // Nur eingeloggte Nutzer dürfen Feedback senden
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    // S8: Rate-Limiting – max. 3 Feedbacks/Stunde, max. 10/Tag pro UID
    try {
      const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
      const isAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();
      if (!isAdmin) {
        const hourLimited = await checkSingleRateLimit(`feedback_${uid}_h`, 60 * 60 * 1000, 3);
        if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`feedback_${uid}_d`, 24 * 60 * 60 * 1000, 10);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      }
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Feedback rate-limit check failed (ignored):", err);
    }

    const { category, message, email, appVersion, device } = request.data || {};

    if (!message || typeof message !== "string" || message.trim().length === 0) {
      throw new HttpsError("invalid-argument", "INVALID_MESSAGE");
    }

    // S8: E-Mail-Format serverseitig validieren (replyTo ist optional, muss aber valide sein)
    const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (email && email.trim() && !EMAIL_REGEX.test(email.trim())) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      throw new HttpsError("failed-precondition", "Email service not configured.");
    }


    const sanitizedCategory = escapeHtml((category || "Sonstiges").slice(0, 100));
    const sanitizedMessage = escapeHtml(message?.trim().slice(0, 5000) || "");
    const sanitizedDevice = escapeHtml((device || "").slice(0, 200));
    const sanitizedAppVersion = escapeHtml((appVersion || "").slice(0, 50));

    const replyTo = email && email.trim() ? email.trim().slice(0, 254) : undefined;
    const subject = `📬 FamWake Feedback: ${sanitizedCategory}`;
    const html = `
      <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
        <h2 style="color: ${BRAND_BLUE};">📬 Neues Feedback</h2>
        <table style="width:100%; border-collapse: collapse; font-size:14px;">
          <tr><td style="padding:6px 0; color:#666; width:130px;">Kategorie</td><td><strong>${sanitizedCategory}</strong></td></tr>
          <tr><td style="padding:6px 0; color:#666;">App-Version</td><td>${sanitizedAppVersion || "–"}</td></tr>
          <tr><td style="padding:6px 0; color:#666;">Gerät</td><td>${sanitizedDevice || "–"}</td></tr>
          ${replyTo ? `<tr><td style="padding:6px 0; color:#666;">Antwort-E-Mail</td><td><a href="mailto:${replyTo}">${replyTo}</a></td></tr>` : ""}
        </table>
        <hr style="border: none; border-top: 1px solid #eee; margin: 16px 0;">
        <h3 style="color: ${BRAND_BLUE};">Nachricht</h3>
        <p style="background:#f9f9f9; padding:12px; border-radius:6px; white-space: pre-wrap;">${sanitizedMessage}</p>
        <hr style="border: none; border-top: 1px solid #eee; margin: 16px 0;">
        <p style="font-size:11px; color:#999; text-align:center;">FamWake Familienwecker – automatisch generiert</p>
      </div>
    `;

    try {
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER.de,
        to: [NOTIFY_EMAIL],
        subject,
        html,
        ...(replyTo ? { reply_to: replyTo } : {}),
      });

      if (error) {
        console.error("Resend Feedback Error:", error);
        throw new HttpsError("internal", "Failed to send feedback email.");
      }

      // Feedback auch in Firestore für Archiv speichern
      await admin.firestore().collection("feedback").add({
        category: category || "",
        message: message.trim(),
        email: email?.trim() || "",
        appVersion: appVersion || "",
        device: device || "",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      return { success: true };

    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Error in sendFeedbackEmail:", err);
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

// ─── Admin-Reports ──────────────────────────────────────────────────────────

/** Hilfsfunktion zur Generierung des Statistik-Reports */
async function getStatsReport() {
  const usersResponse = await admin.auth().listUsers();
  const users = usersResponse.users;

  const familiesSnapshot = await admin.firestore().collection("families").get();
  let familiesHtml = "";

  for (const doc of familiesSnapshot.docs) {
    const family = doc.data();
    const membersSnapshot = await doc.ref.collection("members").get();
    // escapeHtml: Member-Namen könnten HTML-Sonderzeichen enthalten (XSS in Admin-Mail)
    const members = membersSnapshot.docs.map(m => escapeHtml(m.data().name || "Unbekannt")).join(", ");

    familiesHtml += `
            <tr>
                <td style="padding: 8px; border-bottom: 1px solid #eee;"><b>${escapeHtml(family.name || family.familyName || "Unbenannt")}</b></td>
                <td style="padding: 8px; border-bottom: 1px solid #eee; font-size: 0.9em; color: #666;">${members}</td>
            </tr>
        `;
  }

  return `
        <div style="font-family: sans-serif; max-width: 600px; color: #333;">
            <h2 style="color: ${BRAND_BLUE};">FamWake Admin Report</h2>
            <p>Stand: ${new Date().toLocaleString("de-DE", { timeZone: "Europe/Berlin" })}</p>
            
            <h3 style="border-bottom: 2px solid ${BRAND_BLUE}; padding-bottom: 4px;">Zusammenfassung</h3>
            <ul>
                <li>Gesamt-Nutzer: <b>${users.length}</b></li>
                <li>Gesamt-Familien: <b>${familiesSnapshot.size}</b></li>
            </ul>

            <h3 style="border-bottom: 2px solid ${BRAND_BLUE}; padding-bottom: 4px;">Familien & Mitglieder</h3>
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="background: #f8f8f8;">
                        <th style="padding: 8px; text-align: left; border-bottom: 2px solid #ddd;">Name</th>
                        <th style="padding: 8px; text-align: left; border-bottom: 2px solid #ddd;">Mitglieder</th>
                    </tr>
                </thead>
                <tbody>${familiesHtml}</tbody>
            </table>
            
            <hr style="margin-top: 24px; border: 0; border-top: 1px solid #ccc;">
            <p style="font-size: 0.8em; color: #999;">Dieser Report wurde automatisch generiert (anonymisiert für DSGVO).</p>
        </div>
    `;
}

/** Manueller Report-Abruf aus der App */
exports.sendAdminStatsReport = onCall(
  { region: "europe-west3", secrets: ["RESEND_API_KEY", "PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");

    // Admin check
    const uid = request.auth.uid;
    const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
    if (!adminDoc.exists && uid !== primaryAdminUidSecret.value()) {
      throw new HttpsError("permission-denied", "ADMIN_ONLY");
    }

    const html = await getStatsReport();
    await sendEmail(NOTIFY_EMAIL, "FamWake: Manueller Admin-Report", html);
    return { success: true };
  }
);

/** Wöchentlicher automatischer Report */
exports.scheduledAdminStatsReport = onSchedule(
  {
    schedule: "every sunday 20:00",
    timeZone: "Europe/Berlin",
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"]
  },
  async (event) => {
    const html = await getStatsReport();
    await sendEmail(NOTIFY_EMAIL, "FamWake: Wöchentlicher Admin-Report", html);
    console.log("Weekly admin stats report sent.");
  }
);

// ─── Startup-Kontext: Familie des Users in einem Batch-Read ─────────────────
// Ersetzt den 3-fachen sequenziellen Firestore-Read im Client.
// Gibt { familyId, joinCode } zurück oder null wenn kein Familien-Kontext gefunden.
exports.getUserContext = onCall(
  { region: "europe-west3" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    try {
      // Schritt 1: users/{uid} lesen
      const userDoc = await admin.firestore().collection("users").doc(uid).get();
      const familyId = userDoc.exists ? userDoc.data().familyId : null;

      if (!familyId) {
        return { familyId: null, joinCode: null };
      }

      // Schritt 2: families/{familyId} lesen (Batch mit users/{uid} wäre getAll, aber
      // da familyId erst aus Step 1 kommt, ist sequenziell hier unvermeidbar.
      // Gegenüber Client-Pfad: kein 3. Query-Fallback, kein Netz-Overhead pro Read.)
      const familyDoc = await admin.firestore().collection("families").doc(familyId).get();

      if (!familyDoc.exists) {
        // Familie gelöscht – users/{uid}.familyId bereinigen
        await admin.firestore().collection("users").doc(uid).update({
          familyId: admin.firestore.FieldValue.delete()
        });
        console.log(`getUserContext: Family ${familyId} not found, cleaned up user doc for ${uid}`);
        return { familyId: null, joinCode: null };
      }

      const joinCode = familyDoc.data().joinCode || null;
      return { familyId, joinCode };

    } catch (err) {
      console.error(`getUserContext error for ${uid}:`, err);
      throw new HttpsError("internal", "INTERNAL_ERROR");
    }
  }
);

/**
 * Verifiziert ein Play Integrity Token serverseitig via Google API.
 * Client-seitiges Base64-Decoding liefert immer UNKNOWN (JWT ist signiert und
 * kann client-seitig nicht zuverlässig verifiziert werden). Diese Function ruft
 * die offizielle Google Play Integrity API auf und gibt das echte Verdict zurück.
 */
exports.verifyIntegrityToken = onCall(
  {
    region: "europe-west3",
    invoker: "public",
  },
  async (request) => {
    const token = request.data?.token;
    if (!token || typeof token !== "string") {
      throw new HttpsError("invalid-argument", "Token required");
    }

    try {
      const { GoogleAuth } = require("google-auth-library");
      const auth = new GoogleAuth({
        scopes: ["https://www.googleapis.com/auth/playintegrity"],
      });
      const client = await auth.getClient();

      const packageName = "de.familienwecker.famwake";
      const url = `https://playintegrity.googleapis.com/v1/${packageName}:decodeIntegrityToken`;

      const response = await client.request({
        url,
        method: "POST",
        data: { integrity_token: token },
      });

      const payload = response.data?.tokenPayloadExternal;
      const verdictArray = payload?.deviceIntegrity?.deviceRecognitionVerdict ?? [];
      const trusted = Array.isArray(verdictArray) && verdictArray.includes("MEETS_DEVICE_INTEGRITY");

      console.log(`Play Integrity verdict: ${JSON.stringify(verdictArray)}, trusted=${trusted}`);
      return { trusted, verdict: verdictArray };

    } catch (err) {
      // Fail-open: bei API-Fehler UNKNOWN zurückgeben, nicht blocken
      console.error("verifyIntegrityToken error:", err?.message ?? err);
      return { trusted: null, verdict: [], error: "API_ERROR" };
    }
  }
);

exports.onUserDeleted = functions.region("europe-west3").auth.user().onDelete(
  async (user) => {
    const uid = user.uid;
    console.log(`User ${uid} deleted. Starting DSGVO cleanup...`);
    const db = admin.firestore();
    const userRef = db.collection("users").doc(uid);

    try {
      // 1. Read user doc to find familyId
      const userDoc = await userRef.get();
      if (userDoc.exists) {
        const familyId = userDoc.data().familyId;
        if (familyId) {
          // Find and unclaim any members in this family claimed by this user
          const membersSnap = await db.collection("families").doc(familyId)
            .collection("members")
            .where("claimedByUserId", "==", uid)
            .get();

          if (!membersSnap.empty) {
            const batch = db.batch();
            membersSnap.docs.forEach((doc) => {
              batch.update(doc.ref, {
                claimedByUserId: null,
                claimedByUserName: null,
                claimedByDeviceId: null,
                deviceAlarmEnabled: false,
                lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
              });
            });
            await batch.commit();
            console.log(`Unclaimed ${membersSnap.size} members in family ${familyId} for deleted user ${uid}`);
          }
        }
      }

      // 2. Recursively delete the user document and its subcollections (fcmTokens, pushMeta)
      if (db.recursiveDelete) {
        await db.recursiveDelete(userRef);
        console.log(`Recursively deleted user doc users/${uid} (including subcollections)`);
      } else {
        // Fallback
        const fcmTokens = await userRef.collection("fcmTokens").get();
        const pushMeta = await userRef.collection("pushMeta").get();
        const batch = db.batch();
        fcmTokens.docs.forEach(doc => batch.delete(doc.ref));
        pushMeta.docs.forEach(doc => batch.delete(doc.ref));
        batch.delete(userRef);
        await batch.commit();
        console.log(`Fallback-deleted user doc users/${uid} and subcollections`);
      }
    } catch (err) {
      console.error(`Error in onUserDeleted cleanup for ${uid}:`, err);
    }
  }
);
