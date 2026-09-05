const { test, describe } = require("node:test");
const assert = require("node:assert/strict");
const {
  buildEmailHtml,
  buildConfirmEmailHtml,
  buildVerifyEmailHtml
} = require("../lib/email");

describe("Functions Email HTML Builders", () => {
  const dummyLink = "https://www.familienwecker.de/reset-password.html?apiKey=fake&mode=resetPassword&oobCode=12345";

  test("buildEmailHtml generates valid HTML with required elements", () => {
    const htmlDe = buildEmailHtml(dummyLink, "de");
    assert.ok(htmlDe.includes(dummyLink), "Reset link must be embedded in HTML");
    assert.ok(htmlDe.includes("Passwort zurücksetzen"), "Must include German action button text");
    assert.ok(htmlDe.includes("Datenschutzerklärung"), "Must include privacy link");
    assert.ok(htmlDe.includes("Impressum"), "Must include imprint link");
    assert.ok(htmlDe.includes("famwake@goork.de"), "Must include security contact email");

    const htmlEn = buildEmailHtml(dummyLink, "en");
    assert.ok(htmlEn.includes(dummyLink));
    assert.ok(htmlEn.includes("Reset Password"));
    assert.ok(htmlEn.includes("Privacy Policy"));
  });

  test("buildConfirmEmailHtml generates valid confirmation notice", () => {
    const htmlDe = buildConfirmEmailHtml("de");
    assert.ok(htmlDe.includes("Datenschutzerklärung"));
    assert.ok(htmlDe.includes("famwake@goork.de"));

    const htmlEn = buildConfirmEmailHtml("en");
    assert.ok(htmlEn.includes("Privacy Policy"));
  });

  test("buildVerifyEmailHtml generates valid verification email", () => {
    const verifyLink = "https://www.familienwecker.de/verify-email.html?mode=verifyEmail&oobCode=67890";
    const htmlDe = buildVerifyEmailHtml(verifyLink, "de");
    assert.ok(htmlDe.includes(verifyLink));
    assert.ok(htmlDe.includes("E-Mail-Adresse bestätigen") || htmlDe.includes("bestätigen"));
    assert.ok(htmlDe.includes("Datenschutzerklärung"));
    assert.ok(htmlDe.includes("Impressum"));
  });
});

