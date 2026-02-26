package com.example.familienwecker.data

import com.example.familienwecker.model.FamilyMember
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
            val joinCode = generateJoinCode()
            val familyData = hashMapOf(
                "name" to familyName,
                "joinCode" to joinCode,
                "createdByUserId" to userId
            )
            val docRef = db.collection("families").add(familyData).await()
            // Gib die Document ID und den Join Code zurück
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
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val members = snapshot.documents.mapNotNull { doc ->
                    try {
                        FamilyMember(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            earliestWakeUp = LocalTime.parse(doc.getString("earliestWakeUp") ?: "06:00"),
                            latestWakeUp = LocalTime.parse(doc.getString("latestWakeUp") ?: "07:30"),
                            bathroomDurationMinutes = doc.getLong("bathroomDurationMinutes") ?: 20L,
                            wantsBreakfast = doc.getBoolean("wantsBreakfast") ?: true,
                            leaveHomeTime = doc.getString("leaveHomeTime")?.let { LocalTime.parse(it) },
                            isPaused = doc.getBoolean("isPaused") ?: false,
                            claimedByUserId = doc.getString("claimedByUserId"),
                            claimedByUserName = doc.getString("claimedByUserName")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(members)
            }
        }

        awaitClose { subscription.remove() }
    }

    fun addOrUpdateMember(familyId: String, member: FamilyMember) {
        val data = hashMapOf(
            "name" to member.name,
            "earliestWakeUp" to member.earliestWakeUp.toString(),
            "latestWakeUp" to member.latestWakeUp.toString(),
            "bathroomDurationMinutes" to member.bathroomDurationMinutes,
            "wantsBreakfast" to member.wantsBreakfast,
            "leaveHomeTime" to member.leaveHomeTime?.toString(),
            "isPaused" to member.isPaused,
            "claimedByUserId" to member.claimedByUserId,
            "claimedByUserName" to member.claimedByUserName
        )
        db.collection("families").document(familyId).collection("members").document(member.id).set(data)
    }

    fun removeMember(familyId: String, id: String) {
        db.collection("families").document(familyId).collection("members").document(id).delete()
    }

    suspend fun claimMember(familyId: String, memberId: String, userId: String, userName: String?): Boolean {
        return try {
            val docRef = db.collection("families").document(familyId).collection("members").document(memberId)
            val snapshot = docRef.get().await()
            val existingClaim = snapshot.getString("claimedByUserId")
            
            if (existingClaim == null || existingClaim == userId) {
                docRef.update(
                    mapOf(
                        "claimedByUserId" to userId,
                        "claimedByUserName" to userName
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
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
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

    suspend fun getUserFamily(userId: String): Result<Pair<String, String>?> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val familyId = doc.getString("familyId")
                val joinCode = doc.getString("joinCode")
                if (familyId != null && joinCode != null) {
                    Result.success(Pair(familyId, joinCode))
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
                    earliestWakeUp = LocalTime.parse(doc.getString("earliestWakeUp") ?: "06:00"),
                    latestWakeUp = LocalTime.parse(doc.getString("latestWakeUp") ?: "07:30"),
                    bathroomDurationMinutes = doc.getLong("bathroomDurationMinutes") ?: 20L,
                    wantsBreakfast = doc.getBoolean("wantsBreakfast") ?: true,
                    leaveHomeTime = doc.getString("leaveHomeTime")?.let { LocalTime.parse(it) },
                    isPaused = doc.getBoolean("isPaused") ?: false,
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
            
            // 1. Delete all members in the subcollection
            val membersCollection = familyRef.collection("members")
            val membersSnapshot = membersCollection.get().await()
            
            // We use a batch for better performance and atomicity where possible
            var batch = db.batch()
            var count = 0
            
            for (doc in membersSnapshot.documents) {
                batch.delete(doc.reference)
                count++
                // Firestore batches have a limit of 500 operations
                if (count >= 500) {
                    batch.commit().await()
                    batch = db.batch()
                    count = 0
                }
            }
            if (count > 0) {
                batch.commit().await()
            }

            // 2. Delete the family document itself
            familyRef.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
