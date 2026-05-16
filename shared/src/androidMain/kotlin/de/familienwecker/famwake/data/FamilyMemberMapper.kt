package de.familienwecker.famwake.data

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.android
import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalTime
import com.google.firebase.Timestamp // Added import for native Firebase Timestamp

/**
 * Mapper: Firestore DocumentSnapshot → FamilyMember (GitLive API)
 * Nutzt android.get() für alle Felder die Serialization-Probleme verursachen könnten.
 */
@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toFamilyMember(): FamilyMember {
    val rawProfiles = android.get("dayProfiles") as? Map<*, *>
    val dayProfiles = rawProfiles?.mapNotNull { (key, value) ->
        val dayNum = key.toString().toIntOrNull() ?: return@mapNotNull null
        val map = value as? Map<*, *> ?: return@mapNotNull null
        val leaveStr2 = map["leaveHomeTime"] as? String
        val earliestRaw = map["earliestWakeUp"] as? String
        val latestRaw = map["latestWakeUp"] as? String
        val bathroomRaw = map["bathroomDurationMinutes"]
        val bufferRaw = map["bufferMinutes"]
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
            },
            // Nullable: null = globaler Puffer, sonst individueller Override
            bufferMinutes = when (bufferRaw) {
                is Long -> bufferRaw
                is Number -> bufferRaw.toLong()
                else -> null
            }
        )
    }?.toMap()?.takeIf { it.isNotEmpty() }

    // android.get() umgeht Serialization-Probleme mit Any? und Timestamp
    val createdAtRaw = android.get("createdAt")
    val createdAt: Long? = when (createdAtRaw) {
        is Timestamp -> createdAtRaw.seconds * 1000L
        is Number -> createdAtRaw.toLong()
        else -> null
    }

    val lastUpdatedRaw = android.get("lastUpdatedAt")
    val lastUpdatedAt: Long? = when (lastUpdatedRaw) {
        is Timestamp -> lastUpdatedRaw.seconds * 1000L
        is Number -> lastUpdatedRaw.toLong()
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
        claimedByDeviceId = get("claimedByDeviceId"),
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
                "leaveHomeTime" to profile.leaveHomeTime?.toString(),
                "bufferMinutes" to profile.bufferMinutes
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
        "claimedByDeviceId" to claimedByDeviceId,
        "sequenceOrder" to sequenceOrder,
        "createdAt" to (createdAt ?: System.currentTimeMillis()),
        "lastUpdatedAt" to dev.gitlive.firebase.firestore.FieldValue.serverTimestamp,
        "deviceAlarmEnabled" to deviceAlarmEnabled,
        "dayProfiles" to dayProfilesData
    )
}
