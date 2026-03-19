const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { Resend } = require("resend");
const { randomInt } = require("crypto");

admin.initializeApp();

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
 * Dual Rate-Limit auf E-Mail-Adresse:
 * max. 5 Versuche pro Stunde UND max. 10 pro Tag.
 */
async function checkEmailRateLimit(email) {
  const key = `email_${email.toLowerCase().replace(/[^a-z0-9]/g, "_").slice(0, 80)}`;

  // Stunden-Limit
  const hourLimited = await checkSingleRateLimit(`${key}_h`, 60 * 60 * 1000, 5);
  if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");

  // Tages-Limit (2× stündliches Limit)
  const dayLimited = await checkSingleRateLimit(`${key}_d`, 24 * 60 * 60 * 1000, 10);
  if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
}

const NOTIFY_EMAIL = "daniel.notthoff@gmail.com";
const BRAND_BLUE = "#1A3A5C";

const SENDER = {
  de: "FamWake Familienwecker <no-reply@familienwecker.de>",
  en: "FamWake Family Alarm <no-reply@familienwecker.de>",
};

const EMAIL_CONTENT = {
  de: {
    subject: "🔑 Passwort zurücksetzen – FamWake Familienwecker",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "Hallo!",
    intro: "Wir haben eine Anfrage erhalten, das Passwort für dein <strong>FamWake</strong> Familienwecker-Konto zurückzusetzen.",
    instruction: "Klicke auf den folgenden Button, um ein neues Passwort zu vergeben:",
    button: "Passwort zurücksetzen",
    fallback: "Falls der Button nicht funktioniert, kopiere bitte diesen Link in deinen Browser:",
    security: "⚠️ Falls du diese Anfrage <strong>nicht</strong> gestellt hast, kannst du diese E-Mail einfach ignorieren. Dein Passwort bleibt unverändert. Solltest du verdächtige Aktivitäten bemerken, wende dich bitte an: daniel.notthoff@gmail.com",
    footerNote: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "🔑 Reset your password – FamWake Family Alarm",
    appName: "<strong>FamWake</strong> Family Alarm",
    greeting: "Hello!",
    intro: "We received a request to reset the password for your <strong>FamWake</strong> Family Alarm account.",
    instruction: "Click the button below to set a new password:",
    button: "Reset Password",
    fallback: "If the button doesn't work, please paste this link into your browser:",
    security: "⚠️ If you did <strong>not</strong> request this, you can safely ignore this email. Your password will remain unchanged. If you notice any suspicious activity, please contact us at: daniel.notthoff@gmail.com",
    footerNote: "This is an automated message. Please do not reply directly to this email.",
  },
};

const EMAIL_CONTENT_CONFIRM = {
  de: {
    subject: "✅ Passwort erfolgreich geändert – FamWake Familienwecker",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "Hallo!",
    intro: "Dein Passwort für dein <strong>FamWake</strong> Familienwecker-Konto wurde erfolgreich geändert.",
    instruction: "Du kannst dich ab sofort mit deinem neuen Passwort in der App anmelden.",
    security: "⚠️ Falls du dein Passwort <strong>nicht</strong> geändert hast, wende dich bitte umgehend an uns: daniel.notthoff@gmail.com",
    footerNote: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "✅ Password successfully changed – FamWake Family Alarm",
    appName: "<strong>FamWake</strong> Family Alarm",
    greeting: "Hello!",
    intro: "The password for your <strong>FamWake</strong> Family Alarm account has been successfully changed.",
    instruction: "You can now log in to the app with your new password.",
    security: "⚠️ If you did <strong>not</strong> change your password, please contact us immediately: daniel.notthoff@gmail.com",
    footerNote: "This is an automated message. Please do not reply directly to this email.",
  },
};

