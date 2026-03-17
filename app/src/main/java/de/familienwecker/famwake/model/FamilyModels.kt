package de.familienwecker.famwake.model

import java.time.LocalTime

data class DayProfile(
    val isActive: Boolean = true,
    val earliestWakeUp: LocalTime = LocalTime.of(6, 0),
    val latestWakeUp: LocalTime = LocalTime.of(7, 30),
    val bathroomDurationMinutes: Long = 20L,
    val wantsBreakfast: Boolean = true,
    val leaveHomeTime: LocalTime? = null
)

data class FamilyMember(
    val id: String,
    val name: String,
    val earliestWakeUp: LocalTime,
    val latestWakeUp: LocalTime,
    val bathroomDurationMinutes: Long,
    val wantsBreakfast: Boolean,
    val leaveHomeTime: LocalTime? = null,
    val isPaused: Boolean = false,
    val isAwakeToday: Boolean = false,
    val lastResetDate: String = "", // YYYY-MM-DD
    val claimedByUserId: String? = null,
    val claimedByUserName: String? = null,
    val sequenceOrder: Int = 0,    // Manuelle Reihung (0 = am Anfang)
    val createdAt: Long? = null,   // Epoch-Millis beim ersten Anlegen – für stabile Sortierung
    val lastUpdatedAt: Long? = null, // Epoch-Millis für Konfliktlösung (Last Intent Wins)
    val deviceAlarmEnabled: Boolean? = null, // Vom Gerät des geclaimten Users gesetzter Alarm-Status (nur Anzeige)
    // Key = DayOfWeek value (1=Mo…7=So). null = Feature nicht konfiguriert (Fallback: bestehende Felder)
    val dayProfiles: Map<Int, DayProfile>? = null
)

data class ScheduleResult(
    val member: FamilyMember,
    val wakeUpTime: LocalTime,
    val bathroomStartTime: LocalTime,
    val bathroomEndTime: LocalTime
)

data class FamilySchedule(
    val memberSchedules: List<ScheduleResult>,
    val breakfastTime: LocalTime?,
    val isValid: Boolean,
    val scheduleMessage: ScheduleMessage
)

data class FamilyData(
    val id: String,
    val name: String,
    val createdByUserId: String?
)

data class SyncStatus(
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false
)
