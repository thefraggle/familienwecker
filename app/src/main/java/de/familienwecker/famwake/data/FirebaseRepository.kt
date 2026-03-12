package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.FamilyMember
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

    suspend fun createFamily(familyName: String, userId: String): Result<Pair<String, String>> {
        return try {
            var joinCode = generateJoinCode()
            var codeExists = true
            var attempts = 0
            
            while (codeExists && attempts < 5) {
                val snapshot = db.collection("families").whereEqualTo("joinCode", joinCode).limit(1).get().await()
                if (snapshot.isEmpty) {
                    codeExists = false
                } else {
                    joinCode = generateJoinCode()
                    attempts++
                }
            }

            if (codeExists) {
                return Result.failure(CodeGenerationFailedException())
            }

            val familyData = hashMapOf(
                "name" to familyName,
                "joinCode" to joinCode,
                "createdByUserId" to userId,
                "isAlarmEnabled" to true
            )
            val docRef = db.collection("families").add(familyData).await()
            Result.success(Pair(docRef.id, joinCode))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // K-1 + H-1: Join-Flow über gesicherte Cloud Function statt direktem Firestore-Query.
    // Die families-Collection ist damit nicht mehr global lesbar.
    // Die Cloud Function validiert den Code serverseitig und erzwingt Rate-Limiting.
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

    // Bug-Fix: isAlarmEnabled ist eine rein gerätespezifische Einstellung und wird
    // NICHT mehr in Firestore gespeichert oder von dort gelesen. Die folgenden
    // Funktionen sind deprecated und werden nicht mehr aufgerufen.
    @Deprecated("isAlarmEnabled ist jetzt ausschließlich lokal in PreferencesRepository gespeichert.")
    fun getFamilyAlarmEnabledFlow(familyId: String): Flow<Boolean> = callbackFlow {
        awaitClose { }
    }

    @Deprecated("isAlarmEnabled ist jetzt ausschließlich lokal in PreferencesRepository gespeichert.")
    suspend fun updateFamilyAlarmEnabled(familyId: String, enabled: Boolean) {
        // Keine Aktion mehr – Firestore-Sync für dieses Feld wurde bewusst entfernt
        android.util.Log.d("FirebaseRepository", "updateFamilyAlarmEnabled wird ignoriert (nur noch lokal)")
    }

    suspend fun getFamilyName(familyId: String): String? {
        return try {
            val doc = db.collection("families").document(familyId).get().await()
            doc.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    fun getFamilyMembersFlow(familyId: String): Flow<List<FamilyMember>> = callbackFlow {
        val collection = db.collection("families").document(familyId).collection("members")
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseRepository", "Fehler in getFamilyMembersFlow für $familyId: ${error.message}")
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    android.util.Log.d("FirebaseRepository", "Subkollektion 'members' ist leer für $familyId")
                }
                val members = snapshot.documents.mapNotNull { doc ->
                    try {
                        val name = doc.getString("name") ?: "Unbekannt"
                        val earliestStr = doc.getString("earliestWakeUp") ?: "06:00"
                        val latestStr = doc.getString("latestWakeUp") ?: "07:30"
                        val leaveStr = doc.getString("leaveHomeTime")
                        
                        FamilyMember(
                            id = doc.id,
                            name = name,
                            earliestWakeUp = try { LocalTime.parse(earliestStr) } catch (e: Exception) { LocalTime.of(6, 0) },
                            latestWakeUp = try { LocalTime.parse(latestStr) } catch (e: Exception) { LocalTime.of(7, 30) },
                            bathroomDurationMinutes = doc.getLong("bathroomDurationMinutes") ?: 20L,
                            wantsBreakfast = doc.getBoolean("wantsBreakfast") ?: true,
                            leaveHomeTime = leaveStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } },
                            isPaused = doc.getBoolean("isPaused") ?: false,
                            isAwakeToday = doc.getBoolean("isAwakeToday") ?: false,
                            lastResetDate = doc.getString("lastResetDate") ?: "",
                            claimedByUserId = doc.getString("claimedByUserId"),
                            claimedByUserName = doc.getString("claimedByUserName"),
                            sequenceOrder = doc.getLong("sequenceOrder")?.toInt() ?: 0,
                            createdAt = doc.getLong("createdAt"),
                            lastUpdatedAt = doc.getLong("lastUpdatedAt"),
                            deviceAlarmEnabled = doc.getBoolean("deviceAlarmEnabled")
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepository", "Kritischer Fehler beim Mapping von ${doc.id}: ${e.message}")
                        null
                    }
                }
                // Stabilitäts-Fix: Sortierung nach sequenceOrder (manuell) und dann createdAt (stabil)
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
                "lastUpdatedAt" to currentTime,
                "deviceAlarmEnabled" to member.deviceAlarmEnabled
            )
            docRef.set(data).await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Fehler beim Speichern von Member ${member.id}: ${e.message}")
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

    // K-2 Security: joinCode wird NICHT im User-Profil gespeichert.
    // Er wird bei Bedarf direkt aus dem Family-Dokument gelesen.
    suspend fun saveUserFamily(userId: String, familyId: String): Result<Unit> {
        return try {
            val data = hashMapOf("familyId" to familyId)
            db.collection("users").document(userId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Fehler beim Speichern der User-Family-Zuordnung für $userId: ${e.message}")
            Result.failure(e)
        }
    }

    // Bug-Fix: isAlarmEnabled aus Firestore entfernt. Triple -> Pair (familyId, joinCode).
    // isAlarmEnabled ist rein gerätespezifisch und wird nicht mehr von Firestore geladen.
    suspend fun getUserFamily(userId: String): Result<Pair<String, String>?> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val familyId = doc.getString("familyId")
                if (familyId != null) {
                    // K-2: joinCode wird aus dem Family-Dokument gelesen, nicht aus dem User-Profil
                    val familyDoc = db.collection("families").document(familyId).get().await()
                    val joinCode = familyDoc.getString("joinCode")
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
            android.util.Log.e("FirebaseRepository", "Fehler beim Entfernen der User-Family-Zuordnung für $userId: ${e.message}")
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

    suspend fun getClaimedMember(familyId: String, userId: String): FamilyMember? {
        return try {
            val snapshot = db.collection("families").document(familyId)
                .collection("members")
                .whereEqualTo("claimedByUserId", userId)
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                FamilyMember(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    earliestWakeUp = try { LocalTime.parse(doc.getString("earliestWakeUp") ?: "06:00") } catch (e: Exception) { LocalTime.of(6, 0) },
                    latestWakeUp = try { LocalTime.parse(doc.getString("latestWakeUp") ?: "07:30") } catch (e: Exception) { LocalTime.of(7, 30) },
                    bathroomDurationMinutes = doc.getLong("bathroomDurationMinutes") ?: 20L,
                    wantsBreakfast = doc.getBoolean("wantsBreakfast") ?: true,
                    leaveHomeTime = doc.getString("leaveHomeTime")?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } },
                    isPaused = doc.getBoolean("isPaused") ?: false,
                    isAwakeToday = doc.getBoolean("isAwakeToday") ?: false,
                    lastResetDate = doc.getString("lastResetDate") ?: "",
                    claimedByUserId = doc.getString("claimedByUserId"),
                    claimedByUserName = doc.getString("claimedByUserName"),
                    sequenceOrder = doc.getLong("sequenceOrder")?.toInt() ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFamily(familyId: String): Result<Unit> {
        return try {
            val familyRef = db.collection("families").document(familyId)

            // 1. Alle Members als Batch löschen (atomar, robuster bei Netzwerkabbruch)
            val membersCollection = familyRef.collection("members")
            val membersSnapshot = membersCollection.get().await()

            if (membersSnapshot.documents.isNotEmpty()) {
                val batch = db.batch()
                membersSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }

            // 2. Familie-Dokument selbst löschen
            // HINWEIS: Falls dieser Schritt fehlschlägt, existiert eine Zombie-Familie (Members = 0, Dokument noch da).
            // Wird durch Cloud Function (Garbage Collection nach 180 Tagen) bereinigt.
            try {
                familyRef.delete().await()
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRepository", "KRITISCH: Members gelöscht, aber Familie-Dokument $familyId konnte nicht entfernt werden: ${e.message}")
                return Result.failure(e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualisiert die Reihenfolge mehrerer Mitglieder atomar in einem Batch.
     * Verhindert Inkonsistenzen bei gleichzeitigem Schieben.
     */
    suspend fun updateMemberOrders(familyId: String, orders: Map<String, Int>) {
        try {
            val batch = db.batch()
            val collection = db.collection("families").document(familyId).collection("members")
            
            orders.forEach { (memberId, order) ->
                val docRef = collection.document(memberId)
                batch.update(docRef, "sequenceOrder", order, "lastUpdatedAt", System.currentTimeMillis())
            }
            
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Fehler beim Batch-Update der Reihenfolge: ${e.message}")
        }
    }

    /**
     * Schreibt nur das Feld 'deviceAlarmEnabled' für das eigene Mitglieds-Dokument.
     * Wird aufgerufen wenn der User seinen lokalen Alarm-Switch ändert, damit andere
     * Geräte den Status in der Mitgliederliste anzeigen können.
     */
    suspend fun updateDeviceAlarmEnabled(familyId: String, memberId: String, enabled: Boolean) {
        try {
            db.collection("families").document(familyId)
                .collection("members").document(memberId)
                .update("deviceAlarmEnabled", enabled)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Fehler beim Schreiben von deviceAlarmEnabled für $memberId: ${e.message}")
        }
    }

    /**
     * Erzeugt einen Flow, der den Synchronisationsstatus der Mitglieder-Kollektion überwacht.
     * Nutzt Firestore Metadaten (isFromCache, hasPendingWrites).
     */
    fun getSyncStatusFlow(familyId: String): Flow<de.familienwecker.famwake.model.SyncStatus> = callbackFlow {
        val collection = db.collection("families").document(familyId).collection("members")
        val subscription = collection.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(
                    de.familienwecker.famwake.model.SyncStatus(
                        isFromCache = snapshot.metadata.isFromCache(),
                        hasPendingWrites = snapshot.metadata.hasPendingWrites()
                    )
                )
            }
        }
        awaitClose { subscription.remove() }
    }
}
