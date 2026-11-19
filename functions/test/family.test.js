const { test, describe } = require("node:test");
const assert = require("node:assert/strict");
const { HttpsError } = require("firebase-functions/v2/https");
const { canDeleteMemberOnLeave, assertCanNotifyBathroomFree } = require("../lib/family");

describe("Functions Family Authorization Helpers", () => {
  describe("canDeleteMemberOnLeave", () => {
    test("allows deleting own claimed member", () => {
      assert.doesNotThrow(() =>
        canDeleteMemberOnLeave({ claimedByUserId: "u1" }, { createdByUserId: "u2" }, "u1")
      );
    });

    test("rejects deleting another user's claimed member", () => {
      assert.throws(
        () => canDeleteMemberOnLeave({ claimedByUserId: "u2" }, { createdByUserId: "u1" }, "u1"),
        (err) => err instanceof HttpsError && err.code === "permission-denied"
      );
    });

    test("allows family creator to delete unclaimed member on leave", () => {
      assert.doesNotThrow(() =>
        canDeleteMemberOnLeave({ claimedByUserId: null }, { createdByUserId: "u1" }, "u1")
      );
    });

    test("rejects non-creator deleting unclaimed member on leave", () => {
      assert.throws(
        () => canDeleteMemberOnLeave({ claimedByUserId: null }, { createdByUserId: "u2" }, "u1"),
        (err) => err instanceof HttpsError && err.code === "permission-denied"
      );
    });
  });

  describe("assertCanNotifyBathroomFree", () => {
    test("allows claimed owner or unclaimed member", () => {
      assert.doesNotThrow(() => assertCanNotifyBathroomFree({ claimedByUserId: "u1" }, "u1"));
      assert.doesNotThrow(() => assertCanNotifyBathroomFree({ claimedByUserId: null }, "u1"));
    });

    test("rejects spoofing another user's claimed member", () => {
      assert.throws(
        () => assertCanNotifyBathroomFree({ claimedByUserId: "u2" }, "u1"),
        (err) => err instanceof HttpsError && err.code === "permission-denied"
      );
    });
  });
});
