// family.js – Familien-Verwaltung und Push-Notifications
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { randomInt } = require("crypto");
const { admin, checkSingleRateLimit, primaryAdminUidSecret, sendPushToUsers } = require("./shared");

/**
 * Interner Helper: Trägt einen User sauber aus einer alten Familie aus,
 * wenn er einer neuen beitritt oder eine neue erstellt (H7).
 */
async function internalLeaveFamily(uid, familyId, memberId = null) {
  try {
    const familyDocRef = admin.firestore().collection("families").doc(familyId);
    const familyDoc = await familyDocRef.get();

    if (!familyDoc.exists) {
      return;
    }

    const familyData = familyDoc.data() || {};
    const membersSnapshot = await familyDocRef.collection("members").get();

    // Falls User der Ersteller war und noch andere Mitglieder existieren: neuen Ersteller bestimmen
    if (familyData.createdByUserId === uid) {
      const otherMembers = membersSnapshot.docs.filter(doc => doc.id !== (memberId || uid) && doc.data().claimedByUserId !== uid);
      if (otherMembers.length > 0) {
        const newCreatorDoc = otherMembers.find(doc => doc.data().claimedByUserId);
        const newCreatorUid = newCreatorDoc ? newCreatorDoc.data().claimedByUserId : null;
        if (newCreatorUid) {
          await familyDocRef.update({ createdByUserId: newCreatorUid });
          console.log(`Reassigned creator of family ${familyId} from ${uid} to ${newCreatorUid}.`);
        }
      }
    }

    // Member-Dokument ermitteln und löschen
    let targetMemberRef = null;
    if (memberId) {
      const memberDocRef = familyDocRef.collection("members").doc(memberId);
      const memberDoc = await memberDocRef.get();
      if (memberDoc.exists) {
        if (memberDoc.data().claimedByUserId && memberDoc.data().claimedByUserId !== uid) {
          throw new HttpsError("permission-denied", "CANNOT_DELETE_OTHER_MEMBER");
        }
        targetMemberRef = memberDocRef;
      }
    } else {
      const claimedSnap = await familyDocRef.collection("members")
        .where("claimedByUserId", "==", uid)
        .limit(1)
        .get();
      if (!claimedSnap.empty) {
        targetMemberRef = claimedSnap.docs[0].ref;
      } else {
        const fallbackDoc = familyDocRef.collection("members").doc(uid);
        if ((await fallbackDoc.get()).exists) {
          targetMemberRef = fallbackDoc;
        }
      }
    }
    if (targetMemberRef) {
      await targetMemberRef.delete();
    }

    // UID aus userIds der Familie entfernen
    await familyDocRef.update({
      userIds: admin.firestore.FieldValue.arrayRemove(uid)
    });

    // Verbleibende Mitglieder informieren (fire-and-forget)
    const existingUserIds = familyData.userIds || [];
    notifyFamilyMemberLeft(existingUserIds, uid).catch(err =>
      console.warn("notifyFamilyMemberLeft failed (non-critical):", err?.message)
    );
  } catch (err) {
    if (err instanceof HttpsError) throw err;
    console.error(`Fehler bei internalLeaveFamily für User ${uid} in Familie ${familyId}:`, err);
  }
}