function buildEmailHtml(link, lang) {
  const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.de;
  const isDE = lang !== "en";
  return `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: ${BRAND_BLUE};">${t.greeting}</h2>
      <p>${t.intro}</p>
      <p>${t.instruction}</p>
      <div style="text-align: center; margin: 30px 0;">
        <a href="${link}" style="background-color: ${BRAND_BLUE}; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 15px;">${t.button}</a>
      </div>
      <p style="font-size: 12px; color: #666;">${t.fallback}</p>
      <p style="font-size: 12px; color: #888; word-break: break-all;">${link}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 12px; color: #888; background-color: #f9f9f9; padding: 12px; border-radius: 6px;">${t.security}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 11px; color: #999; text-align: center;">
        <strong>${t.appName}</strong><br>
        Daniel Notthoff<br>
        Rudolf-Virchow-Str. 20, 58300 Wetter<br><br>
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${isDE ? "Startseite" : "Website"}</a> &nbsp;|&nbsp;
        <a href="https://www.familienwecker.de/privacy-policy.html" style="color: #999; text-decoration: none;">${isDE ? "Datenschutzerklärung" : "Privacy Policy"}</a> &nbsp;|&nbsp;
        <a href="https://www.familienwecker.de/imprint.html" style="color: #999; text-decoration: none;">${isDE ? "Impressum" : "Legal Notice"}</a><br><br>
        ${t.footerNote}
      </p>
    </div>
  `;
}

