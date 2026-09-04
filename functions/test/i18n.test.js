const { test, describe } = require("node:test");
const assert = require("node:assert/strict");
const {
  SUPPORTED_LANGS,
  DIALECT_TO_LANG,
  SENDER,
  LINK_LABELS,
  EMAIL_CONTENT,
  EMAIL_CONTENT_CONFIRM,
  EMAIL_CONTENT_VERIFY
} = require("../lib/i18n");

describe("Functions i18n Localization Integrity", () => {
  test("SUPPORTED_LANGS should contain 22 supported languages", () => {
    assert.equal(SUPPORTED_LANGS.length, 22);
    assert.ok(SUPPORTED_LANGS.includes("de"));
    assert.ok(SUPPORTED_LANGS.includes("en"));
    assert.ok(SUPPORTED_LANGS.includes("zh-CN"));
  });

  test("DIALECT_TO_LANG maps German dialects to 'de'", () => {
    assert.equal(DIALECT_TO_LANG.gsw, "de");
    assert.equal(DIALECT_TO_LANG.swg, "de");
    assert.equal(DIALECT_TO_LANG.ksh, "de");
  });

  test("Every supported language has a sender name with valid format", () => {
    for (const lang of SUPPORTED_LANGS) {
      const sender = SENDER[lang];
      assert.ok(sender, `Missing sender for language: ${lang}`);
      assert.match(sender, /FamWake.*<no-reply@familienwecker\.de>/, `Invalid sender format for ${lang}: ${sender}`);
    }
  });

  test("Every supported language has valid LINK_LABELS", () => {
    const requiredKeys = ["home", "privacy", "imprint"];
    for (const lang of SUPPORTED_LANGS) {
      const labels = LINK_LABELS[lang];
      assert.ok(labels, `Missing LINK_LABELS for ${lang}`);
      for (const key of requiredKeys) {
        assert.ok(labels[key] && labels[key].trim().length > 0, `Missing or empty link label '${key}' for ${lang}`);
      }
    }
  });

  test("Every supported language has complete EMAIL_CONTENT (Password Reset)", () => {
    const requiredFields = [
      "subject", "appName", "greeting", "intro",
      "instruction", "button", "fallback", "security", "footerNote"
    ];
    for (const lang of SUPPORTED_LANGS) {
      const content = EMAIL_CONTENT[lang];
      assert.ok(content, `Missing EMAIL_CONTENT for ${lang}`);
      for (const field of requiredFields) {
        assert.ok(
          content[field] && typeof content[field] === "string" && content[field].trim().length > 0,
          `Missing or empty field '${field}' in EMAIL_CONTENT for language: ${lang}`
        );
      }
      assert.ok(content.security.includes("daniel.notthoff@gmail.com"), `Security note for ${lang} must contain daniel.notthoff@gmail.com`);
    }
  });

  test("Every supported language has complete EMAIL_CONTENT_CONFIRM", () => {
    const requiredFields = [
      "subject", "appName", "greeting", "intro",
      "security", "footerNote"
    ];
    for (const lang of SUPPORTED_LANGS) {
      const content = EMAIL_CONTENT_CONFIRM[lang];
      assert.ok(content, `Missing EMAIL_CONTENT_CONFIRM for ${lang}`);
      for (const field of requiredFields) {
        assert.ok(
          content[field] && typeof content[field] === "string" && content[field].trim().length > 0,
          `Missing or empty field '${field}' in EMAIL_CONTENT_CONFIRM for language: ${lang}`
        );
      }
      assert.ok(content.security.includes("daniel.notthoff@gmail.com"), `Confirm security note for ${lang} must contain daniel.notthoff@gmail.com`);
    }
  });

  test("Every supported language has complete EMAIL_CONTENT_VERIFY", () => {
    const requiredFields = [
      "subject", "appName", "greeting", "intro",
      "instruction", "button", "fallback", "privacy", "security", "footerNote"
    ];
    for (const lang of SUPPORTED_LANGS) {
      const content = EMAIL_CONTENT_VERIFY[lang];
      assert.ok(content, `Missing EMAIL_CONTENT_VERIFY for ${lang}`);
      for (const field of requiredFields) {
        assert.ok(
          content[field] && typeof content[field] === "string" && content[field].trim().length > 0,
          `Missing or empty field '${field}' in EMAIL_CONTENT_VERIFY for language: ${lang}`
        );
      }
    }
  });

});
