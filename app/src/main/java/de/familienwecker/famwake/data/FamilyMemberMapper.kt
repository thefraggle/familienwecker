package de.familienwecker.famwake.data

import com.google.firebase.firestore.DocumentSnapshot
import de.familienwecker.famwake.model.FamilyMember
import java.time.LocalTime

/**
 * M-1: Extrahiert das Duplikat-Mapping von Firestore-Dokument zu FamilyMember.
 * Vorher war dieses Mapping in getFamilyMembersFlow() und getClaimedMember() dupliziert.
 */
fun DocumentSnapshot.toFamilyMember(): FamilyMember {
    val earliestStr = getString("earliestWakeUp") ?: "06:00"
    val latestStr = getString("latestWakeUp") ?: "07:30"
    val leaveStr = getString("leaveHomeTime")

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
        deviceAlarmEnabled = getBoolean("deviceAlarmEnabled")
    )
}
