package de.familienwecker.famwake.data

import android.util.Log
import de.familienwecker.famwake.model.FamilyData
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.SyncStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.functions.android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.toInstant

class FirebaseRepository : IFirebaseRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth = Firebase.auth

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val FIREBASE_REGION = "europe-west3"
        private const val COLLECTION_ADMINS = "_admins"
        private const val COLLECTION_FAMILIES = "families"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_MEMBERS = "members"

        // S2: Debug-Logging-Flag – wird aus FamWakeApplication via BuildConfig.DEBUG gesetzt.
        // Vermeidet BuildConfig-Dependency im shared-Modul; verhindert Log-Leaks im Release-Build.
        var debugLogging: Boolean = false

        /**
         * O7: Persistente Offline-Persistenz aktivieren – Writes überleben App-Neustart.
         * Muss einmalig VOR dem ersten Firestore-Aufruf gesetzt werden (z.B. in Application.onCreate).
         */
        fun configurePersistentCache() {
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                    .build()
                com.google.firebase.firestore.FirebaseFirestore.getInstance().firestoreSettings = settings
            } catch (e: Exception) {
                if (debugLogging) Log.w(TAG, "Firestore PersistentCache already configured or failed: ${e.message}")
            }
        }
    }

    override fun getAuthStateFlow() = auth.authStateChanged

    // ── Familie ──────────────────────────────────────────────────────────────

    override suspend fun createFamily(familyName: String, userId: String): Result<Pair<String, String>> {
        return try {
            val functions = Firebase.functions(FIREBASE_REGION)
            val data = mapOf("familyName" to familyName, "userId" to userId)
            @Suppress("UNCHECKED_CAST")
            val result = functions.httpsCallable("createFamily").invoke(data).android.data as? Map<String, Any>
                ?: return Result.failure(CodeGenerationFailedException())
            val familyId = result["familyId"] as? String
                ?: return Result.failure(CodeGenerationFailedException())
            val joinCode = result["joinCode"] as? String
                ?: return Result.failure(CodeGenerationFailedException())
            Result.success(Pair(familyId, joinCode))
        } catch (e: FirebaseFunctionsException) {
            when {
                e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                e.message?.contains("CODE_GENERATION_FAILED", ignoreCase = true) == true ->
                    Result.failure(CodeGenerationFailedException())
                else -> Result.failure(e)
            }
        } catch (e: Exception) {
            when {
                e.message?.contains("CODE_GENERATION_FAILED", ignoreCase = true) == true ->
                    Result.failure(CodeGenerationFailedException())
                e.message?.contains("TOO_MANY_REQUESTS", ignoreCase = true) == true ||
                e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                else -> Result.failure(e)
            }
        }
    }

    override suspend fun joinFamilyByCode(joinCode: String): Result<Pair<String, String>> {
        return try {
            val functions = Firebase.functions(FIREBASE_REGION)
            val data = mapOf("code" to joinCode)
            @Suppress("UNCHECKED_CAST")
            val resultData = functions.httpsCallable("joinFamilyByCode").invoke(data).android.data as? Map<String, Any>
            val familyId = resultData?.get("familyId") as? String
            val code = resultData?.get("joinCode") as? String
            if (familyId != null && code != null) {
                Result.success(Pair(familyId, code))
            } else {
                Result.failure(FamilyNotFoundException())
            }
        } catch (e: FirebaseFunctionsException) {
            when {
                e.message?.contains("NOT_FOUND", ignoreCase = true) == true ->
                    Result.failure(FamilyNotFoundException())
                e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                else -> Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFamilyName(familyId: String): String? {
        return try {
            db.collection(COLLECTION_FAMILIES).document(familyId).get().get<String?>("name")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getFamilyData(familyId: String): FamilyData? {
        return try {
            val doc = db.collection(COLLECTION_FAMILIES).document(familyId).get()
            val name: String = doc.get<String?>("name") ?: return null
            val createdByUserId: String? = doc.get<String?>("createdByUserId")
            // Nullable Long: Alte Familien haben das Feld nicht → Default 0
            val globalBufferMinutes: Long = doc.get<Long?>("globalBufferMinutes") ?: 0L
            FamilyData(id = familyId, name = name, createdByUserId = createdByUserId, globalBufferMinutes = globalBufferMinutes)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserContext(uid: String): Result<Pair<String, String>?> {
        return try {
            val functions = Firebase.functions(FIREBASE_REGION)
            @Suppress("UNCHECKED_CAST")
            val resultData = functions.httpsCallable("getUserContext").invoke(mapOf<String, Any>()).android.data as? Map<String, Any?>
            val familyId = resultData?.get("familyId") as? String
            val joinCode = resultData?.get("joinCode") as? String
            if (familyId != null && joinCode != null) {
                Result.success(Pair(familyId, joinCode))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "getUserContext error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getUserFamily(uid: String, cachedJoinCode: String?): Result<Pair<String, String>?> {
        return try {
            val userDoc = db.collection(COLLECTION_USERS).document(uid).get()
            var familyId: String? = userDoc.get<String?>("familyId")

            if (familyId == null) {
                // Kein familyId in users/{uid} → Collection-Query als Fallback
                val queryResults = db.collection(COLLECTION_FAMILIES)
                    .where { "userIds" equalTo uid }
                    .limit(1)
                    .get()
                val doc = queryResults.documents.firstOrNull()
                if (doc != null) {
                    val joinCode = doc.get<String?>("joinCode")
                    return if (joinCode != null) Result.success(Pair(doc.id, joinCode))
                    else Result.success(null)
                }
                return Result.success(null)
            }

            // Short-Circuit: familyId bekannt + Code gecacht => kein zweiter Read nötig
            if (cachedJoinCode != null) {
                return Result.success(Pair(familyId, cachedJoinCode))
            }

            // Kein gecachter Code → Familie-Dokument lesen
            val familyDoc = db.collection(COLLECTION_FAMILIES).document(familyId).get()
            if (familyDoc.exists) {
                val joinCode = familyDoc.get<String?>("joinCode")
                if (joinCode != null) Result.success(Pair(familyId, joinCode))
                else Result.success(null)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    override suspend fun checkFamilyExists(familyId: String): Boolean {
        return try {
            db.collection(COLLECTION_FAMILIES).document(familyId).get().exists
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeUserFamily(userId: String, familyId: String): Result<Unit> {
        return try {
            val functions = Firebase.functions(FIREBASE_REGION)
            val data = mapOf("familyId" to familyId)
            functions.httpsCallable("leaveFamily").invoke(data)
            Result.success(Unit)
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler beim Verlassen der Familie für $userId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteFamily(familyId: String, userId: String): Result<Unit> {
        return try {
            val functions = Firebase.functions(FIREBASE_REGION)
            val data = mapOf("familyId" to familyId)
            functions.httpsCallable("deleteFamily").invoke(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveFamilyBatch(userId: String, familyId: String, memberId: String): Result<Unit> {
        return try {
            val functions = Firebase.functions(FIREBASE_REGION)
            val data = mapOf("familyId" to familyId, "memberId" to memberId)
            functions.httpsCallable("leaveFamily").invoke(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncLocalFamilyToCloud(
        localFamilyId: String,
        familyName: String,
        members: List<FamilyMember>,
        userId: String
    ): Result<Pair<String, String>> {
        return try {
            // 1. Echte Familie via Cloud Function anlegen (identisch zu createFamily)
            val functions = Firebase.functions(FIREBASE_REGION)
            val data = mapOf("familyName" to familyName, "userId" to userId)
            @Suppress("UNCHECKED_CAST")
            val result = functions.httpsCallable("createFamily").invoke(data).android.data as? Map<String, Any>
                ?: return Result.failure(CodeGenerationFailedException())
            val newFamilyId = result["familyId"] as? String
                ?: return Result.failure(CodeGenerationFailedException())
            val joinCode = result["joinCode"] as? String
                ?: return Result.failure(CodeGenerationFailedException())

            // 2. Lokal erstellte Members in die neue Familie hochladen (Batch-Write)
            if (members.isNotEmpty()) {
                val membersColl = db.collection(COLLECTION_FAMILIES).document(newFamilyId)
                    .collection(COLLECTION_MEMBERS)
                members.chunked(500).forEach { chunk ->
                    db.batch().run {
                        chunk.forEach { member ->
                            set(membersColl.document(member.id), member.toFirestoreMap())
                        }
                        commit()
                    }
                }
            }

            if (debugLogging) Log.i(TAG, "syncLocalFamilyToCloud: $localFamilyId → $newFamilyId (${members.size} members)")
            Result.success(Pair(newFamilyId, joinCode))
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "syncLocalFamilyToCloud failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Mitglieder ────────────────────────────────────────────────────────────

    override fun getFamilyMembersFlow(familyId: String): Flow<List<FamilyMember>> = callbackFlow {
        val query = db.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_MEMBERS)
            .orderBy("sequenceOrder", Direction.ASCENDING)

        query.snapshots.collect { snapshot ->
            val members = snapshot.documents.mapNotNull { doc ->
                try { doc.toFamilyMember() } catch (e: Exception) {
                    if (debugLogging) Log.e(TAG, "toFamilyMember failed for doc ${doc.id}: ${e.message}", e)
                    null
                }
            }
            trySend(members)
        }
        awaitClose { }
    }.retryWhen { cause, attempt ->
        // PERMISSION_DENIED ist permanent (Familie gelöscht/gewechselt) → nicht retrien
        val isPermissionError = cause.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
        if (isPermissionError || attempt >= 5) {
            if (debugLogging) Log.w(TAG, "getFamilyMembersFlow giving up (attempt=$attempt, permission=$isPermissionError): ${cause.message}")
            false
        } else {
            val delayMillis = minOf(1000L * (attempt + 1), 10000L)
            if (debugLogging) Log.w(TAG, "getFamilyMembersFlow retry (attempt=$attempt): ${cause.message}")
            delay(delayMillis)
            true
        }
    }.distinctUntilChanged { old, new ->
        // Snapshot als identisch werten wenn Anzahl, IDs und alle relevanten Felder gleich sind.
        // Verhindert redundante Room-Writes bei Firestore-Metadaten-Updates (z.B. hasPendingWrites).
        // WICHTIG: lastUpdatedAt allein reicht nicht – Claim und Erstellung können in derselben
        // Sekunde passieren (Timestamp.seconds * 1000L), was identische Werte ergibt und den
        // Claim-Update fälschlicherweise herausfiltert. Deshalb auch claimedByUserId + isPaused prüfen.
        old.size == new.size &&
        old.zip(new).all { (a, b) ->
            a.id == b.id &&
            a.lastUpdatedAt == b.lastUpdatedAt &&
            a.claimedByUserId == b.claimedByUserId &&
            a.isPaused == b.isPaused &&
            a.isAwakeToday == b.isAwakeToday &&
            a.deviceAlarmEnabled == b.deviceAlarmEnabled &&
            a.snoozeUntil == b.snoozeUntil &&
            a.snoozeCount == b.snoozeCount
        }
    }

    override suspend fun addOrUpdateMember(familyId: String, member: FamilyMember) {
        try {
            db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(member.id)
                .set(member.toFirestoreMap())
            if (debugLogging) Log.i(TAG, "Mitglied ${member.id} erfolgreich gespeichert")
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler beim Speichern von Member ${member.id} in Familie $familyId: ${e.message}", e)
            throw e
        }
    }

    override suspend fun removeMember(familyId: String, id: String): Result<Unit> {
        return try {
            db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(id).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "removeMember: failed for $id: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun claimMember(familyId: String, memberId: String, userId: String, userName: String?, deviceId: String, force: Boolean): Boolean {
        return try {
            val docRef = db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
            db.runTransaction {
                val snapshot = get(docRef)
                @Suppress("UNCHECKED_CAST")
                val existingClaim = (snapshot.get("claimedByUserId") as? String)
                if (force || existingClaim == null || existingClaim == userId) {
                    update(docRef, mapOf(
                        "claimedByUserId" to userId,
                        "claimedByUserName" to userName,
                        "claimedByDeviceId" to deviceId,
                        "deviceAlarmEnabled" to true,
                        // lastUpdatedAt setzen damit distinctUntilChanged den Snapshot nicht herausfiltert
                        // und Room den Claim-Status korrekt übernimmt.
                        "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
                    ))
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "claimMember failed for $memberId: ${e.message}")
            false
        }
    }

    /** Offline-Variante: Kein Transaction-Roundtrip, Firestore queut den Write bis Reconnect. */
    override suspend fun claimMemberOffline(familyId: String, memberId: String, userId: String, userName: String?, deviceId: String) {
        try {
            val docRef = db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
            docRef.update(mapOf(
                "claimedByUserId" to userId,
                "claimedByUserName" to userName,
                "claimedByDeviceId" to deviceId,
                "deviceAlarmEnabled" to true,
                "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
            ))
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "claimMemberOffline failed for $memberId: ${e.message}")
        }
    }

    override suspend fun unclaimMember(familyId: String, memberId: String, userId: String, deviceId: String): Boolean {
        return try {
            val docRef = db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
            db.runTransaction {
                val snapshot = get(docRef)
                @Suppress("UNCHECKED_CAST")
                val existingClaim = (snapshot.get("claimedByUserId") as? String)
                if (existingClaim == userId) {
                    update(docRef, mapOf(
                        "claimedByUserId" to null,
                        "claimedByUserName" to null,
                        "claimedByDeviceId" to null,
                        // isAwakeToday zurücksetzen – verhindert, dass ein veralteter
                        // "Schon wach"-Status auf den nächsten Claimer vererbt wird.
                        "isAwakeToday" to false,
                        "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
                    ))
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "unclaimMember failed for $memberId: ${e.message}")
            false
        }
    }

    override suspend fun getClaimedMember(familyId: String, userId: String): FamilyMember? {
        return try {
            val snapshot = db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS)
                .where { "claimedByUserId" equalTo userId }
                .limit(1)
                .get()
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.first().toFamilyMember()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateMemberOrders(familyId: String, orders: Map<String, Int>) {
        try {
            val collection = db.collection(COLLECTION_FAMILIES).document(familyId).collection(COLLECTION_MEMBERS)
            db.batch().run {
                orders.forEach { (memberId, order) ->
                    val docRef = collection.document(memberId)
                    update(docRef, mapOf(
                        "sequenceOrder" to order,
                        "lastUpdatedAt"  to FieldValue.serverTimestamp
                    ))
                }
                commit()
            }
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler beim Batch-Update der Reihenfolge: ${e.message}")
            throw e
        }
    }

    override suspend fun updateMemberOrderAndProfiles(familyId: String, members: List<FamilyMember>) {
        try {
            val collection = db.collection(COLLECTION_FAMILIES).document(familyId).collection(COLLECTION_MEMBERS)
            db.batch().run {
                members.forEach { member ->
                    val dayProfilesData = member.dayProfiles?.mapKeys { it.key.toString() }
                        ?.mapValues { (_, profile) ->
                            buildMap<String, Any?> {
                                put("isActive", profile.isActive)
                                put("earliestWakeUp", profile.earliestWakeUp.toString())
                                put("latestWakeUp", profile.latestWakeUp.toString())
                                put("bathroomDurationMinutes", profile.bathroomDurationMinutes)
                                put("wantsBreakfast", profile.wantsBreakfast)
                                profile.leaveHomeTime?.let { put("leaveHomeTime", it.toString()) }
                                profile.bufferMinutes?.let { put("bufferMinutes", it) }
                                put("isSimpleMode", profile.isSimpleMode)
                                profile.sequenceOrder?.let { put("sequenceOrder", it) }
                            }
                        }
                    val docRef = collection.document(member.id)
                    update(docRef, mapOf(
                        "sequenceOrder" to member.sequenceOrder,
                        "dayProfiles" to dayProfilesData,
                        "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
                    ))
                }
                commit()
            }
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler beim Batch-Update von Profilen und Reihenfolge: ${e.message}")
            throw e
        }
    }

    override suspend fun updateMemberDayProfiles(
        familyId: String,
        memberId: String,
        dayProfiles: Map<Int, de.familienwecker.famwake.model.DayProfile>?
    ) {
        try {
            val dayProfilesData = dayProfiles?.mapKeys { it.key.toString() }
                ?.mapValues { (_, profile) ->
                    buildMap<String, Any?> {
                        put("isActive", profile.isActive)
                        put("earliestWakeUp", profile.earliestWakeUp.toString())
                        put("latestWakeUp", profile.latestWakeUp.toString())
                        put("bathroomDurationMinutes", profile.bathroomDurationMinutes)
                        put("wantsBreakfast", profile.wantsBreakfast)
                        profile.leaveHomeTime?.let { put("leaveHomeTime", it.toString()) }
                        profile.bufferMinutes?.let { put("bufferMinutes", it) }
                        put("isSimpleMode", profile.isSimpleMode)
                        profile.sequenceOrder?.let { put("sequenceOrder", it) }
                    }
                }

            db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
                .update(mapOf(
                    "dayProfiles" to dayProfilesData,
                    "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
                ))
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler beim Updaten der dayProfiles für Member $memberId: ${e.message}")
            throw e
        }
    }

    override suspend fun setReorderMeta(uid: String, familyId: String) {
        try {
            db.collection(COLLECTION_USERS).document(uid)
                .collection("pushMeta").document("reorder")
                .set(mapOf(
                    "familyId"  to familyId,
                    "timestamp" to FieldValue.serverTimestamp
                ))
        } catch (e: Exception) {
            // Non-critical: nur für Self-Push-Filter, kein throw
            if (debugLogging) Log.w(TAG, "setReorderMeta failed for $uid: ${e.message}")
        }
    }

    override suspend fun setUserActionMeta(uid: String, familyId: String) {
        try {
            db.collection(COLLECTION_USERS).document(uid)
                .collection("pushMeta").document("user_action")
                .set(mapOf(
                    "familyId"  to familyId,
                    "timestamp" to FieldValue.serverTimestamp
                ))
        } catch (e: Exception) {
            if (debugLogging) Log.w(TAG, "setUserActionMeta failed for $uid: ${e.message}")
        }
    }

    override suspend fun updateMembersBatch(familyId: String, members: List<FamilyMember>) {
        try {
            val membersColl = db.collection(COLLECTION_FAMILIES).document(familyId).collection(COLLECTION_MEMBERS)
            members.chunked(500).forEach { chunk ->
                db.batch().run {
                    chunk.forEach { member ->
                        // Nur koordinative Felder updaten – kein volles .set() um Security Rules
                        // für name/claimedByUserId nicht zu verletzen (betrifft fremde Members).
                        update(membersColl.document(member.id), mapOf(
                            "isPaused" to member.isPaused,
                            "isAwakeToday" to member.isAwakeToday,
                            "lastResetDate" to member.lastResetDate,
                            "snoozeUntil" to member.snoozeUntil?.let {
                                val instant = it.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
                                com.google.firebase.Timestamp(instant.epochSeconds, instant.nanosecondsOfSecond)
                            },
                            "snoozeCount" to member.snoozeCount,
                            "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
                        ))
                    }
                    commit()
                }
            }
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler im updateMembersBatch für $familyId: ${e.message}")
            throw e
        }
    }

    override suspend fun updateDeviceAlarmEnabled(familyId: String, memberId: String, enabled: Boolean) {
        try {
            db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
                .update(mapOf(
                    "deviceAlarmEnabled" to enabled,
                    "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
                ))
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler beim Schreiben von deviceAlarmEnabled für $memberId: ${e.message}")
            throw e
        }
    }

    override suspend fun updateMemberPauseState(familyId: String, memberId: String, isPaused: Boolean) {
        try {
            db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
                .update(mapOf(
                    "isPaused" to isPaused,
                    "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp
                ))
            if (debugLogging) Log.i(TAG, "Pause-Status für $memberId gesetzt: isPaused=$isPaused")
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "Fehler beim Setzen von isPaused für $memberId: ${e.message}")
            throw e
        }
    }

    override suspend fun updateMemberSnoozeState(familyId: String, memberId: String, snoozeUntil: kotlinx.datetime.LocalDateTime?, snoozeCount: Int) {
        try {
            // WICHTIG: Direkt die native Firebase SDK nutzen, NICHT GitLive!
            // GitLive's update() konvertiert com.google.firebase.Timestamp nicht korrekt,
            // weshalb snoozeUntil nie auf anderen Geräten ankam.
            val nativeDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val docRef = nativeDb.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
            val data = hashMapOf<String, Any>(
                "snoozeCount" to snoozeCount,
                "lastUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            val snoozeTimestamp = snoozeUntil?.let {
                val instant = it.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
                com.google.firebase.Timestamp(instant.epochSeconds, instant.nanosecondsOfSecond)
            }
            if (snoozeTimestamp != null) {
                data["snoozeUntil"] = snoozeTimestamp
            } else {
                // Explizit null setzen mit FieldValue.delete() oder direkte Null-Setzung
                data["snoozeUntil"] = com.google.firebase.firestore.FieldValue.delete()
            }
            docRef.update(data).await()
            if (debugLogging) Log.d(TAG, "snoozeState erfolgreich geschrieben für $memberId: until=$snoozeUntil, count=$snoozeCount")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Schreiben von snoozeState für $memberId: ${e.message}")
            throw e
        }
    }

    override suspend fun updateGlobalBufferMinutes(familyId: String, minutes: Long) {
        try {
            db.collection(COLLECTION_FAMILIES).document(familyId)
                .update(mapOf("globalBufferMinutes" to minutes))
        } catch (e: Exception) {
            if (debugLogging) Log.e(TAG, "updateGlobalBufferMinutes failed for $familyId: ${e.message}")
            throw e
        }
    }

    // ── Admin / Status ────────────────────────────────────────────────────────────────

    override fun checkIsGlobalAdminFlow(uid: String): Flow<Boolean> = callbackFlow {
        db.collection(COLLECTION_ADMINS).document(uid).snapshots.collect { snapshot ->
            trySend(snapshot?.exists == true)
        }
        awaitClose { }
    }.retryWhen { cause, attempt ->
        val isPermissionError = cause.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
        if (isPermissionError || attempt >= 5) {
            if (debugLogging) Log.w(TAG, "checkIsGlobalAdminFlow giving up (attempt=$attempt, permission=$isPermissionError): ${cause.message}")
            false
        } else {
            val delayMillis = minOf(1000L * (attempt + 1), 10000L)
            if (debugLogging) Log.w(TAG, "checkIsGlobalAdminFlow retry (attempt=$attempt): ${cause.message}")
            delay(delayMillis)
            true
        }
    }

    override fun getSyncStatusFlow(familyId: String): Flow<SyncStatus> = callbackFlow {
        val familyRef = db.collection(COLLECTION_FAMILIES).document(familyId)
        val membersRef = familyRef.collection(COLLECTION_MEMBERS)

        var familySynced = SyncStatus()
        var membersSynced = SyncStatus()

        fun emitCombined() {
            trySend(SyncStatus(
                isFromCache = familySynced.isFromCache || membersSynced.isFromCache,
                hasPendingWrites = familySynced.hasPendingWrites || membersSynced.hasPendingWrites
            ))
        }

        // #5 Strukturiertes Concurrency: 'this' nutzt den callbackFlow-Scope statt eines
        // manuellen CoroutineScope(Dispatchers.IO) – Jobs werden in awaitClose() sauber gecancelt.
        val familyJob = this.launch {
            familyRef.snapshots.collect { snapshot ->
                familySynced = SyncStatus(
                    isFromCache = snapshot.metadata.isFromCache,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites
                )
                emitCombined()
            }
        }
        val membersJob = this.launch {
            membersRef.snapshots.collect { snapshot ->
                membersSynced = SyncStatus(
                    isFromCache = snapshot.metadata.isFromCache,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites
                )
                emitCombined()
            }
        }

        awaitClose {
            familyJob.cancel()
            membersJob.cancel()
        }
    }.retryWhen { cause, attempt ->
        val isPermissionError = cause.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
        if (isPermissionError || attempt >= 5) {
            if (debugLogging) Log.w(TAG, "getSyncStatusFlow giving up (attempt=$attempt, permission=$isPermissionError): ${cause.message}")
            false
        } else {
            val delayMillis = minOf(1000L * (attempt + 1), 10000L)
            delay(delayMillis)
            true
        }
    }

    override suspend fun requestAdminStatsReport(): Result<Unit> {
        return try {
            Firebase.functions(FIREBASE_REGION).httpsCallable("sendAdminStatsReport").invoke()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Feedback ─────────────────────────────────────────────────────────────

    override suspend fun sendFeedback(
        category: String,
        message: String,
        email: String,
        appVersion: String,
        device: String
    ): Result<Unit> {
        return try {
            val data = mapOf(
                "category" to category,
                "message" to message,
                "email" to email,
                "appVersion" to appVersion,
                "device" to device
            )
            Firebase.functions(FIREBASE_REGION).httpsCallable("sendFeedbackEmail").invoke(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
