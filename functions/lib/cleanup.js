// cleanup.js – Scheduled Cleanup Jobs
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { Resend } = require("resend");
const { admin, NOTIFY_EMAIL, BRAND_BLUE } = require("./shared");
const { SENDER } = require("./i18n");

// ─── Scheduled Function: Bereinigung unbestätigter User ──────────────────────

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
    timeoutSeconds: 1800, // 30 Min Timeout (Firebase Max) für wachsende Datenmengen
    secrets: ["RESEND_API_KEY"],
  },
  async (event) => {
    // 6 Monate (180 Tage) Inaktivität
    const sixMonthsAgoMs = Date.now() - 180 * 24 * 60 * 60 * 1000;
    const staleFamilies = [];

    console.log(`Running inactive families cleanup. Looking for activity before ${new Date(sixMonthsAgoMs).toISOString()}`);

    // Stream statt .get() verhindert Memory-Limits bei >10.000 Familien
    const familiesStream = admin.firestore().collection("families").stream();

    for await (const familyDoc of familiesStream) {
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

// ─── Scheduled Function: Bereinigung unlinked anonymer User (jeden Sonntag 03:00) ───
exports.cleanupAnonymousUsers = onSchedule(
  {
    schedule: "0 3 * * 0",
    region: "europe-west3",
    timeZone: "Europe/Berlin",
  },
  async (event) => {
    console.log("Running weekly anonymous users cleanup...");
    const thirtyDaysAgoMs = Date.now() - 30 * 24 * 60 * 60 * 1000;
    const staleAnonymousUsers = [];

    async function listAnonymousUsers(nextPageToken) {
      const result = await admin.auth().listUsers(1000, nextPageToken);
      result.users.forEach((user) => {
        // Anonyme User haben keine verknüpften Provider (providerData ist leer)
        const isAnonymous = user.providerData.length === 0;
        const createdAtMs = Date.parse(user.metadata.creationTime);
        if (isAnonymous && createdAtMs < thirtyDaysAgoMs) {
          staleAnonymousUsers.push(user.uid);
        }
      });
      if (result.pageToken) {
        await listAnonymousUsers(result.pageToken);
      }
    }

    await listAnonymousUsers();

    if (staleAnonymousUsers.length === 0) {
      console.log("No expired anonymous users found.");
      return;
    }

    console.log(`Found ${staleAnonymousUsers.length} expired anonymous users. Starting batch deletion...`);

    // In Chunks von 1000 löschen (Limit von deleteUsers)
    for (let i = 0; i < staleAnonymousUsers.length; i += 1000) {
      const chunk = staleAnonymousUsers.slice(i, i + 1000);
      await admin.auth().deleteUsers(chunk);
    }

    console.log(`Successfully deleted ${staleAnonymousUsers.length} anonymous users.`);
  }
);