exports.sendBrandedResetEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"],
    invoker: "public",
  },
  async (request) => {
    const email = request.data?.email;
    const lang = (request.data?.language || "de").startsWith("en") ? "en" : "de";

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim());
      const linkOriginal = await admin.auth().generatePasswordResetLink(email.trim());
      let link = linkOriginal.replace("deine-domain.de", "www.familienwecker.de");

      // Dynamically switch to English HTML page if language is set to English
      if (lang === "en") {
        link = link.replace("reset-password.html", "reset-password-en.html");
      }

      const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.de;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.de,
        to: [email.trim()],
        subject: t.subject,
        html: buildEmailHtml(link, lang),
      });

      if (error) {
        console.error("Resend Error:", error);
        throw new HttpsError("internal", "Failed to send email via Resend.");
      }

      return { success: true };

    } catch (err) {
      if (err instanceof HttpsError) throw err;

      console.error("Error in sendBrandedResetEmail:", err);
      // Accessing error code from Firebase Admin SDK error
      const code = String(err.code || (err.errorInfo && err.errorInfo.code) || "");
      console.log("Extracted error code:", code);

      if (code.includes("invalid-email") || code.includes("argument")) {
        throw new HttpsError("invalid-argument", "INVALID_EMAIL");
      } else if (code.includes("user-not-found") || code.includes("not-found")) {
        console.log("Mapping to USER_NOT_FOUND");
        throw new HttpsError("not-found", "USER_NOT_FOUND");
      } else if (code.includes("too-many-requests") || code.includes("resource-exhausted")) {
        throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      }
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

function buildConfirmEmailHtml(lang) {
  const t = EMAIL_CONTENT_CONFIRM[lang] || EMAIL_CONTENT_CONFIRM.de;
  const isDE = lang !== "en";
  return `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: ${BRAND_BLUE};">${t.greeting}</h2>
      <p>${t.intro}</p>
      <p>${t.instruction}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 12px; color: #888; background-color: #f9f9f9; padding: 12px; border-radius: 6px;">${t.security}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 11px; color: #999; text-align: center;">
        <strong>${t.appName}</strong><br>
        Daniel Notthoff<br>
        Rudolf-Virchow-Str. 20, 58300 Wetter<br><br>
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${isDE ? "Startseite" : "Website"}</a> &nbsp;|&nbsp;
        <a href="https://www.familienwecker.de/privacy-policy.html" style="color: #999; text-decoration: none;">${isDE ? "Datenschutzerklärung" : "Privacy Policy"}</a> &nbsp;|&nbsp;
        <a href="https://www.familienwecker.de/imprint.html" style="color: #999; text-decoration: none;">${isDE ? "Impressum" : "Legal Notice"}</a><br><br>
        ${t.footerNote}
      </p>
    </div>
  `;
}

exports.sendBrandedConfirmationEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"],
    invoker: "public",
  },
  async (request) => {
    const email = request.data?.email;
    const lang = (request.data?.language || "de").startsWith("en") ? "en" : "de";

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim());
      const t = EMAIL_CONTENT_CONFIRM[lang] || EMAIL_CONTENT_CONFIRM.de;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.de,
        to: [email.trim()],
        subject: t.subject,
        html: buildConfirmEmailHtml(lang),
      });

      if (error) {
        console.error("Resend Error:", error);
        throw new HttpsError("internal", "Failed to send confirmation email via Resend.");
      }

      return { success: true };

    } catch (err) {
      console.error("Error in sendBrandedConfirmationEmail:", err);
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

const EMAIL_CONTENT_VERIFY = {
  de: {
    subject: "🚀 Bestätige dein FamWake Familienwecker-Konto",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "Willkommen bei FamWake Familienwecker!",
    intro: "Vielen Dank für deine Registrierung bei <strong>FamWake</strong> Familienwecker. Wir freuen uns darauf, dir und deiner Familie zu einem entspannten Morgen ohne Chaos zu verhelfen!",
    instruction: "Bitte bestätige deine E-Mail-Adresse, um dein Konto zu aktivieren:",
    button: "E-Mail-Adresse bestätigen",
    fallback: "Falls der Button nicht funktioniert, kopiere bitte diesen Link in deinen Browser:",
    privacy: "<strong>Hinweis:</strong> Aus Datenschutzgründen werden dieser Link und deine unbestätigten Registrierungsdaten automatisch nach 48 Stunden gelöscht, falls keine Aktivierung erfolgt.",
    security: "Falls du dieses Konto nicht erstellt hast, kannst du diese E-Mail einfach ignorieren. Es werden keine Daten dauerhaft von dir gespeichert.",
    footerNote: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "🚀 Confirm your FamWake Family Alarm account",
    appName: "<strong>FamWake</strong> Family Alarm",
    greeting: "Welcome to FamWake Family Alarm!",
    intro: "Thank you for registering with <strong>FamWake</strong> Family Alarm. We look forward to helping you and your family start the day stress-free!",
    instruction: "Please confirm your email address to activate your account:",
    button: "Confirm email address",
    fallback: "If the button doesn't work, please paste this link into your browser:",
    privacy: "<strong>Note:</strong> For privacy reasons, this link and your unconfirmed registration data will be automatically deleted after 48 hours if no activation occurs.",
    security: "If you did not create this account, you can safely ignore this email. No data will be permanently stored.",
    footerNote: "This is an automated message. Please do not reply directly to this email.",
  },
};

function buildVerifyEmailHtml(link, lang) {
  const t = EMAIL_CONTENT_VERIFY[lang] || EMAIL_CONTENT_VERIFY.de;
  const isDE = lang !== "en";
  return `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: ${BRAND_BLUE};">${t.greeting}</h2>
      <p>${t.intro}</p>
      <p>${t.instruction}</p>
      <div style="text-align: center; margin: 30px 0;">
        <a href="${link}" style="background-color: ${BRAND_BLUE}; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 15px;">${t.button}</a>
      </div>
      <p style="font-size: 12px; color: #666;">${t.fallback}</p>
      <p style="font-size: 12px; color: #888; word-break: break-all;">${link}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 12px; color: #888; background-color: #f9f9f9; padding: 12px; border-radius: 6px;">${t.privacy}</p>
      <p style="font-size: 12px; color: #888;">${t.security}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 11px; color: #999; text-align: center;">
        <strong>${t.appName}</strong><br>
        Daniel Notthoff<br>
        Rudolf-Virchow-Str. 20, 58300 Wetter<br><br>
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${isDE ? "Startseite" : "Website"}</a> &nbsp;|&nbsp;
        <a href="https://www.familienwecker.de/privacy-policy.html" style="color: #999; text-decoration: none;">${isDE ? "Datenschutzerklärung" : "Privacy Policy"}</a> &nbsp;|&nbsp;
        <a href="https://www.familienwecker.de/imprint.html" style="color: #999; text-decoration: none;">${isDE ? "Impressum" : "Legal Notice"}</a><br><br>
        ${t.footerNote}
      </p>
    </div>
  `;
}

exports.sendVerificationEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"],
    invoker: "public",
  },
  async (request) => {
    const email = request.data?.email;
    const lang = (request.data?.language || "de").startsWith("en") ? "en" : "de";

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim());
      const linkOriginal = await admin.auth().generateEmailVerificationLink(email.trim());
      // Firebase uses a single global Action URL configured in the console (currently reset-password.html)
      // So we must string-replace it back to verify-email.html for verification links.
      let link = linkOriginal.replace("deine-domain.de", "www.familienwecker.de");
      link = link.replace("reset-password.html", "verify-email.html");

      // Dynamically switch to English HTML page if language is set to English
      if (lang === "en") {
        link = link.replace("verify-email.html", "verify-email-en.html");
      }

      const t = EMAIL_CONTENT_VERIFY[lang] || EMAIL_CONTENT_VERIFY.de;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.de,
        to: [email.trim()],
        subject: t.subject,
        html: buildVerifyEmailHtml(link, lang),
      });

      if (error) {
        console.error("Resend Error:", error);
        throw new HttpsError("internal", "Failed to send verification email via Resend.");
      }

      return { success: true };

    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Error in sendVerificationEmail:", err);
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

