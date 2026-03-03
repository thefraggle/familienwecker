const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { Resend } = require("resend");

admin.initializeApp();

const BRAND_BLUE = "#1A3A5C";

const SENDER = {
  de: "FamWake Familienwecker <no-reply@familienwecker.de>",
  en: "FamWake Family Alarm <no-reply@familienwecker.de>",
};

const EMAIL_CONTENT = {
  de: {
    subject: "Passwort zurücksetzen - FamWake",
    appName: "FamWake - Familienwecker",
    greeting: "Hallo!",
    intro: "Du hast eine Anfrage zum Zurücksetzen deines Passworts für <strong>FamWake</strong> gestellt.",
    instruction: "Klicke auf den folgenden Button, um ein neues Passwort zu vergeben:",
    button: "Passwort zurücksetzen",
    fallback: "Falls der Button nicht funktioniert, kopiere diesen Link in deinen Browser:",
    security: "⚠️ Falls du diese Anfrage <strong>nicht</strong> gestellt hast, kannst du diese E-Mail ignorieren. Dein Passwort bleibt unverändert.",
    footer: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt darauf.",
  },
  en: {
    subject: "Reset your password - FamWake",
    appName: "FamWake - Family Alarm",
    greeting: "Hello!",
    intro: "You requested a password reset for your <strong>FamWake</strong> account.",
    instruction: "Click the button below to set a new password:",
    button: "Reset Password",
    fallback: "If the button doesn't work, paste this link into your browser:",
    security: "⚠️ If you did <strong>not</strong> request this, you can safely ignore this email. Your password will remain unchanged.",
    footer: "This is an automated message. Please do not reply directly.",
  },
};

const EMAIL_CONTENT_CONFIRM = {
  de: {
    subject: "Passwort erfolgreich geändert - FamWake",
    appName: "FamWake - Familienwecker",
    greeting: "Hallo!",
    intro: "Dein Passwort für <strong>FamWake</strong> wurde erfolgreich geändert.",
    instruction: "Du kannst dich jetzt mit deinem neuen Passwort in der App anmelden.",
    security: "⚠️ Falls du dein Passwort <strong>nicht</strong> geändert hast, kontaktiere uns bitte umgehend: support@familienwecker.de",
    footer: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt darauf.",
  },
  en: {
    subject: "Password successfully changed - FamWake",
    appName: "FamWake - Family Alarm",
    greeting: "Hello!",
    intro: "Your password for <strong>FamWake</strong> has been successfully changed.",
    instruction: "You can now log in to the app with your new password.",
    security: "⚠️ If you did <strong>not</strong> change your password, please contact us immediately: support@familienwecker.de",
    footer: "This is an automated message. Please do not reply directly.",
  },
};

function buildEmailHtml(link, lang) {
  const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.de;
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
      <p style="font-size: 11px; color: #999; text-align: center;">
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${t.appName}</a><br>
        <a href="https://www.familienwecker.de" style="color: #999;">www.familienwecker.de</a><br><br>
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
  return `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: ${BRAND_BLUE};">${t.greeting}</h2>
      <p>${t.intro}</p>
      <p>${t.instruction}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 12px; color: #888; background-color: #f9f9f9; padding: 12px; border-radius: 6px;">${t.security}</p>
      <p style="font-size: 11px; color: #999; text-align: center;">
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${t.appName}</a><br>
        <a href="https://www.familienwecker.de" style="color: #999;">www.familienwecker.de</a><br><br>
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
