const { test, describe } = require("node:test");
const assert = require("node:assert/strict");
const { escapeHtml, resolveLanguage } = require("../lib/shared");

describe("Functions Shared Helpers", () => {
  describe("escapeHtml", () => {
    test("escapes HTML special characters correctly", () => {
      const input = '<script>alert("XSS & attack\'s")</script>';
      const expected = '&lt;script&gt;alert(&quot;XSS &amp; attack&#039;s&quot;)&lt;/script&gt;';
      assert.equal(escapeHtml(input), expected);
    });

    test("handles empty and non-string values gracefully", () => {
      assert.equal(escapeHtml(""), "");
      assert.equal(escapeHtml(null), "");
      assert.equal(escapeHtml(undefined), "");
      assert.equal(escapeHtml(123), "");
    });
  });

  describe("resolveLanguage", () => {
    test("resolves supported standard languages", () => {
      assert.equal(resolveLanguage("de"), "de");
      assert.equal(resolveLanguage("en"), "en");
      assert.equal(resolveLanguage("es"), "es");
      assert.equal(resolveLanguage("fr"), "fr");
    });

    test("maps German dialects to standard German 'de'", () => {
      assert.equal(resolveLanguage("gsw"), "de");
      assert.equal(resolveLanguage("swg"), "de");
      assert.equal(resolveLanguage("ksh"), "de");
    });

    test("handles Chinese locale mapping to zh-CN", () => {
      assert.equal(resolveLanguage("zh"), "zh-CN");
      assert.equal(resolveLanguage("zh-cn"), "zh-CN");
      assert.equal(resolveLanguage("zh-CN"), "zh-CN");
    });

    test("falls back to 'en' for unknown or unsupported languages", () => {
      assert.equal(resolveLanguage("xx"), "en");
      assert.equal(resolveLanguage("klingon"), "en");
    });

    test("falls back to 'de' if input is null or undefined", () => {
      assert.equal(resolveLanguage(null), "de");
      assert.equal(resolveLanguage(undefined), "de");
      assert.equal(resolveLanguage(""), "de");
    });
  });
});
