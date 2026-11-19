package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyData
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.SyncStatus
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime

/**
 * In-Memory-Adapter für [IFirebaseRepository] in Tests (zweiter Adapter am Firebase-Seam).
 */
class InMemoryFirebaseRepository : IFirebaseRepository {

    private val authState = MutableStateFlow<FirebaseUser?>(null)
    private val families = MutableStateFlow<Map<String, FamilyData>>(emptyMap())
    private val membersByFamily = MutableStateFlow<Map<String, List<FamilyMember>>>(emptyMap())
    private val userFamilies = mutableMapOf<String, Pair<String, String>>() // uid -> (familyId, joinCode)
    private val joinCodes = mutableMapOf<String, String>() // joinCode -> familyId
    private val adminUids = MutableStateFlow<Set<String>>(emptySet())
    private val feedbackLog = mutableListOf<Map<String, String>>()
    private var nextId = 1

    override fun getAuthStateFlow(): Flow<FirebaseUser?> = authState

    override suspend fun createFamily(familyName: String, userId: String): Result<Pair<String, String>> {
        val familyId = "fam_${nextId++}"
        val joinCode = "CODE${10 + nextId}".take(6)
        families.value = families.value + (familyId to FamilyData(id = familyId, name = familyName, createdByUserId = userId))
        membersByFamily.value = membersByFamily.value + (familyId to emptyList())
        userFamilies[userId] = familyId to joinCode
        joinCodes[joinCode] = familyId
        return Result.success(familyId to joinCode)
    }

    override suspend fun joinFamilyByCode(joinCode: String): Result<Pair<String, String>> {
        val familyId = joinCodes[joinCode]
            ?: return Result.failure(AppErrorException(AppError.FamilyNotFound))
        return Result.success(familyId to joinCode)
    }

    override suspend fun getFamilyName(familyId: String): String? = families.value[familyId]?.name

    override suspend fun getFamilyData(familyId: String): FamilyData? = families.value[familyId]

    override fun getFamilyDataFlow(familyId: String): Flow<FamilyData?> =
        families.map { it[familyId] }

    override suspend fun getUserContext(uid: String): Result<Pair<String, String>?> =
        Result.success(userFamilies[uid])

    override suspend fun getUserFamily(uid: String, cachedJoinCode: String?): Result<Pair<String, String>?> =
        Result.success(userFamilies[uid])

    override suspend fun checkFamilyExists(familyId: String): Boolean = families.value.containsKey(familyId)

    override suspend fun removeUserFamily(userId: String, familyId: String): Result<Unit> {
        if (userFamilies[userId]?.first == familyId) userFamilies.remove(userId)
        return Result.success(Unit)
    }

    override suspend fun deleteFamily(familyId: String, userId: String): Result<Unit> {
        families.value = families.value - familyId
        membersByFamily.value = membersByFamily.value - familyId
        userFamilies.entries.removeAll { it.value.first == familyId }
        return Result.success(Unit)
    }

    override suspend fun syncLocalFamilyToCloud(
        localFamilyId: String,
        familyName: String,
        members: List<FamilyMember>,
        userId: String
    ): Result<Pair<String, String>> {
        val created = createFamily(familyName, userId).getOrThrow()
        membersByFamily.value = membersByFamily.value + (created.first to members)
        return Result.success(created)
    }

    override suspend fun leaveFamilyBatch(userId: String, familyId: String, memberId: String?): Result<Unit> {
        userFamilies.remove(userId)
        if (memberId != null) {
            updateMember(familyId, memberId) { it.copy(claimedByUserId = null, claimedByUserName = null, claimedByDeviceId = null) }
        }
        return Result.success(Unit)
    }

    override fun getFamilyMembersFlow(familyId: String): Flow<List<FamilyMember>> =
        membersByFamily.map { it[familyId].orEmpty() }

    override suspend fun addOrUpdateMember(familyId: String, member: FamilyMember) {
        val current = membersByFamily.value[familyId].orEmpty()
        val updated = if (current.any { it.id == member.id }) {
            current.map { if (it.id == member.id) member else it }
        } else {
            current + member
        }
        membersByFamily.value = membersByFamily.value + (familyId to updated)
    }

    override suspend fun removeMember(familyId: String, id: String): Result<Unit> {
        val current = membersByFamily.value[familyId].orEmpty()
        membersByFamily.value = membersByFamily.value + (familyId to current.filterNot { it.id == id })
        return Result.success(Unit)
    }

