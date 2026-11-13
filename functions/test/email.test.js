const { test, describe } = require("node:test");
const assert = require("node:assert/strict");
const {
  renderEmailFooter,
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

  test("renderEmailFooter uses neutral fallback and respects OPERATOR_* env variables", () => {
    const prevName = process.env.OPERATOR_NAME;
    const prevAddr = process.env.OPERATOR_ADDRESS;
    const t = { appName: "FamWake", footerNote: "Note" };
    const l = { home: "Home", privacy: "Privacy", imprint: "Imprint" };

    try {
      delete process.env.OPERATOR_NAME;
      delete process.env.OPERATOR_ADDRESS;
      const neutralFooter = renderEmailFooter(t, l, "https://example.com/p", "https://example.com/i");
      assert.ok(neutralFooter.includes("FamWake Support"));
      assert.ok(!neutralFooter.includes("Virchow"));

      process.env.OPERATOR_NAME = "Test Operator";
      process.env.OPERATOR_ADDRESS = "Example Street 1";
      const customFooter = renderEmailFooter(t, l, "https://example.com/p", "https://example.com/i");
      assert.ok(customFooter.includes("Test Operator"));
      assert.ok(customFooter.includes("Example Street 1"));
    } finally {
      if (prevName === undefined) delete process.env.OPERATOR_NAME;
      else process.env.OPERATOR_NAME = prevName;
      if (prevAddr === undefined) delete process.env.OPERATOR_ADDRESS;
      else process.env.OPERATOR_ADDRESS = prevAddr;
    }
  });
});

