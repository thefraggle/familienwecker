// email.js – E-Mail-Functions (Reset, Bestätigung, Verifizierung)
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { Resend } = require("resend");
const { admin, checkEmailRateLimit, BRAND_BLUE, resolveLanguage } = require("./shared");
const { SENDER, LINK_LABELS, EMAIL_CONTENT, EMAIL_CONTENT_CONFIRM, EMAIL_CONTENT_VERIFY } = require("./i18n");

function buildEmailHtml(link, lang) {
  const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  const l = LINK_LABELS[lang] || LINK_LABELS.en;

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
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${l.home}</a> &nbsp;|&nbsp;
        <a href="${privacyUrl}" style="color: #999; text-decoration: none;">${l.privacy}</a> &nbsp;|&nbsp;
        <a href="${imprintUrl}" style="color: #999; text-decoration: none;">${l.imprint}</a><br><br>
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
    if (request.auth && email && request.auth.token.email &&
      email.trim().toLowerCase() !== request.auth.token.email.toLowerCase()) {
      throw new HttpsError("permission-denied", "EMAIL_MISMATCH");
    }
    const lang = resolveLanguage(request.data?.language);
    console.log(`Email request for ${email?.trim()} with language: ${request.data?.language} -> mapped to: ${lang}`);

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim(), "reset");
      const linkOriginal = await admin.auth().generatePasswordResetLink(email.trim());
      let link = linkOriginal.replace("deine-domain.de", "www.familienwecker.de");

      if (lang !== "de") {
        link = link.replace("reset-password.html", `reset-password-${lang}.html`);
      }

      const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.en;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.en,
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
  const t = EMAIL_CONTENT_CONFIRM[lang] || EMAIL_CONTENT_CONFIRM.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  const l = LINK_LABELS[lang] || LINK_LABELS.en;

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
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${l.home}</a> &nbsp;|&nbsp;
        <a href="${privacyUrl}" style="color: #999; text-decoration: none;">${l.privacy}</a> &nbsp;|&nbsp;
        <a href="${imprintUrl}" style="color: #999; text-decoration: none;">${l.imprint}</a><br><br>
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
    const lang = resolveLanguage(request.data?.language);

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim(), "confirm");
      const t = EMAIL_CONTENT_CONFIRM[lang] || EMAIL_CONTENT_CONFIRM.en;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.en,
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
      if (err instanceof HttpsError) throw err;
      console.error("Error in sendBrandedConfirmationEmail:", err);
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

function buildVerifyEmailHtml(link, lang) {
  const t = EMAIL_CONTENT_VERIFY[lang] || EMAIL_CONTENT_VERIFY.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  const l = LINK_LABELS[lang] || LINK_LABELS.en;

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
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${l.home}</a> &nbsp;|&nbsp;
        <a href="${privacyUrl}" style="color: #999; text-decoration: none;">${l.privacy}</a> &nbsp;|&nbsp;
        <a href="${imprintUrl}" style="color: #999; text-decoration: none;">${l.imprint}</a><br><br>
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
    // Security: Nur eingeloggte User dürfen für ihre eigene E-Mail-Adresse eine Verifikations-Mail anfordern.
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const email = request.data?.email;
    // E-Mail muss mit der des eingeloggten Users übereinstimmen.
    if (email && request.auth.token.email && email.trim().toLowerCase() !== request.auth.token.email.toLowerCase()) {
      throw new HttpsError("permission-denied", "EMAIL_MISMATCH");
    }
    const lang = resolveLanguage(request.data?.language);

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim(), "verify");
      const linkOriginal = await admin.auth().generateEmailVerificationLink(email.trim());
      // Firebase uses a single global Action URL configured in the console (currently reset-password.html)
      // So we must string-replace it back to verify-email.html for verification links.
      let link = linkOriginal.replace("deine-domain.de", "www.familienwecker.de");
      link = link.replace("reset-password.html", "verify-email.html");

      // Dynamically switch to localized HTML page
      if (lang !== "de") {
        link = link.replace("verify-email.html", `verify-email-${lang}.html`);
      }

      const t = EMAIL_CONTENT_VERIFY[lang] || EMAIL_CONTENT_VERIFY.en;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.en,
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
