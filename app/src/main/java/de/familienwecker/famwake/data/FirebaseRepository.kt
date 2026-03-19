package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.FamilyMember
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalTime


class FamilyNotFoundException : Exception()
class CodeGenerationFailedException : Exception()

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    fun getAuthStateFlow(): kotlinx.coroutines.flow.Flow<com.google.firebase.auth.FirebaseUser?> = kotlinx.coroutines.flow.callbackFlow {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * H-5: Familie-Erstellung über Cloud Function.
     * Die Function generiert den eindeutigen joinCode serverseitig und schreibt das Familie-Dokument.
     * Verhindert den client-seitigen joinCode-Uniqueness-Check, der globalen Lesezugriff erfordert.
     */
    suspend fun createFamily(familyName: String, userId: String): Result<Pair<String, String>> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            val data = hashMapOf(
                "familyName" to familyName,
                "userId" to userId
            )
            val result = functions
                .getHttpsCallable("createFamily")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any>
                ?: return Result.failure(CodeGenerationFailedException())

            val familyId = map["familyId"] as? String
                ?: return Result.failure(CodeGenerationFailedException())
            val joinCode = map["joinCode"] as? String
                ?: return Result.failure(CodeGenerationFailedException())

            Result.success(Pair(familyId, joinCode))
        } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
            when (e.code) {
                com.google.firebase.functions.FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                else ->
                    if (e.message?.contains("CODE_GENERATION_FAILED", ignoreCase = true) == true)
                        Result.failure(CodeGenerationFailedException())
                    else
                        Result.failure(e)
            }
        } catch (e: Exception) {
            if (e.message?.contains("CODE_GENERATION_FAILED", ignoreCase = true) == true) {
                Result.failure(CodeGenerationFailedException())
            } else if (e.message?.contains("TOO_MANY_REQUESTS", ignoreCase = true) == true ||
                       e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true) {
                Result.failure(Exception("TOO_MANY_REQUESTS"))
            } else {
                Result.failure(e)
            }
        }
    }


    /**
     * Join-Flow über gesicherte Cloud Function.
     * Die Cloud Function validiert den Code serverseitig und erzwingt Rate-Limiting.
     */
    suspend fun joinFamilyByCode(joinCode: String): Result<Pair<String, String>> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            val data = hashMapOf("code" to joinCode)
            val result = functions
                .getHttpsCallable("joinFamilyByCode")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any>
            val familyId = response?.get("familyId") as? String
            val code = response?.get("joinCode") as? String
            if (familyId != null && code != null) {
                Result.success(Pair(familyId, code))
            } else {
                Result.failure(FamilyNotFoundException())
            }
        } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
            when (e.code) {
                com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND ->
                    Result.failure(FamilyNotFoundException())
                com.google.firebase.functions.FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    Result.failure(Exception("TOO_MANY_REQUESTS"))
                else -> Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun getFamilyName(familyId: String): String? {
        return try {
            val doc = db.collection("families").document(familyId).get().await()
            doc.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFamilyData(familyId: String): de.familienwecker.famwake.model.FamilyData? {
        return try {
            val doc = db.collection("families").document(familyId).get().await()
            val name = doc.getString("name") ?: return null
            val createdByUserId = doc.getString("createdByUserId")
            de.familienwecker.famwake.model.FamilyData(id = familyId, name = name, createdByUserId = createdByUserId)
        } catch (e: Exception) {
            null
        }
    }

    fun getFamilyMembersFlow(familyId: String): Flow<List<FamilyMember>> = callbackFlow {
        val collection = db.collection("families").document(familyId).collection("members")
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseRepository", "Fehler in getFamilyMembersFlow für $familyId: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                android.util.Log.d("FirebaseRepository", "Habe ${snapshot.size()} Dokumente für Familie $familyId empfangen")
                val members = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toFamilyMember()
                    } catch (e: Exception) {
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.e("FirebaseRepository", "Fehler beim Mapping von ${doc.id}: ${e.message}")
                        }
                        null
                    }
                }
                val sortedMembers = members.sortedWith(compareBy({ it.sequenceOrder }, { it.createdAt ?: 0L }))
                trySend(sortedMembers)
            }
        }

        awaitClose { subscription.remove() }
    }

    suspend fun addOrUpdateMember(familyId: String, member: FamilyMember) {
        try {
            val currentTime = System.currentTimeMillis()
            val docRef = db.collection("families").document(familyId)
                .collection("members").document(member.id)
            val existingCreatedAt = member.createdAt ?: currentTime

            // dayProfiles: Map<Int, DayProfile> → Map<String, Map<String, Any?>>
            val dayProfilesData = member.dayProfiles?.mapKeys { it.key.toString() }
                ?.mapValues { (_, profile) ->
                    mapOf(
                        "isActive" to profile.isActive,
                        "earliestWakeUp" to profile.earliestWakeUp.toString(),
                        "latestWakeUp" to profile.latestWakeUp.toString(),
                        "bathroomDurationMinutes" to profile.bathroomDurationMinutes,
                        "wantsBreakfast" to profile.wantsBreakfast,
                        "leaveHomeTime" to profile.leaveHomeTime?.toString()
                    )
                }

            val data = hashMapOf(
                "name" to member.name,
                "earliestWakeUp" to member.earliestWakeUp.toString(),
                "latestWakeUp" to member.latestWakeUp.toString(),
                "bathroomDurationMinutes" to member.bathroomDurationMinutes,
                "wantsBreakfast" to member.wantsBreakfast,
                "leaveHomeTime" to member.leaveHomeTime?.toString(),
                "isPaused" to member.isPaused,
                "isAwakeToday" to member.isAwakeToday,
                "lastResetDate" to member.lastResetDate,
                "claimedByUserId" to member.claimedByUserId,
                "claimedByUserName" to member.claimedByUserName,
                "sequenceOrder" to member.sequenceOrder,
                "createdAt" to existingCreatedAt,
                "lastUpdatedAt" to FieldValue.serverTimestamp(),
                "deviceAlarmEnabled" to member.deviceAlarmEnabled,
                "dayProfiles" to dayProfilesData
            )
            docRef.set(data).await()
            android.util.Log.i("FirebaseRepository", "Mitglied ${member.id} ('${member.name}') erfolgreich in Familie $familyId gespeichert")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Fehler beim Speichern von Member ${member.id} in Familie $familyId: ${e.message}", e)
            throw e
        }
    }

    suspend fun removeMember(familyId: String, id: String): Result<Unit> {
        return try {
            db.collection("families").document(familyId).collection("members").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun claimMember(familyId: String, memberId: String, userId: String, userName: String?): Boolean {
        return try {
            val docRef = db.collection("families").document(familyId).collection("members").document(memberId)
            // Atomare Transaktion: verhindert Race Condition wenn zwei User
            // gleichzeitig dasselbe Profil beanspruchen wollen.
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val existingClaim = snapshot.getString("claimedByUserId")
                if (existingClaim == null || existingClaim == userId) {
                    transaction.update(docRef, mapOf(
                        "claimedByUserId" to userId,
                        "claimedByUserName" to userName
                    ))
                    true
                } else {
                    false
                }
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unclaimMember(familyId: String, memberId: String, userId: String): Boolean {
        return try {
            val docRef = db.collection("families").document(familyId).collection("members").document(memberId)
            // Atomare Transaktion: verhindert Race Condition beim unclaimen
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val existingClaim = snapshot.getString("claimedByUserId")
                if (existingClaim == userId) {
                    transaction.update(
                        docRef,
                        mapOf(
                            "claimedByUserId" to null,
                            "claimedByUserName" to null
                        )
                    )
                    true
                } else {
                    false
                }
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    private fun generateJoinCode(): String {
        // Base32 ohne verwechselbare Zeichen (0, O, 1, I)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val secureRandom = java.security.SecureRandom()
        return (1..6).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
    }

    suspend fun requestAdminStatsReport(): Result<Unit> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            functions.getHttpsCallable("sendAdminStatsReport").call().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserFamily(userId: String, familyId: String): Result<Unit> {
        return try {
            val data = hashMapOf("familyId" to familyId)
            db.collection("users").document(userId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseRepository", "Fehler beim Speichern der User-Family-Zuordnung für $userId: ${e.message}")
            }
            Result.failure(e)
        }
    }

    // cachedJoinCode als Fallback, falls das Firestore-Family-Dokument nicht gelesen werden kann
    suspend fun getUserFamily(userId: String, cachedJoinCode: String? = null): Result<Pair<String, String>?> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val familyId = doc.getString("familyId")
                if (familyId != null) {
                    val familyDoc = db.collection("families").document(familyId).get().await()
                    val joinCode = familyDoc.getString("joinCode") ?: cachedJoinCode
                    if (joinCode != null) {
                        Result.success(Pair(familyId, joinCode))
                    } else {
                        Result.success(null)
                    }
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeUserFamily(userId: String) {
        try {
            db.collection("users").document(userId).delete().await()
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseRepository", "Fehler beim Entfernen der User-Family-Zuordnung für $userId: ${e.message}")
            }
        }
    }

    suspend fun checkFamilyExists(familyId: String): Boolean {
        return try {
            val doc = db.collection("families").document(familyId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun checkIsGlobalAdminFlow(uid: String): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.callbackFlow {
        val docRef = db.collection("_admins").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, _ ->
            // Fallback auf PRIMARY_ADMIN_UID für absolute Sicherheit direkt im Code
            val isGlobal = snapshot?.exists() == true || uid == "yqmtXyDNQCa5ajCvL9LEWbVgJmF2"
            trySend(isGlobal)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getClaimedMember(familyId: String, userId: String): FamilyMember? {
        return try {
            val snapshot = db.collection("families").document(familyId)
                .collection("members")
                .whereEqualTo("claimedByUserId", userId)
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                // Zentrales Mapping via Extension-Funktion
                snapshot.documents.first().toFamilyMember()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFamily(familyId: String, userId: String): Result<Unit> {
        return try {
            val familyRef = db.collection("families").document(familyId)
            val membersCollection = familyRef.collection("members")
            val userRef = db.collection("users").document(userId)
            val membersSnapshot = membersCollection.get().await()

            if (membersSnapshot.documents.isNotEmpty()) {
                // Erst alle fremden Claims entfernen
                val claimedByOthers = membersSnapshot.documents.filter { doc ->
                    val claimed = doc.getString("claimedByUserId")
                    claimed != null && claimed != userId
                }
                if (claimedByOthers.isNotEmpty()) {
                    val unclaimBatch = db.batch()
                    claimedByOthers.forEach { doc ->
                        unclaimBatch.update(doc.reference, mapOf(
                            "claimedByUserId" to null,
                            "claimedByUserName" to null
                        ))
                    }
                    unclaimBatch.commit().await()
                }

                // Alle Members löschen
                membersSnapshot.documents.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            }

            // Atomares Löschen: Familie-Dokument UND User-Mapping
            val finalBatch = db.batch()
            finalBatch.delete(familyRef)
            finalBatch.delete(userRef)
            finalBatch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verlässt eine Familie atomar: löscht das Member-Dokument und das User-Mapping in einem Batch.
     */
    suspend fun leaveFamilyBatch(userId: String, familyId: String, memberId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            val memberRef = db.collection("families").document(familyId).collection("members").document(memberId)
            val userRef = db.collection("users").document(userId)
            
            batch.delete(memberRef)
            batch.delete(userRef)
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualisiert die Reihenfolge mehrerer Mitglieder atomar in einem Batch.
     */
    suspend fun updateMemberOrders(familyId: String, orders: Map<String, Int>) {
        try {
            val batch = db.batch()
            val collection = db.collection("families").document(familyId).collection("members")
            
            orders.forEach { (memberId, order) ->
                val docRef = collection.document(memberId)
                batch.update(docRef, "sequenceOrder", order, "lastUpdatedAt", FieldValue.serverTimestamp())
            }
            
            batch.commit().await()
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseRepository", "Fehler beim Batch-Update der Reihenfolge: ${e.message}")
            }
        }
    }

    /**
     * Aktualisiert mehrere Mitglieder-Dokumente in einem einzigen Batch.
     * Optimiert für den täglichen Reset oder Massen-Updates.
     */
    suspend fun updateMembersBatch(familyId: String, members: List<FamilyMember>) {
        try {
            val familyDocRef = db.collection("families").document(familyId)
            val membersColl = familyDocRef.collection("members")
            
            // Firestore limit: 500 operations per batch
            members.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { member ->
                    val docRef = membersColl.document(member.id)
                    val currentTime = System.currentTimeMillis()
                    val existingCreatedAt = member.createdAt ?: currentTime

                    val dayProfilesData = member.dayProfiles?.mapKeys { it.key.toString() }
                        ?.mapValues { (_, profile) ->
                            mapOf(
                                "isActive" to profile.isActive,
                                "earliestWakeUp" to profile.earliestWakeUp.toString(),
                                "latestWakeUp" to profile.latestWakeUp.toString(),
                                "bathroomDurationMinutes" to profile.bathroomDurationMinutes,
                                "wantsBreakfast" to profile.wantsBreakfast,
                                "leaveHomeTime" to profile.leaveHomeTime?.toString()
                            )
                        }

                    val data = hashMapOf(
                        "name" to member.name,
                        "earliestWakeUp" to member.earliestWakeUp.toString(),
                        "latestWakeUp" to member.latestWakeUp.toString(),
                        "bathroomDurationMinutes" to member.bathroomDurationMinutes,
                        "wantsBreakfast" to member.wantsBreakfast,
                        "leaveHomeTime" to member.leaveHomeTime?.toString(),
                        "isPaused" to member.isPaused,
                        "isAwakeToday" to member.isAwakeToday,
                        "lastResetDate" to member.lastResetDate,
                        "claimedByUserId" to member.claimedByUserId,
                        "claimedByUserName" to member.claimedByUserName,
                        "sequenceOrder" to member.sequenceOrder,
                        "createdAt" to existingCreatedAt,
                        "lastUpdatedAt" to FieldValue.serverTimestamp(),
                        "deviceAlarmEnabled" to member.deviceAlarmEnabled,
                        "dayProfiles" to dayProfilesData
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseRepository", "Fehler im updateMembersBatch für $familyId: ${e.message}")
            }
            throw e
        }
    }

    /**
     * Schreibt nur das Feld 'deviceAlarmEnabled' für das eigene Mitglieds-Dokument.
     */
    suspend fun updateDeviceAlarmEnabled(familyId: String, memberId: String, enabled: Boolean) {
        try {
            db.collection("families").document(familyId)
                .collection("members").document(memberId)
                .update("deviceAlarmEnabled", enabled)
                .await()
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseRepository", "Fehler beim Schreiben von deviceAlarmEnabled für $memberId: ${e.message}")
            }
        }
    }

    /**
     * Erzeugt einen Flow, der den Synchronisationsstatus überwacht.
     * Kombiniert members-Subkollektion UND das families-Dokument selbst.
     */
    fun getSyncStatusFlow(familyId: String): Flow<de.familienwecker.famwake.model.SyncStatus> = callbackFlow {
        val familyRef = db.collection("families").document(familyId)
        val membersRef = familyRef.collection("members")

        var familySynced = de.familienwecker.famwake.model.SyncStatus()
        var membersSynced = de.familienwecker.famwake.model.SyncStatus()

        fun emitCombined() {
            trySend(
                de.familienwecker.famwake.model.SyncStatus(
                    isFromCache = familySynced.isFromCache || membersSynced.isFromCache,
                    hasPendingWrites = familySynced.hasPendingWrites || membersSynced.hasPendingWrites
                )
            )
        }

        val familySub = familyRef.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                familySynced = de.familienwecker.famwake.model.SyncStatus(
                    isFromCache = snapshot.metadata.isFromCache(),
                    hasPendingWrites = snapshot.metadata.hasPendingWrites()
                )
                emitCombined()
            }
        }

        val membersSub = membersRef.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                membersSynced = de.familienwecker.famwake.model.SyncStatus(
                    isFromCache = snapshot.metadata.isFromCache(),
                    hasPendingWrites = snapshot.metadata.hasPendingWrites()
                )
                emitCombined()
            }
        }

        awaitClose {
            familySub.remove()
            membersSub.remove()
        }
    }
}