// ─── Scheduled Function: Bereinigung unbestätigter User ──────────────────────
const { onSchedule } = require("firebase-functions/v2/scheduler");

exports.cleanupUnverifiedUsers = onSchedule(
  {
    schedule: "every day 04:00",
    region: "europe-west3",
    timeZone: "Europe/Berlin",
    secrets: ["RESEND_API_KEY"],
  },
  async (event) => {
    const fortyEightHoursAgoMs = Date.now() - 48 * 60 * 60 * 1000;
    const staleUsers = [];

    console.log(`Running unverified users cleanup. Deleting users created before ${new Date(fortyEightHoursAgoMs).toISOString()}`);

    async function listAllUnverifiedUsers(nextPageToken) {
      const result = await admin.auth().listUsers(1000, nextPageToken);
      result.users.forEach((user) => {
        const createdAtMs = Date.parse(user.metadata.creationTime);
        if (!user.emailVerified && createdAtMs < fortyEightHoursAgoMs) {
          // Check if user has any other providers (e.g. Google) - if Google, email is usually verified automatically
          // but we check anyway to be safe. Password-only users are the main target.
          const isPasswordUser = user.providerData.some(p => p.providerId === 'password');
          if (isPasswordUser) {
            staleUsers.push(user.uid);
          }
        }
      });
      if (result.pageToken) {
        await listAllUnverifiedUsers(result.pageToken);
      }
    }

    await listAllUnverifiedUsers();

    if (staleUsers.length === 0) {
      console.log("No expired unverified users found.");
      return;
    }

    console.log(`Found ${staleUsers.length} users to delete. Starting batch deletion...`);

    // Delete in chunks of 1000 (limit of deleteUsers)
    for (let i = 0; i < staleUsers.length; i += 1000) {
      const chunk = staleUsers.slice(i, i + 1000);
      await admin.auth().deleteUsers(chunk);
    }

    console.log(`Successfully deleted ${staleUsers.length} unverified users.`);

    // Admin-Benachrichtigung senden
    const resendKey = process.env.RESEND_API_KEY;
    if (resendKey && staleUsers.length > 0) {
      try {
        const resend = new Resend(resendKey);
        await resend.emails.send({
          from: SENDER.de,
          to: [NOTIFY_EMAIL],
          subject: `🧹 User-Profile bereinigt: ${staleUsers.length} Accounts gelöscht`,
          html: `
            <div style="font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
              <h2 style="color: ${BRAND_BLUE};">🧹 User-Bereinigung abgeschlossen</h2>
              <p>Der tägliche Cleanup-Job hat nicht verifizierte Benutzerkonten entfernt, die älter als 48 Stunden waren.</p>
              <p><strong>Anzahl gelöschter Accounts:</strong> ${staleUsers.length}</p>
              <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
              <p style="font-size: 11px; color: #999;">Projekt: Familienwecker App</p>
            </div>
          `,
        });
        console.log("Admin notification sent for user cleanup.");
      } catch (err) {
        console.error("Failed to send admin notification for user cleanup:", err);
      }
    }
  }
);

