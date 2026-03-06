package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.FamilyMember
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import kotlin.random.Random

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
                return Result.failure(Exception("Konnte keinen eindeutigen Code generieren. Bitte erneut versuchen."))
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

    suspend fun joinFamilyByCode(joinCode: String): Result<Pair<String, String>> {
        return try {
            val snapshot = db.collection("families").whereEqualTo("joinCode", joinCode).limit(1).get().await()
            if (!snapshot.isEmpty) {
                Result.success(Pair(snapshot.documents.first().id, joinCode))
            } else {
                Result.failure(Exception("Unter diesem Code wurde keine Familie gefunden."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFamilyAlarmEnabledFlow(familyId: String): Flow<Boolean> = callbackFlow {
        val docRef = db.collection("families").document(familyId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.getBoolean("isAlarmEnabled") ?: true)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun updateFamilyAlarmEnabled(familyId: String, enabled: Boolean) {
        try {
            db.collection("families").document(familyId).update("isAlarmEnabled", enabled).await()
        } catch (e: Exception) {
            e.printStackTrace()
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
                            createdAt = doc.getLong("createdAt"),
                            lastUpdatedAt = doc.getLong("lastUpdatedAt")
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepository", "Kritischer Fehler beim Mapping von ${doc.id}: ${e.message}")
                        null
                    }
                }
                // Stabilitäts-Fix: Sortierung vereinfachen
                val sortedMembers = members.sortedBy { it.createdAt ?: 0L }
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
                "createdAt" to existingCreatedAt,
                "lastUpdatedAt" to currentTime
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
            val snapshot = docRef.get().await()
            val existingClaim = snapshot.getString("claimedByUserId")
            
            if (existingClaim == userId) {
                docRef.update(
                    mapOf(
                        "claimedByUserId" to null,
                        "claimedByUserName" to null
                    )
                ).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun generateJoinCode(): String {
        // Base32 ohne verwechselbare Zeichen (0, O, 1, I)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    suspend fun saveUserFamily(userId: String, familyId: String, joinCode: String) {
        try {
            val data = hashMapOf(
                "familyId" to familyId,
                "joinCode" to joinCode
            )
            db.collection("users").document(userId).set(data).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserFamily(userId: String): Result<Triple<String, String, Boolean>?> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val familyId = doc.getString("familyId")
                val joinCode = doc.getString("joinCode")
                if (familyId != null && joinCode != null) {
                    // Also get the global alarm state from the family doc
                    val familyDoc = db.collection("families").document(familyId).get().await()
                    val isAlarmEnabled = familyDoc.getBoolean("isAlarmEnabled") ?: true
                    Result.success(Triple(familyId, joinCode, isAlarmEnabled))
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
            e.printStackTrace()
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
                    claimedByUserName = doc.getString("claimedByUserName")
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
            
            // 1. Delete all members in the subcollection individually to be more robust
            val membersCollection = familyRef.collection("members")
            val membersSnapshot = membersCollection.get().await()
            
            for (doc in membersSnapshot.documents) {
                try {
                    doc.reference.delete().await()
                } catch (e: Exception) {
                    // Log and continue - we want to delete as much as possible
                    e.printStackTrace()
                }
            }

            // 2. Delete the family document itself
            familyRef.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // If the family doc deletion itself fails (permissions), we at least tried the members
            Result.failure(e)
        }
    }
}
