package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.model.ScheduleResult
import de.familienwecker.famwake.util.minusMinutes
import de.familienwecker.famwake.util.plusMinutes
import de.familienwecker.famwake.util.isBefore
import de.familienwecker.famwake.util.isAfter
import kotlinx.datetime.LocalTime

class Scheduler {

    fun calculateIdealSchedule(
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long = 30,
        globalBufferMinutes: Long = 0
    ): FamilySchedule {
        // Limit active members (max 6 per UI constraint)
        val activeMembers = members.filter { !it.isPaused }.take(6)

        if (activeMembers.isEmpty()) {
            return FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveMembers)
        }

        // 1. Versuche die exakte Reihung ohne Zeit-Verschiebung, mit vollem Puffer
        val initialResult = evaluatePermutation(activeMembers, breakfastDurationMinutes, 0, globalBufferMinutes, includeInvalid = true)
        if (initialResult.isValid) return initialResult

        // 2. Puffer schrittweise reduzieren (5er-Schritte)
        if (globalBufferMinutes > 0) {
            var reducedBuffer = globalBufferMinutes - 5
            while (reducedBuffer >= 0) {
                val bufferResult = evaluatePermutation(activeMembers, breakfastDurationMinutes, 0, reducedBuffer)
                if (bufferResult.isValid) {
                    return bufferResult.copy(scheduleMessage = ScheduleMessage.BufferReduced(globalBufferMinutes, reducedBuffer))
                }
                reducedBuffer -= 5
            }
        }

        // 3. Fallback: Erlaube moderate Zeit-Verschiebungen (5-15 Min) bei festgehaltener Reihung, ohne Puffer
        for (shiftMinutes in 5..15 step 5) {
            val flexibleSchedule = evaluatePermutation(activeMembers, breakfastDurationMinutes, shiftMinutes, 0)
            if (flexibleSchedule.isValid) {
                return flexibleSchedule.copy(scheduleMessage = ScheduleMessage.TimeAdjusted(shiftMinutes))
            }
        }

        // 4. Fallback: Frühstück leicht verkürzen und mit Verschiebungen erneut probieren
        if (breakfastDurationMinutes >= 15) {
            for (reduceBreakfast in 5..10 step 5) {
                val reducedDuration = breakfastDurationMinutes - reduceBreakfast
                val reductionSchedule = evaluatePermutation(activeMembers, reducedDuration, 0, 0)

                if (reductionSchedule.isValid) {
                    return reductionSchedule.copy(scheduleMessage = ScheduleMessage.BreakfastReduced(reduceBreakfast))
                }

                // Kombiniere Frühstücksverkürzung mit Zeit-Verschiebung
                for (shiftMinutes in 5..15 step 5) {
                    val flexibleReductionSchedule = evaluatePermutation(activeMembers, reducedDuration, shiftMinutes, 0)
                    if (flexibleReductionSchedule.isValid) {
                        return flexibleReductionSchedule.copy(scheduleMessage = ScheduleMessage.BreakfastAndTimeAdjusted(reduceBreakfast, shiftMinutes))
                    }
                }
            }
        }

