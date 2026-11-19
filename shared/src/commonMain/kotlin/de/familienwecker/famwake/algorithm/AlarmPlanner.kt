package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

data class SchedulePlanResult(
    val uiSchedule: FamilySchedule,
    val deviceSchedule: FamilySchedule
)

enum class AlarmCancelReason {
    NO_MEMBER_ID,
    AWAKE_TODAY,
    VACATION,
    DAY_INACTIVE,
    NOT_FOUND
}

sealed class AlarmDecision {
    data class ScheduleSnooze(
        val wakeUpTime: LocalDateTime,
        val memberId: String,
        val memberName: String
    ) : AlarmDecision()

    data class ScheduleRegular(
        val wakeUpTime: LocalDateTime,
        val memberId: String,
        val memberName: String,
        val targetDate: LocalDate
    ) : AlarmDecision()

    data object KeepGracePeriod : AlarmDecision()

    data class CancelAll(val reason: AlarmCancelReason) : AlarmDecision()
}

data class RebootAlarmDecision(
    val targetDateTime: LocalDateTime,
    val isImmediateMissed: Boolean,
    val missedAlarmTime: LocalTime? = null,
    val notifyMissed: Boolean = false
)

/**
 * Tiefes in-process Planungs-Module für UI-Schedule, Device-Alarm-Entscheidungen
 * und Boot-Wiederherstellung. Hält alle Alarm-Regeln an einer Stelle (Locality).
 */
object AlarmPlanner {

    private const val GRACE_PERIOD_MILLIS = 300_000L
    private const val REBOOT_MISSED_WINDOW_MINUTES = 30L

    fun planSchedules(
        members: List<FamilyMember>,
        myMemberId: String?,
        selectedDate: LocalDate?,
        globalBufferMinutes: Long,
        vacationUntil: String?,
        isAwakeToday: Boolean,
        now: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        scheduler: Scheduler = Scheduler()
    ): SchedulePlanResult {
        val today = now.date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val nowTime = now.time

        val uiTargetDate = resolveUiTargetDate(
            members = members,
            selectedDate = selectedDate,
            globalBufferMinutes = globalBufferMinutes,
            today = today,
            tomorrow = tomorrow,
            nowTime = nowTime,
            timeZone = timeZone,
            scheduler = scheduler
        )
        val uiSchedule = calculateForDate(
            members = members,
            targetDate = uiTargetDate,
            globalBufferMinutes = globalBufferMinutes,
            timeZone = timeZone,
            scheduler = scheduler
        )

        val deviceTargetDate = resolveDeviceTargetDate(
            members = members,
            myMemberId = myMemberId,
            globalBufferMinutes = globalBufferMinutes,
            vacationUntil = vacationUntil,
            isAwakeToday = isAwakeToday,
            today = today,
            tomorrow = tomorrow,
            nowTime = nowTime,
            timeZone = timeZone,
            scheduler = scheduler
        )
        val deviceSchedule = calculateForDate(
            members = members,
            targetDate = deviceTargetDate,
            globalBufferMinutes = globalBufferMinutes,
            timeZone = timeZone,
            scheduler = scheduler
        )

        return SchedulePlanResult(
            uiSchedule = uiSchedule,
            deviceSchedule = deviceSchedule
        )
    }

