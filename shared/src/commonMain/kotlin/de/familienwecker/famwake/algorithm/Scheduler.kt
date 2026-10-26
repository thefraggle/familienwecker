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
        val allActiveMembers = members.filter { !it.isPaused }
        val activeMembers = allActiveMembers.take(6)
        // M11: Warnung wenn aktive Mitglieder abgeschnitten werden
        val memberLimitExceeded = allActiveMembers.size > 6

        if (activeMembers.isEmpty()) {
            return FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveMembers)
        }

        // 1. Versuche die exakte Reihung ohne Zeit-Verschiebung, mit vollem Puffer
        val initialResult = evaluatePermutation(activeMembers, breakfastDurationMinutes, 0, globalBufferMinutes, includeInvalid = true)
        if (initialResult.isValid) {
            return if (memberLimitExceeded) initialResult.copy(scheduleMessage = ScheduleMessage.MemberLimitExceeded(allActiveMembers.size, 6)) else initialResult
        }

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
        var minLeaveForBreakfastEaters = LocalTime(23, 59)

        if (breakfastEaters.isNotEmpty()) {
            for (m in breakfastEaters) {
                val naturalBathEnd = m.latestWakeUp.plusMinutes(m.bathroomDurationMinutes.coerceAtLeast(0))
                val leave = m.leaveHomeTime ?: naturalBathEnd
                if (leave.isBefore(minLeaveForBreakfastEaters)) {
                    minLeaveForBreakfastEaters = leave
                }
            }
            // Safety-Guard: Verhindert Mitternachts-Überlauf (negative Zeit bzw. Sprung zurück zum Vortag).
            // Unterstützt auch frühe Schichten (z. B. vor 04:00 Uhr).
            val startTime = minLeaveForBreakfastEaters
            val calculatedBreakfast = startTime.minusMinutes(breakfastDurationMinutes)
            breakfastTime = if (calculatedBreakfast.isAfter(startTime)) {
                // minusMinutes-Wraparound über Mitternacht aufgetreten → auf 00:00 clampen
                LocalTime(0, 0)
            } else {
                calculatedBreakfast
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

            val rawAllowedLatest = member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
            val allowedLatestWakeUp = if (rawAllowedLatest.isBefore(member.latestWakeUp)) LocalTime(23, 59) else rawAllowedLatest
            val rawAllowedEarliest = member.earliestWakeUp.minusMinutes(shiftToleranceMinutes.toLong())
            val allowedEarliestWakeUp = if (rawAllowedEarliest.isAfter(member.earliestWakeUp)) LocalTime(0, 0) else rawAllowedEarliest

            // Fixierte Weckzeit (z.B. durch Snooze oder 0-Minuten-Fenster): earliestWakeUp == latestWakeUp
            // → Weckzeit direkt verwenden, Constraint an Vorgänger weitergeben
            val isFixed = member.earliestWakeUp == member.latestWakeUp
            val isMemberSnoozed = member.snoozeUntil != null

            val bathDuration = member.bathroomDurationMinutes.coerceAtLeast(0)
            val rawMaxBathroomEnd = allowedLatestWakeUp.plusMinutes(bathDuration)
            var maxAllowedBathroomEnd = if (rawMaxBathroomEnd.isBefore(allowedLatestWakeUp)) LocalTime(23, 59) else rawMaxBathroomEnd

            if (currentLatestBathroomEndTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = currentLatestBathroomEndTime
            }

            if (member.wantsBreakfast && breakfastTime != null) {
                // Individuelle Frühstücksdauer berücksichtigen (Fallback: globaler/übergebener Wert)
                val effectiveDuration = member.breakfastDurationMinutes ?: breakfastDurationMinutes
                val memberBreakfastStart = if (effectiveDuration == breakfastDurationMinutes) {
                    breakfastTime
                } else {
                    val mStart = if (breakfastEaters.isNotEmpty()) minLeaveForBreakfastEaters else LocalTime(8, 0)
                    val calculated = mStart.minusMinutes(effectiveDuration)
                    if (calculated.isAfter(mStart)) LocalTime(0, 0) else calculated
                }

                if (!memberBreakfastStart.isAfter(maxAllowedBathroomEnd)) {
                    maxAllowedBathroomEnd = memberBreakfastStart
                }
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
                val rawEnd = wakeUpTime.plusMinutes(bathDuration)
                bathroomEnd = if (rawEnd.isBefore(wakeUpTime)) LocalTime(23, 59) else rawEnd
                // Wenn ein fixierter Member (kein Snooze!) seinen Bad-Slot nach maxAllowedBathroomEnd beendet → Konflikt
                if (!isMemberSnoozed && bathroomEnd.isAfter(maxAllowedBathroomEnd)) {
                    if (!includeInvalid) {
                        return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                    }
                    isValid = false
                }
            } else {
                val rawWake = maxAllowedBathroomEnd.minusMinutes(bathDuration)
                wakeUpTime = if (rawWake.isAfter(maxAllowedBathroomEnd)) LocalTime(0, 0) else rawWake
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
            // dayProfiles enthält nach resolveEffectiveMember() nur 1 Entry (den aktuellen Wochentag),
            // daher ist firstOrNull() hier korrekt und liefert immer das richtige Tagesprofil.
            val effectiveBuffer = if (index < orderedMembers.lastIndex) {
                member.dayProfiles?.values?.firstOrNull()?.bufferMinutes ?: globalBufferMinutes
            } else {
                0L
            }

            // Für den nächsten Schritt (das vorhergehende Bad-Mitglied) ziehen wir den Puffer
            // des vorhergehenden Bad-Mitglieds von unserer Wakeup-Zeit ab.
            // Falls direkte Vorgänger im Simple Mode sind, suchen wir rückwärts nach dem
            // nächsten tatsächlichen Bad-Nutzer, damit dessen Puffer nicht verloren geht.
            val prevBuffer = if (index > 0) {
                var buffer = 0L
                for (p in (index - 1) downTo 0) {
                    val prev = orderedMembers[p]
                    if (!prev.isSimpleMode) {
                        buffer = prev.dayProfiles?.values?.firstOrNull()?.bufferMinutes ?: globalBufferMinutes
                        break
                    }
                }
                buffer
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
            val rawPrevEnd = wakeUpTime.minusMinutes(prevBuffer)
            currentLatestBathroomEndTime = if (rawPrevEnd.isAfter(wakeUpTime)) LocalTime(0, 0) else rawPrevEnd
        }

        // Prüfe ob ein Snooze aktiv ist – nur bei echtem Snooze werden temporäre Verschiebungen toleriert
        val hasSnoozeActive = orderedMembers.any { !it.isSimpleMode && it.snoozeUntil != null }

        // Vorwärts-Korrektur-Pass: Wenn ein fixierter Member (z.B. Snooze) seine Badzeit
        // in den Slot des Nachfolgers schiebt, müssen nachfolgende Bad-nutzende Members nach hinten
        // verschoben werden. schedules ist hier noch reversed (letzter Member zuerst).
        val forwardSchedules = schedules.reversed().toMutableList()
        var lastBathIndex = -1
        for (i in forwardSchedules.indices) {
            val next = forwardSchedules[i]
            if (next.member.isSimpleMode) continue

            if (lastBathIndex >= 0) {
                val current = forwardSchedules[lastBathIndex]
                val buffer = current.bufferAfter
                val rawRequiredStart = current.bathroomEndTime.plusMinutes(buffer)
                val requiredNextStart = if (rawRequiredStart.isBefore(current.bathroomEndTime)) LocalTime(23, 59) else rawRequiredStart

                // Wenn der nachfolgende Member vor dem Ende des aktuellen (+ Puffer) startet → verschieben
                if (next.wakeUpTime.isBefore(requiredNextStart)) {
                    val shiftedWakeUp = requiredNextStart
                    val nextBathDuration = next.member.bathroomDurationMinutes.coerceAtLeast(0)
                    val rawShiftedEnd = shiftedWakeUp.plusMinutes(nextBathDuration)
                    val shiftedBathroomEnd = if (rawShiftedEnd.isBefore(shiftedWakeUp)) LocalTime(23, 59) else rawShiftedEnd

                    if (!hasSnoozeActive) {
                        val rawNextAllowedLatest = next.member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
                        val nextAllowedLatest = if (rawNextAllowedLatest.isBefore(next.member.latestWakeUp)) LocalTime(23, 59) else rawNextAllowedLatest
                        val nextLeave = next.member.leaveHomeTime
                        if (shiftedWakeUp.isAfter(nextAllowedLatest) || (nextLeave != null && shiftedBathroomEnd.isAfter(nextLeave))) {
                            isValid = false
                            if (!includeInvalid) {
                                return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                            }
                        }
                    }

                    forwardSchedules[i] = next.copy(
                        wakeUpTime = shiftedWakeUp,
                        bathroomStartTime = shiftedWakeUp,
                        bathroomEndTime = shiftedBathroomEnd
                    )
                }
            }
            lastBathIndex = i
        }

        // Post-Validation (auf korrigierte Schedules) – bei Snooze tolerieren, da temporär
        if (breakfastTime != null && isValid && !hasSnoozeActive) {
            for (s in forwardSchedules) {
                if (s.member.wantsBreakfast && !s.member.isSimpleMode) {
                    val cutoff = if (s.member.breakfastDurationMinutes != null && s.member.breakfastDurationMinutes != breakfastDurationMinutes) {
                        val mStart = if (breakfastEaters.isNotEmpty()) minLeaveForBreakfastEaters else LocalTime(8, 0)
                        val calculated = mStart.minusMinutes(s.member.breakfastDurationMinutes)
                        if (calculated.isAfter(mStart)) LocalTime(0, 0) else calculated
                    } else {
                        breakfastTime
                    }
                    if (s.bathroomEndTime.isAfter(cutoff)) {
                        isValid = false
                        if (!includeInvalid) return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                    }
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
