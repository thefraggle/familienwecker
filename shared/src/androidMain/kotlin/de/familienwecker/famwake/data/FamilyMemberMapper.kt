package de.familienwecker.famwake.data

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.android
import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
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
            },
            breakfastDurationMinutes = when (val bRaw = map["breakfastDurationMinutes"]) {
                is Long -> bRaw
                is Number -> bRaw.toLong()
                else -> null
            },
            isSimpleMode = map["isSimpleMode"] as? Boolean ?: false,
            sequenceOrder = when (val seqRaw = map["sequenceOrder"]) {
                is Long -> seqRaw.toInt()
                is Number -> seqRaw.toInt()
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

    // Snooze-State: als Firestore Timestamp gespeichert
    val snoozeUntilRaw = android.get("snoozeUntil")
    val snoozeUntil: kotlinx.datetime.LocalDateTime? = when (snoozeUntilRaw) {
        is Timestamp -> {
            val millis = snoozeUntilRaw.seconds * 1000L + snoozeUntilRaw.nanoseconds / 1_000_000L
            kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        }
        else -> null
    }
    val snoozeCount: Int = when (val raw = android.get("snoozeCount")) {
        is Long -> raw.toInt()
        is Number -> raw.toInt()
        else -> 0
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
        breakfastDurationMinutes = when (val bRaw = get<Any?>("breakfastDurationMinutes")) {
            is Long -> bRaw
            is Number -> bRaw.toLong()
            else -> null
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
        dayProfiles = dayProfiles,
        isSimpleMode = get<Boolean?>("isSimpleMode") ?: false,
        snoozeUntil = snoozeUntil,
        snoozeCount = snoozeCount
    )
}

/**
 * Mapper: FamilyMember → Firestore Map (GitLive API)
 */
fun FamilyMember.toFirestoreMap(): Map<String, Any?> {
    val dayProfilesData = dayProfiles?.mapKeys { it.key.toString() }
        ?.mapValues { (_, profile) ->
            buildMap<String, Any?> {
                put("isActive", profile.isActive)
                put("earliestWakeUp", profile.earliestWakeUp.toString())
                put("latestWakeUp", profile.latestWakeUp.toString())
                put("bathroomDurationMinutes", profile.bathroomDurationMinutes)
                put("wantsBreakfast", profile.wantsBreakfast)
                profile.leaveHomeTime?.let { put("leaveHomeTime", it.toString()) }
                profile.bufferMinutes?.let { put("bufferMinutes", it) }
                profile.breakfastDurationMinutes?.let { put("breakfastDurationMinutes", it) }
                put("isSimpleMode", profile.isSimpleMode)
                profile.sequenceOrder?.let { put("sequenceOrder", it) }
            }
        }

    return hashMapOf(
        "name" to name,
        "earliestWakeUp" to earliestWakeUp.toString(),
        "latestWakeUp" to latestWakeUp.toString(),
        "bathroomDurationMinutes" to bathroomDurationMinutes,
        "wantsBreakfast" to wantsBreakfast,
        "leaveHomeTime" to leaveHomeTime?.toString(),
        "breakfastDurationMinutes" to breakfastDurationMinutes,
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
        "dayProfiles" to dayProfilesData,
        "isSimpleMode" to isSimpleMode,
        "snoozeUntil" to snoozeUntil?.let {
            val instant = it.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
            Timestamp(instant.epochSeconds, instant.nanosecondsOfSecond)
        },
        "snoozeCount" to snoozeCount
    )
}
