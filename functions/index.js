// Cloud Functions – Modular Entry Point
// admin.initializeApp() wird in shared.js beim ersten require() aufgerufen
require("./lib/shared");

const email = require("./lib/email");
const family = require("./lib/family");
const cleanup = require("./lib/cleanup");
const adminReports = require("./lib/admin-reports");

// E-Mail
exports.sendBrandedResetEmail = email.sendBrandedResetEmail;
exports.sendBrandedConfirmationEmail = email.sendBrandedConfirmationEmail;
exports.sendVerificationEmail = email.sendVerificationEmail;

// Family
exports.joinFamilyByCode = family.joinFamilyByCode;
exports.createFamily = family.createFamily;
exports.leaveFamily = family.leaveFamily;
exports.deleteFamily = family.deleteFamily;
exports.onMemberScheduleChanged = family.onMemberScheduleChanged;

// Cleanup
exports.cleanupUnverifiedUsers = cleanup.cleanupUnverifiedUsers;
exports.cleanupInactiveFamilies = cleanup.cleanupInactiveFamilies;
exports.scheduledMemberReset = cleanup.scheduledMemberReset;
exports.cleanupAnonymousUsers = cleanup.cleanupAnonymousUsers;

// Admin & Sonstige
exports.sendFeedbackEmail = adminReports.sendFeedbackEmail;
exports.sendAdminStatsReport = adminReports.sendAdminStatsReport;
exports.scheduledAdminStatsReport = adminReports.scheduledAdminStatsReport;
exports.getUserContext = adminReports.getUserContext;
exports.verifyIntegrityToken = adminReports.verifyIntegrityToken;
exports.onUserDeleted = adminReports.onUserDeleted;