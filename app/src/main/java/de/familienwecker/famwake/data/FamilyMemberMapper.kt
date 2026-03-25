package de.familienwecker.famwake.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalTime

/**
 * M-1: Extrahiert das Duplikat-Mapping von Firestore-Dokument zu FamilyMember.
 */
@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toFamilyMember(): FamilyMember {
    val earliestStr = getString("earliestWakeUp") ?: "06:00"
    val latestStr = getString("latestWakeUp") ?: "07:30"
    val leaveStr = getString("leaveHomeTime")

    // dayProfiles: Map<String, Map<*,*>> in Firestore → Map<Int, DayProfile>
    val rawProfiles = get("dayProfiles") as? Map<*, *>
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

    val createdAtVal = get("createdAt")
    val createdAt = when (createdAtVal) {
        is Number -> createdAtVal.toLong()
        is com.google.firebase.Timestamp -> createdAtVal.toDate().time
        else -> null
    }

    val lastUpdatedVal = get("lastUpdatedAt")
    val lastUpdatedAt = when (lastUpdatedVal) {
        is Number -> lastUpdatedVal.toLong()
        is com.google.firebase.Timestamp -> lastUpdatedVal.toDate().time
        else -> null
    }

    return FamilyMember(
        id = id,
        name = getString("name") ?: "Unknown",
        latestWakeUp = getString("latestWakeUp")?.let { try { LocalTime.parse(it) } catch (e: Exception) { LocalTime(7, 0) } } ?: LocalTime(7, 0),
        earliestWakeUp = getString("earliestWakeUp")?.let { try { LocalTime.parse(it) } catch (e: Exception) { LocalTime(6, 0) } } ?: LocalTime(6, 0),
        bathroomDurationMinutes = getLong("bathroomDurationMinutes") ?: 20L,
        wantsBreakfast = getBoolean("wantsBreakfast") ?: true,
        leaveHomeTime = getString("leaveHomeTime")?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } },
        isPaused = getBoolean("isPaused") ?: false,
        isAwakeToday = getBoolean("isAwakeToday") ?: false,
        lastResetDate = getString("lastResetDate") ?: "",
        claimedByUserId = getString("claimedByUserId"),
        claimedByUserName = getString("claimedByUserName"),
        sequenceOrder = getLong("sequenceOrder")?.toInt() ?: 0,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt,
        deviceAlarmEnabled = getBoolean("deviceAlarmEnabled"),
        dayProfiles = dayProfiles
    )
}

/**
 * L-2: Extrahiert die Serialisierung von FamilyMember in eine Firestore-Map.
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
        "lastUpdatedAt" to FieldValue.serverTimestamp(),
        "deviceAlarmEnabled" to deviceAlarmEnabled,
        "dayProfiles" to dayProfilesData
    )
}
