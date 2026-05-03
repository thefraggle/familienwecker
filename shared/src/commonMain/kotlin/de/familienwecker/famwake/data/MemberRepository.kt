package de.familienwecker.famwake.data

import de.familienwecker.famwake.db.MemberDao
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.DayProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private val json = Json { ignoreUnknownKeys = true }

class MemberRepository(private val memberDao: MemberDao) {
    
    val members: Flow<List<FamilyMember>> = memberDao.getAllMembers().map { entities ->
        entities.map { it.toDomain() }
    }.distinctUntilChanged { old, new ->
        old.size == new.size &&
        old.zip(new).all { (a, b) -> a.id == b.id && a.lastUpdatedAt == b.lastUpdatedAt }
    }

    suspend fun cacheMembers(members: List<FamilyMember>) {
        if (members.isEmpty()) return // Schutz: 0-Docs-Snapshot nicht als "alle gelöscht" interpretieren
        val entities = members.map { it.toEntity() }
        memberDao.upsertMembers(entities)                           // 1. zuerst einfügen/updaten
        memberDao.deleteNotInIds(entities.map { it.id })           // 2. dann stale löschen → kein 0-State
    }

    suspend fun upsertMember(member: FamilyMember) {
        memberDao.upsertMembers(listOf(member.toEntity()))
    }

    suspend fun deleteMember(id: String) {
        memberDao.deleteMember(id)
    }

    suspend fun clearCache() {
        memberDao.clearAll()
    }
}

// Mapper-Extensions
private fun de.familienwecker.famwake.db.FamilyMemberEntity.toDomain(): FamilyMember = FamilyMember(
    id = id,
    name = name,
    earliestWakeUp = kotlinx.datetime.LocalTime.parse(earliestWakeUp),
    latestWakeUp = kotlinx.datetime.LocalTime.parse(latestWakeUp),
    bathroomDurationMinutes = bathroomDurationMinutes,
    wantsBreakfast = wantsBreakfast,
    leaveHomeTime = leaveHomeTime?.let { kotlinx.datetime.LocalTime.parse(it) },
    isPaused = isPaused,
    isAwakeToday = isAwakeToday,
    lastResetDate = lastResetDate,
    claimedByUserId = claimedByUserId,
    claimedByUserName = claimedByUserName,
    claimedByDeviceId = claimedByDeviceId,
    sequenceOrder = sequenceOrder,
    createdAt = createdAt,
    lastUpdatedAt = lastUpdatedAt,
    deviceAlarmEnabled = deviceAlarmEnabled,
    dayProfiles = dayProfilesJson?.let { json.decodeFromString(it) }
)

private fun FamilyMember.toEntity(): de.familienwecker.famwake.db.FamilyMemberEntity = de.familienwecker.famwake.db.FamilyMemberEntity(
    id = id,
    name = name,
    earliestWakeUp = earliestWakeUp.toString(),
    latestWakeUp = latestWakeUp.toString(),
    bathroomDurationMinutes = bathroomDurationMinutes,
    wantsBreakfast = wantsBreakfast,
    leaveHomeTime = leaveHomeTime?.toString(),
    isPaused = isPaused,
    isAwakeToday = isAwakeToday,
    lastResetDate = lastResetDate,
    claimedByUserId = claimedByUserId,
    claimedByUserName = claimedByUserName,
    claimedByDeviceId = claimedByDeviceId,
    sequenceOrder = sequenceOrder,
    createdAt = createdAt,
    lastUpdatedAt = lastUpdatedAt,
    deviceAlarmEnabled = deviceAlarmEnabled,
    dayProfilesJson = dayProfiles?.let { json.encodeToString(it) }
)