    override suspend fun claimMember(
        familyId: String,
        memberId: String,
        userId: String,
        userName: String?,
        deviceId: String,
        force: Boolean
    ): Boolean {
        val current = membersByFamily.value[familyId].orEmpty()
        val target = current.find { it.id == memberId } ?: return false
        if (!force && target.claimedByUserId != null && target.claimedByUserId != userId) return false
        updateMember(familyId, memberId) {
            it.copy(claimedByUserId = userId, claimedByUserName = userName, claimedByDeviceId = deviceId)
        }
        return true
    }

    override suspend fun claimMemberOffline(
        familyId: String,
        memberId: String,
        userId: String,
        userName: String?,
        deviceId: String
    ) {
        claimMember(familyId, memberId, userId, userName, deviceId, force = true)
    }

    override suspend fun unclaimMember(familyId: String, memberId: String, userId: String, deviceId: String): Boolean {
        updateMember(familyId, memberId) {
            if (it.claimedByUserId == userId) it.copy(claimedByUserId = null, claimedByUserName = null, claimedByDeviceId = null) else it
        }
        return true
    }

    override suspend fun getClaimedMember(familyId: String, userId: String): FamilyMember? =
        membersByFamily.value[familyId].orEmpty().find { it.claimedByUserId == userId }

    override suspend fun updateMemberOrders(familyId: String, orders: Map<String, Int>) {
        val current = membersByFamily.value[familyId].orEmpty()
        membersByFamily.value = membersByFamily.value + (familyId to current.map { m ->
            orders[m.id]?.let { m.copy(sequenceOrder = it) } ?: m
        })
    }

    override suspend fun updateMemberDayProfiles(familyId: String, memberId: String, dayProfiles: Map<Int, DayProfile>?) {
        updateMember(familyId, memberId) { it.copy(dayProfiles = dayProfiles) }
    }

    override suspend fun updateMemberOrderAndProfiles(familyId: String, members: List<FamilyMember>) {
        membersByFamily.value = membersByFamily.value + (familyId to members)
    }

    override suspend fun setReorderMeta(uid: String, familyId: String) = Unit

    override suspend fun setUserActionMeta(uid: String, familyId: String) = Unit

    override suspend fun updateMembersBatch(familyId: String, members: List<FamilyMember>) {
        members.forEach { addOrUpdateMember(familyId, it) }
    }

    override suspend fun updateDeviceAlarmEnabled(familyId: String, memberId: String, enabled: Boolean) {
        updateMember(familyId, memberId) { it.copy(deviceAlarmEnabled = enabled) }
    }

    override suspend fun updateMemberPauseState(familyId: String, memberId: String, isPaused: Boolean) {
        updateMember(familyId, memberId) { it.copy(isPaused = isPaused) }
    }

    override suspend fun updateMemberSnoozeState(
        familyId: String,
        memberId: String,
        snoozeUntil: LocalDateTime?,
        snoozeCount: Int
    ) {
        updateMember(familyId, memberId) { it.copy(snoozeUntil = snoozeUntil, snoozeCount = snoozeCount) }
    }

    override suspend fun updateMemberAwakeToday(familyId: String, memberId: String, isAwakeToday: Boolean) {
        updateMember(familyId, memberId) { it.copy(isAwakeToday = isAwakeToday) }
    }

    override suspend fun updateGlobalBufferMinutes(familyId: String, minutes: Long) {
        val existing = families.value[familyId] ?: return
        families.value = families.value + (familyId to existing.copy(globalBufferMinutes = minutes))
    }

    override suspend fun updateVacationUntil(familyId: String, vacationUntil: String?) {
        val existing = families.value[familyId] ?: return
        families.value = families.value + (familyId to existing.copy(vacationUntil = vacationUntil))
    }

    override suspend fun notifyBathroomFree(familyId: String, memberId: String): Result<String?> = Result.success(null)

    override fun checkIsGlobalAdminFlow(uid: String): Flow<Boolean> = adminUids.map { uid in it }

    override fun getSyncStatusFlow(familyId: String): Flow<SyncStatus> = MutableStateFlow(SyncStatus())

    override suspend fun requestAdminStatsReport(): Result<Unit> = Result.success(Unit)

    override suspend fun sendFeedback(
        category: String,
        message: String,
        email: String,
        appVersion: String,
        device: String
    ): Result<Unit> {
        feedbackLog += mapOf("category" to category, "message" to message, "email" to email)
        return Result.success(Unit)
    }

    private fun updateMember(familyId: String, memberId: String, transform: (FamilyMember) -> FamilyMember) {
        val current = membersByFamily.value[familyId].orEmpty()
        membersByFamily.value = membersByFamily.value + (familyId to current.map {
            if (it.id == memberId) transform(it) else it
        })
    }
}
