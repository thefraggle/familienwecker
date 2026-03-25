package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.FamilyMember
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.time.LocalTime


class FamilyNotFoundException : Exception()
class CodeGenerationFailedException : Exception()

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    companion object {
        private const val COLLECTION_ADMINS = "_admins"
        private const val COLLECTION_FAMILIES = "families"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_MEMBERS = "members"
    }

    fun getAuthStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
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
            val doc = db.collection(COLLECTION_FAMILIES).document(familyId).get().await()
            doc.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFamilyData(familyId: String): de.familienwecker.famwake.model.FamilyData? {
        return try {
            val doc = db.collection(COLLECTION_FAMILIES).document(familyId).get().await()
            val name = doc.getString("name") ?: return null
            val createdByUserId = doc.getString("createdByUserId")
            de.familienwecker.famwake.model.FamilyData(id = familyId, name = name, createdByUserId = createdByUserId)
        } catch (e: Exception) {
            null
        }
    }
    fun getFamilyMembersFlow(familyId: String): kotlinx.coroutines.flow.Flow<List<FamilyMember>> = kotlinx.coroutines.flow.callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        
        fun subscribe() {
            listener?.remove()
            listener = db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS)
                .orderBy("sequenceOrder")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val members = snapshot.documents.mapNotNull { doc ->
                            try { doc.toFamilyMember() } catch (e: Exception) { null }
                        }
                        trySend(members)
                    }
                }
        }

        subscribe()
        awaitClose { listener?.remove() }
    }.retryWhen { cause, attempt: Long ->
        // Bei kritischen Fehlern (z.B. Auth-Verlust) mit Backoff neu versuchen
        if (cause is com.google.firebase.firestore.FirebaseFirestoreException && 
            cause.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            // Bei Permission Denied: Länger warten (5s) und erneut versuchen.
            // Dies ermöglicht "Self-Healing", sobald der User z.B. zum Family-Member wird.
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.w("FirebaseRepository", "PERMISSION_DENIED. Retrying in 5s (Self-healing mode)...")
            }
            delay(5000)
            true 
        } else {
            val delayMillis = kotlin.math.min(1000L * (attempt + 1), 10000L)
            delay(delayMillis)
            true
        }
    }

    suspend fun addOrUpdateMember(familyId: String, member: FamilyMember) {
        try {
            val docRef = db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(member.id)
            docRef.set(member.toFirestoreMap()).await()
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.i("FirebaseRepository", "Mitglied ${member.id} erfolgreich gespeichert")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Fehler beim Speichern von Member ${member.id} in Familie $familyId: ${e.message}", e)
            throw e
        }
    }

    suspend fun removeMember(familyId: String, id: String): Result<Unit> {
        return try {
            db.collection(COLLECTION_FAMILIES).document(familyId).collection(COLLECTION_MEMBERS).document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun claimMember(familyId: String, memberId: String, userId: String, userName: String?): Boolean {
        return try {
            val docRef = db.collection(COLLECTION_FAMILIES).document(familyId).collection(COLLECTION_MEMBERS).document(memberId)
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
            val docRef = db.collection(COLLECTION_FAMILIES).document(familyId).collection(COLLECTION_MEMBERS).document(memberId)
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

    suspend fun requestAdminStatsReport(): Result<Unit> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            functions.getHttpsCallable("sendAdminStatsReport").call().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // M-1: Parallel-Fetching optimiert
    suspend fun getUserFamily(uid: String, cachedJoinCode: String? = null): Result<Pair<String, String>?> = coroutineScope {
        try {
            // M-1: Parallel fetching with async for performance optimization
            val familyIdDeferred = async {
                db.collection("users").document(uid).get().await().getString("familyId")
            }
            val joinCodeDeferred = if (cachedJoinCode == null) {
                async {
                    db.collection(COLLECTION_FAMILIES).whereArrayContains("userIds", uid).get().await()
                        .documents.firstOrNull()?.getString("joinCode")
                }
            } else null

            val familyId = familyIdDeferred.await()
            val joinCode = joinCodeDeferred?.await() ?: cachedJoinCode

            if (familyId != null && joinCode != null) {
                Result.success(Pair(familyId, joinCode))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // M-3: Fehlerpropagierung gefixt
    suspend fun removeUserFamily(userId: String, familyId: String): Result<Unit> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            val data = hashMapOf("familyId" to familyId)
            functions.getHttpsCallable("leaveFamily").call(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseRepository", "Fehler beim Verlassen der Familie für $userId: ${e.message}")
            }
            Result.failure(e)
        }
    }

    suspend fun checkFamilyExists(familyId: String): Boolean {
        return try {
            val doc = db.collection(COLLECTION_FAMILIES).document(familyId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun checkIsGlobalAdminFlow(uid: String): Flow<Boolean> = callbackFlow {
        val docRef = db.collection(COLLECTION_ADMINS).document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("FirebaseRepository", "Admin-Check-Fehler für $uid: ${error.message}")
                }
                trySend(false)
                return@addSnapshotListener
            }
            val isGlobal = snapshot?.exists() == true
            trySend(isGlobal)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getClaimedMember(familyId: String, userId: String): FamilyMember? {
        return try {
            val snapshot = db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS)
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
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            val data = hashMapOf("familyId" to familyId)
            functions.getHttpsCallable("deleteFamily").call(data).await()
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
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            val data = hashMapOf(
                "familyId" to familyId,
                "memberId" to memberId
            )
            functions.getHttpsCallable("leaveFamily").call(data).await()
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
            val collection = db.collection(COLLECTION_FAMILIES).document(familyId).collection(COLLECTION_MEMBERS)
            
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
            val familyDocRef = db.collection(COLLECTION_FAMILIES).document(familyId)
            val membersColl = familyDocRef.collection(COLLECTION_MEMBERS)
            
            // Firestore limit: 500 operations per batch
            members.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { member ->
                    val docRef = membersColl.document(member.id)
                    batch.set(docRef, member.toFirestoreMap())
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
            db.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_MEMBERS).document(memberId)
                .update("deviceAlarmEnabled", enabled)
                .await()
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseRepository", "Fehler beim Schreiben von deviceAlarmEnabled für $memberId: ${e.message}")
            }
            throw e
        }
    }

    /**
     * Erzeugt einen Flow, der den Synchronisationsstatus überwacht.
     * Kombiniert members-Subkollektion UND das families-Dokument selbst.
     */
    fun getSyncStatusFlow(familyId: String): Flow<de.familienwecker.famwake.model.SyncStatus> = callbackFlow {
        val familyRef = db.collection(COLLECTION_FAMILIES).document(familyId)
        val membersRef = familyRef.collection(COLLECTION_MEMBERS)

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
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                familySynced = de.familienwecker.famwake.model.SyncStatus(
                    isFromCache = snapshot.metadata.isFromCache(),
                    hasPendingWrites = snapshot.metadata.hasPendingWrites()
                )
                emitCombined()
            }
        }

        val membersSub = membersRef.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
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
    }.retryWhen { cause, attempt ->
        if (cause is com.google.firebase.firestore.FirebaseFirestoreException && 
            cause.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            delay(5000)
            true
        } else {
            val delayMillis = kotlin.math.min(1000L * (attempt + 1), 10000L)
            delay(delayMillis)
            true
        }
    }

    /**
     * S-1: Sendet Feedback via Cloud Function.
     */
    suspend fun sendFeedback(
        category: String,
        message: String,
        email: String,
        appVersion: String,
        device: String
    ): Result<Unit> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")
            val data = hashMapOf(
                "category" to category,
                "message" to message,
                "email" to email,
                "appVersion" to appVersion,
                "device" to device
            )
            functions.getHttpsCallable("sendFeedbackEmail").call(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