        // 5. Endgültiger Fehlschlag: Best-Effort Plan zurückgeben (vom ersten Versuch)
        return initialResult.copy(
            isValid = false,
            scheduleMessage = extractConflictMessage(initialResult)
        )
    }

    private fun extractConflictMessage(schedule: FamilySchedule): ScheduleMessage {
        // Findet das erste Mitglied, das den Check gerissen hat (wakeUpTime < earliestWakeUp)
        for (s in schedule.memberSchedules) {
            if (s.wakeUpTime.isBefore(s.member.earliestWakeUp)) {
                return ScheduleMessage.MemberConflict(s.member.name)
            }
        }
        return ScheduleMessage.NoValidScheduleFound
    }

    private fun evaluatePermutation(
        orderedMembers: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int = 0,
        globalBufferMinutes: Long = 0,
        includeInvalid: Boolean = false
    ): FamilySchedule {
        val breakfastEaters = orderedMembers.filter { it.wantsBreakfast && !it.isSimpleMode }
        var breakfastTime: LocalTime? = null

        if (breakfastEaters.isNotEmpty()) {
            var minLeaveForBreakfastEaters = LocalTime(23, 59)
            for (m in breakfastEaters) {
                val naturalBathEnd = m.latestWakeUp.plusMinutes(m.bathroomDurationMinutes)
                val leave = m.leaveHomeTime ?: naturalBathEnd
                if (leave.isBefore(minLeaveForBreakfastEaters)) {
                    minLeaveForBreakfastEaters = leave
                }
            }
            // Safety-Guard: Wenn leaveHomeTime oder naturalBathEnd vor 04:00 liegt
            // (z.B. durch Mitternachts-Wrap-Around der plusMinutes-Arithmetik),
            // auf 04:00 clampen. Verhindert unsinnige Frühstückszeiten um 2 Uhr nachts.
            val startTime = if (minLeaveForBreakfastEaters.isBefore(LocalTime(4, 0)))
                LocalTime(4, 0) else minLeaveForBreakfastEaters

            breakfastTime = startTime.minusMinutes(breakfastDurationMinutes)
            // Clamp auch breakfastTime: startTime ist auf 04:00 geclampt, aber
            // breakfastTime = 04:00 - Dauer kann trotzdem in die Nacht rutschen.
            if (breakfastTime?.isAfter(startTime) == true) {
                // plusMinutes-Wraparound aufgetreten → auf 03:30 als hartes Minimum setzen
                breakfastTime = LocalTime(3, 30)
            }
        }

        val schedules = mutableListOf<ScheduleResult>()
        var currentLatestBathroomEndTime = LocalTime(23, 59)
        var isValid = true

        for (index in orderedMembers.indices.reversed()) {
            val member = orderedMembers[index]
            if (member.isSimpleMode) {
                val wakeUpTime = member.latestWakeUp
                schedules.add(
                    ScheduleResult(
                        member = member,
                        wakeUpTime = wakeUpTime,
                        bathroomStartTime = wakeUpTime,
                        bathroomEndTime = wakeUpTime,
                        bufferAfter = 0L
                    )
                )
                continue
            }

            val allowedLatestWakeUp = member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
            val allowedEarliestWakeUp = member.earliestWakeUp.minusMinutes(shiftToleranceMinutes.toLong())

            // Fixierte Weckzeit (z.B. durch Snooze): earliestWakeUp == latestWakeUp
            // → Weckzeit direkt verwenden, Constraint an Vorgänger weitergeben
            val isFixed = member.earliestWakeUp == member.latestWakeUp

            var maxAllowedBathroomEnd = allowedLatestWakeUp.plusMinutes(member.bathroomDurationMinutes)

            if (currentLatestBathroomEndTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = currentLatestBathroomEndTime
            }

            if (member.wantsBreakfast && breakfastTime != null && !breakfastTime.isAfter(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = breakfastTime
            }

            val leaveTime = member.leaveHomeTime
            if (leaveTime != null && leaveTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = leaveTime
            }

            // Bei fixierter Weckzeit: exakt diese Zeit verwenden, Badende daraus berechnen
            val wakeUpTime: LocalTime
            val bathroomEnd: LocalTime
            if (isFixed) {
                wakeUpTime = member.latestWakeUp
                bathroomEnd = wakeUpTime.plusMinutes(member.bathroomDurationMinutes)
            } else {
                wakeUpTime = maxAllowedBathroomEnd.minusMinutes(member.bathroomDurationMinutes)
                bathroomEnd = maxAllowedBathroomEnd
            }

            if (!isFixed && wakeUpTime.isBefore(allowedEarliestWakeUp)) {
                if (!includeInvalid) {
                    return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                }
                isValid = false
            }

            // Der Puffer nach diesem Mitglied bestimmt den Abstand zum Nachfolgenden.
            // Wenn es kein nachfolgendes Mitglied gibt (letztes Element), ist der Puffer 0.
            val effectiveBuffer = if (index < orderedMembers.lastIndex) {
                member.dayProfiles?.values?.firstOrNull()?.bufferMinutes ?: globalBufferMinutes
            } else {
                0L
            }

            // Für den nächsten Schritt (das vorhergehende Mitglied) ziehen wir den Puffer
            // des vorhergehenden Mitglieds von unserer Wakeup-Zeit ab.
            val prevBuffer = if (index > 0) {
                val prevMember = orderedMembers[index - 1]
                if (prevMember.isSimpleMode) {
                    0L
                } else {
                    prevMember.dayProfiles?.values?.firstOrNull()?.bufferMinutes ?: globalBufferMinutes
                }
            } else {
                0L
            }

            schedules.add(
                ScheduleResult(
                    member = member,
                    wakeUpTime = wakeUpTime,
                    bathroomStartTime = wakeUpTime,
                    bathroomEndTime = bathroomEnd,
                    bufferAfter = effectiveBuffer
                )
            )
            currentLatestBathroomEndTime = wakeUpTime.minusMinutes(prevBuffer)
        }

        // Vorwärts-Korrektur-Pass: Wenn ein fixierter Member (Snooze) seine Badzeit
        // in den Slot des Nachfolgers schiebt, müssen nachfolgende Members nach hinten
        // verschoben werden. schedules ist hier noch reversed (letzter Member zuerst).
        val forwardSchedules = schedules.reversed().toMutableList()
        for (i in 0 until forwardSchedules.size - 1) {
            val current = forwardSchedules[i]
            val next = forwardSchedules[i + 1]
            if (next.member.isSimpleMode) continue

            val buffer = current.bufferAfter
            val requiredNextStart = current.bathroomEndTime.plusMinutes(buffer)

            // Wenn der nachfolgende Member vor dem Ende des aktuellen (+ Puffer) startet → verschieben
            if (next.wakeUpTime.isBefore(requiredNextStart)) {
                val shiftedWakeUp = requiredNextStart
                val shiftedBathroomEnd = shiftedWakeUp.plusMinutes(next.member.bathroomDurationMinutes)
                forwardSchedules[i + 1] = next.copy(
                    wakeUpTime = shiftedWakeUp,
                    bathroomStartTime = shiftedWakeUp,
                    bathroomEndTime = shiftedBathroomEnd
                )
            }
        }



        // Prüfe ob ein Snooze (fixierter Member) aktiv ist – dann toleriere Verschiebungen
        val hasSnoozeActive = orderedMembers.any { it.earliestWakeUp == it.latestWakeUp }

        // Post-Validation (auf korrigierte Schedules) – bei Snooze tolerieren, da temporär
        if (breakfastTime != null && isValid && !hasSnoozeActive) {
            for (s in forwardSchedules) {
                if (s.member.wantsBreakfast && s.bathroomEndTime.isAfter(breakfastTime)) {
                    isValid = false
                    if (!includeInvalid) return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                }
            }
        }

        // Bei Snooze-bedingter Verschiebung: Plan als gültig markieren, da die
        // Verschiebung temporär ist und kein User-Eingriff erforderlich ist.
        if (hasSnoozeActive && !isValid) {
            isValid = true
        }

        return FamilySchedule(
            memberSchedules = forwardSchedules,
            breakfastTime = breakfastTime,
            isValid = isValid,
            scheduleMessage = if (isValid) ScheduleMessage.OptimalPlan else ScheduleMessage.NoValidScheduleFound
        )
    }
}