// ─── Scheduled Function: Bereinigung inaktiver Familien (Müll-Sammlung) ───────
exports.cleanupInactiveFamilies = onSchedule(
  {
    schedule: "every sunday 04:00",
    region: "europe-west3",
    timeZone: "Europe/Berlin",
    secrets: ["RESEND_API_KEY"],
  },
  async (event) => {
    // 6 Monate (180 Tage) Inaktivität
    const sixMonthsAgoMs = Date.now() - 180 * 24 * 60 * 60 * 1000;
    const staleFamilies = [];

    console.log(`Running inactive families cleanup. Looking for activity before ${new Date(sixMonthsAgoMs).toISOString()}`);

    const familiesSnapshot = await admin.firestore().collection("families").get();

    for (const familyDoc of familiesSnapshot.docs) {
      // Höre auf das letzte Update eines Mitglieds
      const membersSnapshot = await familyDoc.ref.collection("members")
        .orderBy("lastUpdatedAt", "desc")
        .limit(1)
        .get();

      // Default: NICHT löschen – nur explizit als stale markieren wenn sicher veraltet
      let isStale = false;
      const familyData = familyDoc.data();

      // createdAt prüfen: Fallback Date.now() (= nie löschen) wenn Feld fehlt
      const createdAtMs = familyData.createdAt
        ? (typeof familyData.createdAt.toMillis === "function"
            ? familyData.createdAt.toMillis()
            : familyData.createdAt)
        : Date.now(); // Sicherer Fallback: kein Datum → als "jetzt" behandeln, nie löschen

      if (createdAtMs <= sixMonthsAgoMs) {
        // Familie selbst ist alt genug – prüfe ob Members aktiv waren
        if (membersSnapshot.empty) {
          // Keine Members UND Familie älter als 6 Monate → stale
          isStale = true;
        } else {
          const latestMember = membersSnapshot.docs[0].data();
          // Firestore Timestamps haben .toMillis() – direkter Vergleich mit > wäre nur bei Numbers korrekt
          // Fallback Date.now() wenn lastUpdatedAt fehlt → sicher, nicht löschen
          const lastUpdatedMs = latestMember.lastUpdatedAt
            ? (typeof latestMember.lastUpdatedAt.toMillis === "function"
                ? latestMember.lastUpdatedAt.toMillis()
                : latestMember.lastUpdatedAt)
            : Date.now();
          if (lastUpdatedMs <= sixMonthsAgoMs) {
            isStale = true;
          }
        }
      }

      if (isStale) {
        staleFamilies.push(familyDoc.ref);
      }
    }

    if (staleFamilies.length === 0) {
      console.log("No inactive families found.");
      return;
    }

    console.log(`Found ${staleFamilies.length} families to delete. Starting deletion...`);

    // In modern Admin SDKs, recursiveDelete deletes the document and all its subcollections
    if (admin.firestore().recursiveDelete) {
      for (const ref of staleFamilies) {
        await admin.firestore().recursiveDelete(ref);
      }
    } else {
      // Fallback
      for (const ref of staleFamilies) {
        const members = await ref.collection("members").get();
        const batch = admin.firestore().batch();
        members.docs.forEach(doc => batch.delete(doc.ref));
        batch.delete(ref);
        await batch.commit();
      }
    }

    console.log(`Successfully deleted ${staleFamilies.length} inactive families.`);

    // Admin-Benachrichtigung senden
    const resendKey = process.env.RESEND_API_KEY;
    if (resendKey && staleFamilies.length > 0) {
      try {
        const resend = new Resend(resendKey);
        await resend.emails.send({
          from: SENDER.de,
          to: [NOTIFY_EMAIL],
          subject: `🧹 Familien-Bereinigung: ${staleFamilies.length} inaktive Familien gelöscht`,
          html: `
            <div style="font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
              <h2 style="color: ${BRAND_BLUE};">🧹 Müll-Sammlung abgeschlossen</h2>
              <p>Der wöchentliche Cleanup-Job hat inaktive Familien entfernt, auf die seit mehr als 6 Monaten (180 Tagen) nicht mehr zugegriffen wurde.</p>
              <p><strong>Anzahl gelöschter Familien:</strong> ${staleFamilies.length}</p>
              <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
              <p style="font-size: 11px; color: #999;">Projekt: Familienwecker App</p>
            </div>
          `,
        });
        console.log("Admin notification sent for family cleanup.");
      } catch (err) {
        console.error("Failed to send admin notification for family cleanup:", err);
      }
    }
  }
);

