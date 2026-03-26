package de.familienwecker.famwake.alarm

import kotlinx.datetime.LocalDateTime

interface AlarmPlatformScheduler {
    fun scheduleWakeUp(
        wakeUpTime: LocalDateTime,
        memberId: String,
        memberName: String,
        soundUri: String? = null,
        isSnooze: Boolean = false,
        onPermissionDenied: (() -> Unit)? = null
    )
    
    fun cancelWakeUp(memberId: String, isSnooze: Boolean = false)
}