    fun isInGracePeriod(
        myMember: FamilyMember?,
        now: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean {
        val today = now.date
        val myWakeUpToday = myMember?.dayProfiles?.get(today.dayOfWeek.value)?.latestWakeUp ?: return false
        val todayAlarmMillis = LocalDateTime(today, myWakeUpToday).toInstant(timeZone).toEpochMilliseconds()
        val nowMillis = now.toInstant(timeZone).toEpochMilliseconds()
        return (nowMillis - todayAlarmMillis) in 0..GRACE_PERIOD_MILLIS
    }

    fun decideAlarmAction(
        schedule: FamilySchedule,
        myMemberId: String?,
        isAwakeToday: Boolean,
        vacationUntil: String?,
        snoozeUntil: LocalDateTime?,
        now: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): AlarmDecision {
        val currentMyMemberId = myMemberId ?: return AlarmDecision.CancelAll(AlarmCancelReason.NO_MEMBER_ID)
        val today = now.date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val nowTime = now.time

        val myMemberSchedule = schedule.memberSchedules.find { it.member.id == currentMyMemberId }
        val targetDate = schedule.targetDate
            ?: if (myMemberSchedule != null && nowTime > myMemberSchedule.wakeUpTime) tomorrow else today

        if (isAwakeToday && targetDate == today) {
            return AlarmDecision.CancelAll(AlarmCancelReason.AWAKE_TODAY)
        }

        if (!vacationUntil.isNullOrBlank() && targetDate.toString() <= vacationUntil) {
            return AlarmDecision.CancelAll(AlarmCancelReason.VACATION)
        }

        if (snoozeUntil != null && snoozeUntil > now) {
            val memberName = myMemberSchedule?.member?.name ?: ""
            return AlarmDecision.ScheduleSnooze(
                wakeUpTime = snoozeUntil,
                memberId = currentMyMemberId,
                memberName = memberName
            )
        }

        if (myMemberSchedule == null) {
            return AlarmDecision.CancelAll(AlarmCancelReason.NOT_FOUND)
        }

        val wakeUpTime = myMemberSchedule.wakeUpTime
        if (targetDate != today && !isAwakeToday) {
            val todayAlarmMillis = LocalDateTime(today, wakeUpTime).toInstant(timeZone).toEpochMilliseconds()
            val nowMillis = now.toInstant(timeZone).toEpochMilliseconds()
            if ((nowMillis - todayAlarmMillis) in 0..GRACE_PERIOD_MILLIS) {
                return AlarmDecision.KeepGracePeriod
            }
        }

        val dayProfile = myMemberSchedule.member.dayProfiles?.get(targetDate.dayOfWeek.value)
        if (dayProfile != null && !dayProfile.isActive) {
            return AlarmDecision.CancelAll(AlarmCancelReason.DAY_INACTIVE)
        }

        return AlarmDecision.ScheduleRegular(
            wakeUpTime = LocalDateTime(targetDate, wakeUpTime),
            memberId = currentMyMemberId,
            memberName = myMemberSchedule.member.name,
            targetDate = targetDate
        )
    }

    fun decideRebootAlarm(
        savedDateTime: LocalDateTime,
        now: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): RebootAlarmDecision {
        if (savedDateTime > now) {
            return RebootAlarmDecision(
                targetDateTime = savedDateTime,
                isImmediateMissed = false
            )
        }

        val alarmTime = savedDateTime.time
        if (now.time < alarmTime) {
            return RebootAlarmDecision(
                targetDateTime = LocalDateTime(now.date, alarmTime),
                isImmediateMissed = false
            )
        }

        val todayAlarm = LocalDateTime(now.date, alarmTime)
        val minutesMissed = (now.toInstant(timeZone) - todayAlarm.toInstant(timeZone)).inWholeMinutes
        return if (minutesMissed in 0..REBOOT_MISSED_WINDOW_MINUTES) {
            val tenSecondsLater = (now.toInstant(timeZone) + 10.seconds).toLocalDateTime(timeZone)
            RebootAlarmDecision(
                targetDateTime = tenSecondsLater,
                isImmediateMissed = true
            )
        } else {
            RebootAlarmDecision(
                targetDateTime = LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), alarmTime),
                isImmediateMissed = false,
                missedAlarmTime = alarmTime,
                notifyMissed = true
            )
        }
    }

    private fun resolveUiTargetDate(
        members: List<FamilyMember>,
        selectedDate: LocalDate?,
        globalBufferMinutes: Long,
        today: LocalDate,
        tomorrow: LocalDate,
        nowTime: LocalTime,
        timeZone: TimeZone,
        scheduler: Scheduler
    ): LocalDate {
        if (selectedDate != null) {
            if (selectedDate == today) {
                val todayMembers = members.map { NextAlarmCalculator.resolveEffectiveMember(it, forDate = today, timeZone = timeZone) }
                if (todayMembers.any { !it.isPaused }) {
                    val todaySchedule = scheduler.calculateIdealSchedule(todayMembers, globalBufferMinutes = globalBufferMinutes)
                    val latestAlarm = todaySchedule.memberSchedules.maxByOrNull { it.wakeUpTime }?.wakeUpTime
                    if (latestAlarm != null && nowTime < latestAlarm) {
                        return today
                    }
                }
                return today.plus(7, DateTimeUnit.DAY)
            }
            return selectedDate
        }

        val todayMembers = members.map { NextAlarmCalculator.resolveEffectiveMember(it, forDate = today, timeZone = timeZone) }
        if (todayMembers.any { !it.isPaused }) {
            val todaySchedule = scheduler.calculateIdealSchedule(todayMembers, globalBufferMinutes = globalBufferMinutes)
            val latestAlarm = todaySchedule.memberSchedules.maxByOrNull { it.wakeUpTime }?.wakeUpTime
            if (latestAlarm != null && nowTime < latestAlarm) {
                return today
            }
        }

        val tomorrowMembers = members.map { NextAlarmCalculator.resolveEffectiveMember(it, forDate = tomorrow, timeZone = timeZone) }
        return if (tomorrowMembers.any { !it.isPaused }) tomorrow else today
    }

    private fun resolveDeviceTargetDate(
        members: List<FamilyMember>,
        myMemberId: String?,
        globalBufferMinutes: Long,
        vacationUntil: String?,
        isAwakeToday: Boolean,
        today: LocalDate,
        tomorrow: LocalDate,
        nowTime: LocalTime,
        timeZone: TimeZone,
        scheduler: Scheduler
    ): LocalDate {
        val isVacationActive = !vacationUntil.isNullOrBlank() && today.toString() <= vacationUntil
        if (isVacationActive) {
            val vacEndLocalDate = runCatching { LocalDate.parse(vacationUntil!!) }.getOrNull()
                ?: return tomorrow
            val startSearchDate = vacEndLocalDate.plus(1, DateTimeUnit.DAY)
            for (offset in 0..6) {
                val checkDate = startSearchDate.plus(offset, DateTimeUnit.DAY)
                val dow = checkDate.dayOfWeek.value
                val hasActiveMember = members.any { member ->
                    val profile = member.dayProfiles?.get(dow)
                    !member.isPaused && (profile?.isActive == true)
                }
                if (hasActiveMember) return checkDate
            }
            return startSearchDate
        }

        if (isAwakeToday) return tomorrow

        val todayMembers = members.map { NextAlarmCalculator.resolveEffectiveMember(it, forDate = today, timeZone = timeZone) }
            .sortedWith(compareBy({ it.dayProfiles?.get(today.dayOfWeek.value)?.sequenceOrder ?: it.sequenceOrder }, { it.createdAt ?: 0L }))
        if (todayMembers.none { !it.isPaused }) return tomorrow

        val todaySchedule = scheduler.calculateIdealSchedule(todayMembers, globalBufferMinutes = globalBufferMinutes)
        val myTodaySchedule = myMemberId?.let { id -> todaySchedule.memberSchedules.find { it.member.id == id } }
        val latestAlarm = todaySchedule.memberSchedules.maxByOrNull { it.wakeUpTime }?.wakeUpTime

        return when {
            myTodaySchedule != null -> if (nowTime < myTodaySchedule.wakeUpTime) today else tomorrow
            latestAlarm != null && nowTime < latestAlarm -> today
            else -> tomorrow
        }
    }

    private fun calculateForDate(
        members: List<FamilyMember>,
        targetDate: LocalDate,
        globalBufferMinutes: Long,
        timeZone: TimeZone,
        scheduler: Scheduler
    ): FamilySchedule {
        val calculationMembers = members.map { NextAlarmCalculator.resolveEffectiveMember(it, forDate = targetDate, timeZone = timeZone) }
            .sortedWith(compareBy({ it.dayProfiles?.get(targetDate.dayOfWeek.value)?.sequenceOrder ?: it.sequenceOrder }, { it.createdAt ?: 0L }))
        return if (calculationMembers.none { !it.isPaused }) {
            FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule).copy(targetDate = targetDate)
        } else {
            scheduler.calculateIdealSchedule(calculationMembers, globalBufferMinutes = globalBufferMinutes)
                .copy(targetDate = targetDate)
        }
    }
}
