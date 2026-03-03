const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { Resend } = require("resend");

admin.initializeApp();

const NOTIFY_EMAIL = "daniel.notthoff@gmail.com";
const BRAND_BLUE = "#1A3A5C";

const SENDER = {
  de: "FamWake Familienwecker <no-reply@familienwecker.de>",
  en: "FamWake Family Alarm <no-reply@familienwecker.de>",
};

const EMAIL_CONTENT = {
  de: {
    subject: "🔑 Passwort zurücksetzen – FamWake",
    appName: "FamWake - Familienwecker",
    greeting: "Hallo!",
    intro: "Wir haben eine Anfrage erhalten, das Passwort für dein <strong>FamWake</strong>-Konto zurückzusetzen.",
    instruction: "Klicke auf den folgenden Button, um ein neues Passwort zu vergeben:",
    button: "Passwort zurücksetzen",
    fallback: "Falls der Button nicht funktioniert, kopiere bitte diesen Link in deinen Browser:",
    security: "⚠️ Falls du diese Anfrage <strong>nicht</strong> gestellt hast, kannst du diese E-Mail einfach ignorieren. Dein Passwort bleibt unverändert. Solltest du verdächtige Aktivitäten bemerken, wende dich bitte an: daniel.notthoff@gmail.com",
    footer: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "🔑 Reset your password – FamWake",
    appName: "FamWake - Family Alarm",
    greeting: "Hello!",
    intro: "We received a request to reset the password for your <strong>FamWake</strong> account.",
    instruction: "Click the button below to set a new password:",
    button: "Reset Password",
    fallback: "If the button doesn't work, please paste this link into your browser:",
    security: "⚠️ If you did <strong>not</strong> request this, you can safely ignore this email. Your password will remain unchanged. If you notice any suspicious activity, please contact us at: daniel.notthoff@gmail.com",
    footer: "This is an automated message. Please do not reply directly to this email.",
  },
};

const EMAIL_CONTENT_CONFIRM = {
  de: {
    subject: "✅ Passwort erfolgreich geändert – FamWake",
    appName: "FamWake - Familienwecker",
    greeting: "Hallo!",
    intro: "Dein Passwort für dein <strong>FamWake</strong>-Konto wurde erfolgreich geändert.",
    instruction: "Du kannst dich ab sofort mit deinem neuen Passwort in der App anmelden.",
    security: "⚠️ Falls du dein Passwort <strong>nicht</strong> geändert hast, wende dich bitte umgehend an uns: daniel.notthoff@gmail.com",
    footer: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "✅ Password successfully changed – FamWake",
    appName: "FamWake - Family Alarm",
    greeting: "Hello!",
    intro: "The password for your <strong>FamWake</strong> account has been successfully changed.",
    instruction: "You can now log in to the app with your new password.",
    security: "⚠️ If you did <strong>not</strong> change your password, please contact us immediately: daniel.notthoff@gmail.com",
    footer: "This is an automated message. Please do not reply directly to this email.",
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
        ${t.footer}
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
      const code = err.code || (err.errorInfo && err.errorInfo.code) || "";
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
        ${t.footer}
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
    subject: "🚀 Bestätige dein FamWake-Konto",
    appName: "FamWake - Familienwecker",
    greeting: "Willkommen bei FamWake!",
    intro: "Vielen Dank für deine Registrierung bei <strong>FamWake</strong>. Wir freuen uns darauf, dir und deiner Familie zu einem entspannten Morgen ohne Chaos zu verhelfen!",
    instruction: "Bitte bestätige deine E-Mail-Adresse, um dein Konto zu aktivieren:",
    button: "E-Mail-Adresse bestätigen",
    fallback: "Falls der Button nicht funktioniert, kopiere bitte diesen Link in deinen Browser:",
    privacy: "<strong>Hinweis:</strong> Aus Datenschutzgründen werden dieser Link und deine unbestätigten Registrierungsdaten automatisch nach 48 Stunden gelöscht, falls keine Aktivierung erfolgt.",
    security: "Falls du dieses Konto nicht erstellt hast, kannst du diese E-Mail einfach ignorieren. Es werden keine Daten dauerhaft von dir gespeichert.",
    footer: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "🚀 Confirm your FamWake account",
    appName: "FamWake - Family Alarm",
    greeting: "Welcome to FamWake!",
    intro: "Thank you for registering with <strong>FamWake</strong>. We look forward to helping you and your family start the day stress-free!",
    instruction: "Please confirm your email address to activate your account:",
    button: "Confirm email address",
    fallback: "If the button doesn't work, please paste this link into your browser:",
    privacy: "<strong>Note:</strong> For privacy reasons, this link and your unconfirmed registration data will be automatically deleted after 48 hours if no activation occurs.",
    security: "If you did not create this account, you can safely ignore this email. No data will be permanently stored.",
    footer: "This is an automated message. Please do not reply directly to this email.",
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
        ${t.footer}
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
