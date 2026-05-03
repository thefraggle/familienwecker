package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.FamilyData
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.SyncStatus
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Platform-unabhängiges Interface für alle Firebase-Operationen.
 * Implementierung in androidMain via GitLive Firebase KMP.
 */
interface IFirebaseRepository {

    /** Auth-State-Flow – emittiert den aktuellen User (oder null nach Logout). */
    fun getAuthStateFlow(): Flow<FirebaseUser?>

    // ── Familie ──────────────────────────────────────────────────────────────


    suspend fun createFamily(familyName: String, userId: String): Result<Pair<String, String>>

    suspend fun joinFamilyByCode(joinCode: String): Result<Pair<String, String>>

    suspend fun getFamilyName(familyId: String): String?

    suspend fun getFamilyData(familyId: String): FamilyData?

    /** Familienkontext des Users via Cloud Function (1 Call statt 3 Firestore-Reads). */
    suspend fun getUserContext(uid: String): Result<Pair<String, String>?>

    suspend fun getUserFamily(uid: String, cachedJoinCode: String? = null): Result<Pair<String, String>?>


    suspend fun checkFamilyExists(familyId: String): Boolean

    suspend fun removeUserFamily(userId: String, familyId: String): Result<Unit>

    suspend fun deleteFamily(familyId: String, userId: String): Result<Unit>

    suspend fun leaveFamilyBatch(userId: String, familyId: String, memberId: String): Result<Unit>

    // ── Mitglieder ────────────────────────────────────────────────────────────

    fun getFamilyMembersFlow(familyId: String): Flow<List<FamilyMember>>

    suspend fun addOrUpdateMember(familyId: String, member: FamilyMember)

    suspend fun removeMember(familyId: String, id: String): Result<Unit>

    suspend fun claimMember(familyId: String, memberId: String, userId: String, userName: String?, deviceId: String): Boolean

    /** Offline-Variante ohne Transaction: schreibt claimedByUserId direkt damit Firestore es queuen kann. */
    suspend fun claimMemberOffline(familyId: String, memberId: String, userId: String, userName: String?, deviceId: String)

    suspend fun unclaimMember(familyId: String, memberId: String, userId: String, deviceId: String): Boolean

    suspend fun getClaimedMember(familyId: String, userId: String): FamilyMember?

    suspend fun updateMemberOrders(familyId: String, orders: Map<String, Int>)

    /** Speichert wer zuletzt einen Reorder für eine Familie ausgelöst hat (für Self-Push-Filter). */
    suspend fun setReorderMeta(uid: String, familyId: String)

    suspend fun updateMembersBatch(familyId: String, members: List<FamilyMember>)

    suspend fun updateDeviceAlarmEnabled(familyId: String, memberId: String, enabled: Boolean)

    /** Schreibt nur isPaused + lastUpdatedAt – umgeht das Security-Rule-Problem mit vollem .set(). */
    suspend fun updateMemberPauseState(familyId: String, memberId: String, isPaused: Boolean)

    // ── Admin / Status ────────────────────────────────────────────────────────

    fun checkIsGlobalAdminFlow(uid: String): Flow<Boolean>

    fun getSyncStatusFlow(familyId: String): Flow<SyncStatus>

    suspend fun requestAdminStatsReport(): Result<Unit>

    // ── Feedback ─────────────────────────────────────────────────────────────

    suspend fun sendFeedback(
        category: String,
        message: String,
        email: String,
        appVersion: String,
        device: String
    ): Result<Unit>
}
