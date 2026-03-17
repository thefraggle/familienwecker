package de.familienwecker.famwake.data

import com.google.firebase.firestore.DocumentSnapshot
import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import java.time.LocalTime

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
                try { LocalTime.parse(it) } catch (e: Exception) { LocalTime.of(6, 0) }
            } ?: LocalTime.of(6, 0),
            latestWakeUp = latestRaw?.let {
                try { LocalTime.parse(it) } catch (e: Exception) { LocalTime.of(7, 30) }
            } ?: LocalTime.of(7, 30),
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

    return FamilyMember(
        id = id,
        name = getString("name") ?: "Unknown",
        earliestWakeUp = try { LocalTime.parse(earliestStr) } catch (e: Exception) { LocalTime.of(6, 0) },
        latestWakeUp = try { LocalTime.parse(latestStr) } catch (e: Exception) { LocalTime.of(7, 30) },
        bathroomDurationMinutes = getLong("bathroomDurationMinutes") ?: 20L,
        wantsBreakfast = getBoolean("wantsBreakfast") ?: true,
        leaveHomeTime = leaveStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } },
        isPaused = getBoolean("isPaused") ?: false,
        isAwakeToday = getBoolean("isAwakeToday") ?: false,
        lastResetDate = getString("lastResetDate") ?: "",
        claimedByUserId = getString("claimedByUserId"),
        claimedByUserName = getString("claimedByUserName"),
        sequenceOrder = getLong("sequenceOrder")?.toInt() ?: 0,
        createdAt = getLong("createdAt"),
        lastUpdatedAt = getLong("lastUpdatedAt"),
        deviceAlarmEnabled = getBoolean("deviceAlarmEnabled"),
        dayProfiles = dayProfiles
    )
}
