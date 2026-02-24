package com.example.familienwecker.model

import java.time.LocalTime

data class FamilyMember(
    val id: String,
    val name: String,
    val earliestWakeUp: LocalTime,
    val latestWakeUp: LocalTime,
    val bathroomDurationMinutes: Long,
    val wantsBreakfast: Boolean,
    val leaveHomeTime: LocalTime? = null,
    val isPaused: Boolean = false
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
    val message: String
)