// ─── Sicherer Join-Flow via Cloud Function ────────────────────────────────────
// Verhindert direkten Firestore-Zugriff auf alle Familien und ermöglicht
// serverseitiges Rate-Limiting gegen Brute-Force-Versuche auf Join-Codes.
exports.joinFamilyByCode = onCall(
  { region: "europe-west3" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const rawCode = request.data?.code;
    if (!rawCode || typeof rawCode !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_CODE");
    }
    const code = rawCode.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6);
    if (code.length !== 6) {
      throw new HttpsError("invalid-argument", "INVALID_CODE");
    }

    // Rate-Limiting: max. 5 Join-Versuche pro UID pro Minute, max. 10 pro Tag
    try {
      const minuteLimited = await checkSingleRateLimit(`join_${uid}_m`, 60 * 1000, 5);
      if (minuteLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      const dayLimited = await checkSingleRateLimit(`join_${uid}_d`, 24 * 60 * 60 * 1000, 10);
      if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Rate-Limit-Check fehlgeschlagen (wird ignoriert):", err);
    }

    const snapshot = await admin.firestore()
      .collection("families")
      .where("joinCode", "==", code)
      .limit(1)
      .get();

    if (snapshot.empty) {
      throw new HttpsError("not-found", "FAMILY_NOT_FOUND");
    }

    return { familyId: snapshot.docs[0].id, joinCode: code };
  }
);

