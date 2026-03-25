package de.familienwecker.famwake.data

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Timestamp
import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalTime

/**
 * Mapper: Firestore DocumentSnapshot → FamilyMember (GitLive API)
 */
@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toFamilyMember(): FamilyMember {
    val rawProfiles = get<Map<*, *>?>("dayProfiles")
    val dayProfiles = rawProfiles?.mapNotNull { (key, value) ->
        val dayNum = key.toString().toIntOrNull() ?: return@mapNotNull null
        val map = value as? Map<*, *> ?: return@mapNotNull null
        val leaveStr2 = map["leaveHomeTime"] as? String
        val earliestRaw = map["earliestWakeUp"] as? String
        val latestRaw = map["latestWakeUp"] as? String
        val bathroomRaw = map["bathroomDurationMinutes"]
        dayNum to DayProfile(
            isActive = map["isActive"] as? Boolean ?: true,
            earliestWakeUp = earliestRaw?.let {
                try { LocalTime.parse(it) } catch (e: Exception) { LocalTime(6, 0) }
            } ?: LocalTime(6, 0),
            latestWakeUp = latestRaw?.let {
                try { LocalTime.parse(it) } catch (e: Exception) { LocalTime(7, 30) }
            } ?: LocalTime(7, 30),
            bathroomDurationMinutes = when (bathroomRaw) {
                is Long -> bathroomRaw
                is Number -> bathroomRaw.toLong()
                else -> 20L
            },
            wantsBreakfast = map["wantsBreakfast"] as? Boolean ?: true,
            leaveHomeTime = leaveStr2?.let {
                try { LocalTime.parse(it) } catch (e: Exception) { null }
            }
        )
    }?.toMap()?.takeIf { it.isNotEmpty() }

    val createdAtVal = get<Any?>("createdAt")
    val createdAt = when (createdAtVal) {
        is Number -> createdAtVal.toLong()
        is Timestamp -> createdAtVal.seconds * 1000L
        else -> null
    }

    val lastUpdatedVal = get<Any?>("lastUpdatedAt")
    val lastUpdatedAt = when (lastUpdatedVal) {
        is Number -> lastUpdatedVal.toLong()
        is Timestamp -> lastUpdatedVal.seconds * 1000L
        else -> null
    }

    return FamilyMember(
        id = id,
        name = get("name") ?: "Unknown",
        latestWakeUp = (get<String?>("latestWakeUp"))?.let {
            try { LocalTime.parse(it) } catch (e: Exception) { LocalTime(7, 0) }
        } ?: LocalTime(7, 0),
        earliestWakeUp = (get<String?>("earliestWakeUp"))?.let {
            try { LocalTime.parse(it) } catch (e: Exception) { LocalTime(6, 0) }
        } ?: LocalTime(6, 0),
        bathroomDurationMinutes = get<Long?>("bathroomDurationMinutes") ?: 20L,
        wantsBreakfast = get<Boolean?>("wantsBreakfast") ?: true,
        leaveHomeTime = (get<String?>("leaveHomeTime"))?.let {
            try { LocalTime.parse(it) } catch (e: Exception) { null }
        },
        isPaused = get<Boolean?>("isPaused") ?: false,
        isAwakeToday = get<Boolean?>("isAwakeToday") ?: false,
        lastResetDate = get("lastResetDate") ?: "",
        claimedByUserId = get("claimedByUserId"),
        claimedByUserName = get("claimedByUserName"),
        sequenceOrder = get<Long?>("sequenceOrder")?.toInt() ?: 0,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt,
        deviceAlarmEnabled = get("deviceAlarmEnabled"),
        dayProfiles = dayProfiles
    )
}

/**
 * Mapper: FamilyMember → Firestore Map (GitLive API)
 */
fun FamilyMember.toFirestoreMap(): Map<String, Any?> {
    val dayProfilesData = dayProfiles?.mapKeys { it.key.toString() }
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

    return hashMapOf(
        "name" to name,
        "earliestWakeUp" to earliestWakeUp.toString(),
        "latestWakeUp" to latestWakeUp.toString(),
        "bathroomDurationMinutes" to bathroomDurationMinutes,
        "wantsBreakfast" to wantsBreakfast,
        "leaveHomeTime" to leaveHomeTime?.toString(),
        "isPaused" to isPaused,
        "isAwakeToday" to isAwakeToday,
        "lastResetDate" to lastResetDate,
        "claimedByUserId" to claimedByUserId,
        "claimedByUserName" to claimedByUserName,
        "sequenceOrder" to sequenceOrder,
        "createdAt" to (createdAt ?: System.currentTimeMillis()),
        "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp,
        "deviceAlarmEnabled" to deviceAlarmEnabled,
        "dayProfiles" to dayProfilesData
    )
}