// ─── Sicherer Join-Flow via Cloud Function ────────────────────────────────────
// Verhindert direkten Firestore-Zugriff auf alle Familien und ermöglicht
// serverseitiges Rate-Limiting gegen Brute-Force-Versuche auf Join-Codes.
exports.joinFamilyByCode = onCall(
  { region: "europe-west3", secrets: ["PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const rawCode = request.data?.code;
    if (!rawCode || typeof rawCode !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_CODE");
    }
    const code = rawCode.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6);
    if (code.length !== 6) {
      throw new HttpsError("invalid-argument", "INVALID_CODE");
    }

    // Rate-Limiting: max. 5 Join-Versuche pro UID pro Minute, max. 10 pro Tag
    try {
      const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
      const isAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();

      if (!isAdmin) {
        const minuteLimited = await checkSingleRateLimit(`join_${uid}_m`, 60 * 1000, 5);
        if (minuteLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`join_${uid}_d`, 24 * 60 * 60 * 1000, 10);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      } else {
        console.log(`Bypassing join rate limit for admin UID: ${uid}`);
        // Bootstrap: _admins-Eintrag anlegen falls noch nicht vorhanden
        if (!adminDoc.exists && uid === primaryAdminUidSecret.value()) {
          await admin.firestore().collection("_admins").doc(uid).set({
            email: request.auth.token.email || "",
            promotedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      }
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Rate-Limit-Check fehlgeschlagen (wird ignoriert):", err);
    }

    const snapshot = await admin.firestore()
      .collection("families")
      .where("joinCode", "==", code)
      .limit(1)
      .get();

    if (snapshot.empty) {
      throw new HttpsError("not-found", "FAMILY_NOT_FOUND");
    }

    const familyId = snapshot.docs[0].id;

    // H7: Falls der User bereits in einer anderen Familie war, sauber austragen
    const userDocRef = admin.firestore().collection("users").doc(uid);
    const userDoc = await userDocRef.get();
    const oldFamilyId = userDoc.exists ? userDoc.data()?.familyId : null;
    if (oldFamilyId && oldFamilyId !== familyId) {
      await internalLeaveFamily(uid, oldFamilyId);
    }

    // Security Fix: Write familyId to users collection server-side
    await userDocRef.set({ familyId }, { merge: true });
    // Keep userIds array in sync for Firestore Security Rules (read permission)
    await admin.firestore().collection("families").doc(familyId).update({
      userIds: admin.firestore.FieldValue.arrayUnion(uid)
    });

    // Feature #4: Bestehende Members über neuen Beitritt informieren (fire-and-forget)
    const displayName = request.auth.token.name || request.auth.token.email?.split("@")[0] || "Someone";
    notifyFamilyMemberJoined(familyId, displayName, uid).catch(err =>
      console.warn("notifyFamilyMemberJoined failed (non-critical):", err?.message)
    );

    return { familyId, joinCode: code };
  }
);

// ─── Sicheres Familie-Erstellen via Cloud Function ────────────────────────────
// Generiert den joinCode serverseitig und schreibt das Familie-Dokument.
// Verhindert client-seitigen Query auf die families-Collection für Eindeutigkeitsprüfung.
exports.createFamily = onCall(
  { region: "europe-west3", secrets: ["PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const familyName = request.data?.familyName;
    if (!familyName || typeof familyName !== "string" || familyName.trim().length === 0) {
      throw new HttpsError("invalid-argument", "INVALID_FAMILY_NAME");
    }
    const sanitizedName = familyName.trim().slice(0, 64);

    // Rate-Limiting: max. 3 Family-Erstellungen pro UID pro Stunde, max. 6 pro Tag
    try {
      const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
      const isAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();

      if (!isAdmin) {
        const hourLimited = await checkSingleRateLimit(`create_${uid}_h`, 60 * 60 * 1000, 3);
        if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`create_${uid}_d`, 24 * 60 * 60 * 1000, 6);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      } else {
        console.log(`Bypassing create rate limit for admin UID: ${uid}`);
        // Bootstrap: _admins-Eintrag anlegen falls noch nicht vorhanden.
        // E-Mail-Prüfung entfällt – die äußere Bedingung (PRIMARY_ADMIN_UID) ist ausreichend.
        if (!adminDoc.exists && uid === primaryAdminUidSecret.value()) {
          await admin.firestore().collection("_admins").doc(uid).set({
            email: request.auth.token.email || "",
            promotedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      }
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Rate-Limit-Check fehlgeschlagen (wird ignoriert):", err);
    }

    // Eindeutigen 6-stelligen alphanumerischen Join-Code generieren (max. 5 Versuche)
    const CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // ohne 0/O und 1/I zur Verwechslungsvermeidung
    let joinCode = null;
    let attempts = 0;

    while (attempts < 5) {
      const candidate = Array.from({ length: 6 }, () =>
        CHARS.charAt(randomInt(CHARS.length))
      ).join("");

      try {
        const existing = await admin.firestore()
          .collection("families")
          .where("joinCode", "==", candidate)
          .limit(1)
          .get();

        if (existing.empty) {
          joinCode = candidate;
          break;
        }
      } catch (queryErr) {
        console.error(`Code uniqueness check failed (attempt ${attempts}):`, queryErr.message);
        // Treat as if code is taken → try again
      }
      attempts++;
    }

    if (!joinCode) {
      throw new HttpsError("internal", "CODE_GENERATION_FAILED");
    }

    // Familie-Dokument anlegen
    const familyData = {
      name: sanitizedName,
      joinCode,
      createdByUserId: uid,
      userIds: [uid],
      isAlarmEnabled: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    const docRef = await admin.firestore().collection("families").add(familyData);
    const familyId = docRef.id;

    // H7: Falls der User bereits in einer anderen Familie war, sauber austragen
    const userDocRef = admin.firestore().collection("users").doc(uid);
    const userDoc = await userDocRef.get();
    const oldFamilyId = userDoc.exists ? userDoc.data()?.familyId : null;
    if (oldFamilyId && oldFamilyId !== familyId) {
      await internalLeaveFamily(uid, oldFamilyId);
    }

    // Security Fix: Write familyId to users collection server-side
    await userDocRef.set({ familyId }, { merge: true });

    // Security: joinCode nicht loggen – ist ein Zugangsdaten-Äquivalent.
    console.log(`Family '${sanitizedName}' created by ${uid} with id ${familyId}`);

    return { familyId, joinCode };
  }
);

// ─── Familie verlassen ───────────────────────────────────────────────────────
exports.leaveFamily = onCall(
  { region: "europe-west3" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const { familyId, memberId } = request.data || {};
    if (!familyId || typeof familyId !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_FAMILY_ID");
    }

    const userDocRef = admin.firestore().collection("users").doc(uid);
    const userDoc = await userDocRef.get();

    if (!userDoc.exists || userDoc.data().familyId !== familyId) {
      throw new HttpsError("failed-precondition", "NOT_A_MEMBER_OF_THIS_FAMILY");
    }

    await internalLeaveFamily(uid, familyId, memberId);

    // Remove familyId from user's document
    await userDocRef.update({ familyId: admin.firestore.FieldValue.delete() });

    console.log(`User ${uid} successfully left family ${familyId}.`);

    return { success: true, familyDeleted: false };
  }
);

// ─── Familie löschen (Ersteller oder Global Admin) ───────────────────────────
exports.deleteFamily = onCall(
  { region: "europe-west3", secrets: ["PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const { familyId } = request.data || {};
    if (!familyId || typeof familyId !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_FAMILY_ID");
    }

    const familyDocRef = admin.firestore().collection("families").doc(familyId);
    const familyDoc = await familyDocRef.get();

    if (!familyDoc.exists) {
      throw new HttpsError("not-found", "FAMILY_NOT_FOUND");
    }

    const familyData = familyDoc.data();

    // Global Admin Check
    const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
    const isGlobalAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();
    const isCreator = familyData.createdByUserId === uid;

    // Delete all member documents and remove familyId from their user profiles
    const membersSnapshot = await familyDocRef.collection("members").get();

    // Der Ersteller oder Admin kann die Familie immer löschen.
    // Andere Mitglieder können die Familie nur löschen, wenn keine anderen Mitglieder enthalten sind.
    const otherMembers = membersSnapshot.docs.filter(doc => {
      const claimedUid = doc.data().claimedByUserId;
      return claimedUid ? claimedUid !== uid : true;
    });

    if (!isGlobalAdmin && !isCreator && otherMembers.length > 0) {
      throw new HttpsError("permission-denied", "NON_CREATOR_CANNOT_DELETE_NON_EMPTY_FAMILY");
    }

    const batch = admin.firestore().batch();

    for (const memberDoc of membersSnapshot.docs) {
      const data = memberDoc.data();
      batch.delete(memberDoc.ref); // Delete member document

      const claimedUid = data.claimedByUserId;
      if (claimedUid) {
        // Sicherstellen, dass das Profil nur geleert wird, wenn es noch dieser Familie zugeordnet ist
        const userDocRef = admin.firestore().collection("users").doc(claimedUid);
        const userDoc = await userDocRef.get();
        if (userDoc.exists && userDoc.data().familyId === familyId) {
          batch.set(userDocRef, {
            familyId: admin.firestore.FieldValue.delete()
          }, { merge: true });
        }
      }
    }

    // Recursively delete the family document and its subcollections
    // This is the robust way to ensure everything inside matches/members is gone
    await admin.firestore().recursiveDelete(familyDocRef);
    await batch.commit(); // Commit batch for user profile updates

    console.log(`Family ${familyId} and all its members deleted by ${isGlobalAdmin ? "admin" : "creator"} ${uid}.`);
    return { success: true };
  }
);

// ─── Feature #2: Reihenfolge / Weckzeit geändert ─────────────────────────────
exports.onMemberScheduleChanged = onDocumentWritten(
  {
    document: "families/{familyId}/members/{memberId}",
    region: "europe-west3",
  },
  async (event) => {
    const before = event.data.before?.data();
    const after = event.data.after?.data();

    // Nur bei echten Updates reagieren (kein Create/Delete)
    if (!before || !after) return;

    // Änderungstyp bestimmen: Schedule-Felder vs. Status-Felder
    const scheduleFields = ["earliestWakeUp", "latestWakeUp", "sequenceOrder", "dayProfiles"];
    const scheduleChanged = scheduleFields.some(f => JSON.stringify(before[f]) !== JSON.stringify(after[f]));
    const statusChanged = before.isPaused !== after.isPaused ||
      before.deviceAlarmEnabled !== after.deviceAlarmEnabled;

    if (!scheduleChanged && !statusChanged) return;

    // Ungeclaimte Member bei reinem Reorder skippen: der Push wird durch einen
    // geclaimten Member-Trigger desselben Batch-Writes abgedeckt.
    // Status-Änderungen (z.B. Pause) auf ungeclaimten Membern verarbeiten wir weiterhin.
    if (!after.claimedByUserId && scheduleChanged && !statusChanged) return;

    const familyId = event.params.familyId;

    // Rate-Limit: nur 1 Push pro Familie alle 15 Sekunden.
    // Fängt Batch-Write-Duplikate ab – bei Drag & Drop werden N Members
    // jeweils mit 2s Debounce geschrieben, was gestaffelte Triggers erzeugt.
    const lockRef = admin.firestore().collection("_pushLocks").doc(familyId);
    const now = Date.now();
    let shouldSend = false;
    await admin.firestore().runTransaction(async t => {
      const lockSnap = await t.get(lockRef);
      if (!lockSnap.exists || (now - (lockSnap.data().lastPush || 0)) >= 15000) {
        t.set(lockRef, { lastPush: now });
        shouldSend = true;
      }
    });
    if (!shouldSend) return;

    // Alle Family-Members laden
    const membersSnap = await admin.firestore()
      .collection("families").doc(familyId)
      .collection("members")
      .get();

    // Sender-Erkennung: wer hat die Änderung manuell ausgelöst?
    let changedBy = null;
    const ACTION_WINDOW_MS = 15000;
    
    for (const doc of membersSnap.docs) {
      const uid = doc.data().claimedByUserId;
      if (!uid) continue;
      
      // 1. user_action Subcollection-Dokument prüfen
      let metaSnap = await admin.firestore()
        .collection("users").doc(uid)
        .collection("pushMeta").doc("user_action")
        .get();
      let meta = metaSnap.data();
      
      // 2. reorder Subcollection-Dokument prüfen (Fallback)
      if (!meta) {
        metaSnap = await admin.firestore()
          .collection("users").doc(uid)
          .collection("pushMeta").doc("reorder")
          .get();
        meta = metaSnap.data();
      }
      
      // 3. Altes reorder-Map-Feld im User-Dokument prüfen (Fallback)
      if (!meta) {
        const userSnap = await admin.firestore().collection("users").doc(uid).get();
        const userData = userSnap.data();
        meta = userData?.pushMeta?.reorder;
      }
      
      if (meta?.familyId === familyId &&
        now - (meta?.timestamp?.toMillis?.() || 0) < ACTION_WINDOW_MS) {
        changedBy = uid;
        break;
      }
    }

    // Wenn kein Auslöser gefunden wurde -> Automatische Systemänderung (z.B. Daily Reset) -> Stumm abbrechen
    if (!changedBy) return;

    // Empfängerliste: ALLE geclaimten Members außer dem Auslöser.
    // Kein Positions-Filter: der Schedule wird rückwärts berechnet, daher
    // betrifft JEDE Änderung (Reorder, Pause, Alarm-Toggle, Zeiten) alle.
    // Familienmitglieder sollen auch bei nicht-eigener Betroffenheit informiert werden,
    // damit sie ggf. eingreifen und den Plan anpassen können.
    const recipientUids = [];
    for (const doc of membersSnap.docs) {
      const d = doc.data();
      const uid = d.claimedByUserId;
      if (!uid || uid === changedBy) continue;
      recipientUids.push(uid);
    }

    if (recipientUids.length === 0) return;

    await sendPushToUsers(recipientUids, {
      type: "schedule_change",
    });
  }
);

// ─── Feature #4: Hilfsfunktionen für Join/Leave-Notifications ─────────────────
async function notifyFamilyMemberJoined(familyId, newMemberName, newUid) {
  const membersSnap = await admin.firestore()
    .collection("families").doc(familyId)
    .collection("members")
    .get();

  const recipientUids = [];
  for (const doc of membersSnap.docs) {
    const uid = doc.data().claimedByUserId;
    if (!uid || uid === newUid) continue;
    recipientUids.push(uid);
  }
  if (recipientUids.length === 0) return;

  await sendPushToUsers(recipientUids, {
    type: "family_joined",
    // Android lokalisiert self via type
  });
}

// leftUids: bereits bekannte Liste der verbleibenden UIDs (wird von Caller übergeben)
// Spart einen extra Firestore-Read nach dem arrayRemove.
async function notifyFamilyMemberLeft(recipientUids, leftUid) {
  const filtered = recipientUids.filter(uid => uid !== leftUid);
  if (filtered.length === 0) return;

  await sendPushToUsers(filtered, {
    type: "family_left",
    // Android lokalisiert self via type
  });
}

// ─── Feature #20: Bad ist frei! ──────────────────────────────────────────────
exports.notifyBathroomFree = onCall(
  { region: "europe-west3" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;
    const { familyId, memberId } = request.data || {};
    if (!familyId || !memberId) {
      throw new HttpsError("invalid-argument", "MISSING_PARAMETERS");
    }

    // Rate-Limiting: max. 1 Ruf alle 15 Sekunden pro UID
    const isLimited = await checkSingleRateLimit(`bath_${uid}`, 15 * 1000, 1);
    if (isLimited) {
      throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
    }

    // Prüfen, ob User zur Familie gehört
    const familyRef = admin.firestore().collection("families").doc(familyId);
    const familyDoc = await familyRef.get();
    if (!familyDoc.exists) {
      throw new HttpsError("not-found", "FAMILY_NOT_FOUND");
    }
    const userIds = familyDoc.data().userIds || [];
    if (!userIds.includes(uid)) {
      throw new HttpsError("permission-denied", "NOT_A_MEMBER");
    }

    // Mitglieder abrufen und sortieren
    const membersSnap = await familyRef.collection("members").get();
    const members = membersSnap.docs.map(d => ({ id: d.id, ...d.data() }));

    const sender = members.find(m => m.id === memberId);
    if (!sender) {
      throw new HttpsError("not-found", "MEMBER_NOT_FOUND");
    }

    // Aktive Mitglieder nach Reihenfolge sortieren (sequenceOrder, dann createdAt)
    const activeMembers = members
      .filter(m => !m.isPaused)
      .sort((a, b) => (a.sequenceOrder ?? 0) - (b.sequenceOrder ?? 0) || (a.createdAt ?? 0) - (b.createdAt ?? 0));

    const senderIdx = activeMembers.findIndex(m => m.id === memberId);
    // Nächstes aktives Mitglied in der Reihe
    let nextMember = null;
    if (senderIdx !== -1 && senderIdx < activeMembers.length - 1) {
      nextMember = activeMembers[senderIdx + 1];
    }

    const now = Date.now();
    // Firestore Familien-Dokument aktualisieren für Live-Sync
    await familyRef.update({
      bathroomFreeInfo: {
        freedByMemberId: memberId,
        freedByName: sender.name || "",
        nextMemberId: nextMember ? nextMember.id : null,
        nextMemberName: nextMember ? nextMember.name : null,
        timestamp: now,
      }
    });

    // Wenn nachfolgende Person geclaimt ist und eine UID hat: Gezielten Push senden!
    if (nextMember && nextMember.claimedByUserId && nextMember.claimedByUserId !== uid) {
      await sendPushToUsers([nextMember.claimedByUserId], {
        type: "bathroom_free",
        senderName: sender.name || "",
      });
    }

    return {
      success: true,
      nextMemberName: nextMember ? nextMember.name : null,
    };
  }
);