// ─── Sicheres Familie-Erstellen via Cloud Function ────────────────────────────
// Generiert den joinCode serverseitig und schreibt das Familie-Dokument.
// Verhindert client-seitigen Query auf die families-Collection für Eindeutigkeitsprüfung.
exports.createFamily = onCall(
  { region: "europe-west3" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const familyName = request.data?.familyName;
    if (!familyName || typeof familyName !== "string" || familyName.trim().length === 0) {
      throw new HttpsError("invalid-argument", "INVALID_FAMILY_NAME");
    }
    const sanitizedName = familyName.trim().slice(0, 64);

    // Rate-Limiting: max. 3 Family-Erstellungen pro UID pro Stunde, max. 6 pro Tag
    try {
      const hourLimited = await checkSingleRateLimit(`create_${uid}_h`, 60 * 60 * 1000, 3);
      if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      const dayLimited = await checkSingleRateLimit(`create_${uid}_d`, 24 * 60 * 60 * 1000, 6);
      if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Rate-Limit-Check fehlgeschlagen (wird ignoriert):", err);
    }

    // Eindeutigen 6-stelligen alphanumerischen Join-Code generieren (max. 5 Versuche)
    const CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // ohne 0/O und 1/I zur Verwechslungsvermeidung
    let joinCode = null;
    let attempts = 0;

    while (attempts < 5) {
      const candidate = Array.from({ length: 6 }, () =>
        CHARS.charAt(randomInt(CHARS.length))
      ).join("");

      const existing = await admin.firestore()
        .collection("families")
        .where("joinCode", "==", candidate)
        .limit(1)
        .get();

      if (existing.empty) {
        joinCode = candidate;
        break;
      }
      attempts++;
    }

    if (!joinCode) {
      throw new HttpsError("internal", "CODE_GENERATION_FAILED");
    }

    // Familie-Dokument anlegen
    const familyData = {
      name: sanitizedName,
      joinCode,
      createdByUserId: uid,
      isAlarmEnabled: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    const docRef = await admin.firestore().collection("families").add(familyData);

    console.log(`Family '${sanitizedName}' created by ${uid} with id ${docRef.id} and code ${joinCode}`);

    return { familyId: docRef.id, joinCode };
  }
);
 
/**
 * Täglicher Reset-Job für Familienmitglieder (alle 1h).
 * Setzt "isAwakeToday" und "isPaused" (nur bei ungeclaimten) zurück.
 * Schwellenwert: latestWakeUp + 2 Stunden.
 */
exports.scheduledMemberReset = onSchedule(
  {
    schedule: "every 1 hours",
    region: "europe-west3",
    timeZone: "Europe/Berlin",
  },
  async (event) => {
    const familiesSnapshot = await admin.firestore().collection("families").get();
    const now = new Date();
    
    // Berlin Zeit für den Vergleich (YYYY-MM-DD und HH:mm)
    const options = { timeZone: "Europe/Berlin", hour12: false };
    const todayStr = now.toLocaleDateString("en-CA", options); // YYYY-MM-DD
    const currentTimeStr = now.toLocaleTimeString("en-GB", options).slice(0, 5); // HH:mm
    
    console.log(`Running scheduled reset check at ${currentTimeStr} (${todayStr}).`);

    for (const familyDoc of familiesSnapshot.docs) {
      const membersRef = familyDoc.ref.collection("members");
      const membersSnapshot = await membersRef.get();
      const batch = admin.firestore().batch();
      let hasUpdates = false;

      membersSnapshot.forEach((memberDoc) => {
        const member = memberDoc.data();
        const latestWakeUp = member.latestWakeUp; // "HH:mm"
        
        if (!latestWakeUp) return;

        // Schwellenwert berechnen (latestWakeUp + 2h)
        const [hours, minutes] = latestWakeUp.split(":").map(Number);
        const resetDate = new Date();
        resetDate.setHours(hours + 2, minutes, 0, 0);
        const resetTimeStr = resetDate.toLocaleTimeString("en-GB", options).slice(0, 5);

        // Reset nur wenn:
        // 1. Aktuelle Zeit > (latestWakeUp + 2h)
        // 2. lastResetDate != today (sichert dass 1x pro Tag resettet wird)
        const isPastResetThreshold = currentTimeStr >= resetTimeStr;
        const needsReset = isPastResetThreshold && member.lastResetDate !== todayStr;

        if (needsReset) {
          const isUnclaimed = !member.claimedByUserId;
          const updates = {
            isAwakeToday: false,
            lastResetDate: todayStr,
            lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
          };
          
          if (isUnclaimed) {
            updates.isPaused = false;
          }
          
          batch.update(memberDoc.ref, updates);
          hasUpdates = true;
        }
      });

      if (hasUpdates) {
        await batch.commit();
        console.log(`Reset performed for members in family ${familyDoc.id}`);
      }
    }
    console.log("Scheduled member reset completed.");
  }
);

// ─── Feedback-E-Mail via Resend ──────────────────────────────────────────────
exports.sendFeedbackEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"],
    invoker: "public",
  },
  async (request) => {
    const { category, message, email, appVersion, device } = request.data || {};

    if (!message || typeof message !== "string" || message.trim().length === 0) {
      throw new HttpsError("invalid-argument", "INVALID_MESSAGE");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      throw new HttpsError("failed-precondition", "Email service not configured.");
    }

    const sanitizedCategory = escapeHtml(category || "Sonstiges");
    const sanitizedMessage = escapeHtml(message?.trim() || "");

    const replyTo = email && email.trim() ? email.trim() : undefined;
    const subject = `📬 FamWake Feedback: ${sanitizedCategory}`;
    const html = `
      <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
        <h2 style="color: ${BRAND_BLUE};">📬 Neues Feedback</h2>
        <table style="width:100%; border-collapse: collapse; font-size:14px;">
          <tr><td style="padding:6px 0; color:#666; width:130px;">Kategorie</td><td><strong>${sanitizedCategory}</strong></td></tr>
          <tr><td style="padding:6px 0; color:#666;">App-Version</td><td>${appVersion || "–"}</td></tr>
          <tr><td style="padding:6px 0; color:#666;">Gerät</td><td>${device || "–"}</td></tr>
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
