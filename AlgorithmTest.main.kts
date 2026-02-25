import java.time.LocalTime

/**
 * 1. DATENMODELLE
 */
data class FamilyMember(
    val id: String,
    val name: String,
    val earliestWakeUp: LocalTime,
    val latestWakeUp: LocalTime,
    val bathroomDurationMinutes: Long,
    val wantsBreakfast: Boolean,
    val leaveHomeTime: LocalTime? = null
)

data class ScheduleResult(
    val member: FamilyMember,
    val wakeUpTime: LocalTime,
    val bathroomStartTime: LocalTime,
    val bathroomEndTime: LocalTime
)

data class FamilySchedule(
    val memberSchedules: List<ScheduleResult>,
    val breakfastTime: LocalTime?, // Gemeinsame Frühstückszeit (falls gewünscht)
    val isValid: Boolean,
    val message: String
)

/**
 * 2. ALGORITHMUS LOGIK
 */
class Scheduler {

    /**
     * Berechnet den bestmöglichen Zeitplan anhand der Präferenzen aller Familienmitglieder.
     * Nutzt Permutationen (alle möglichen Reihenfolgen für das Bad), was bei kleinen Familiengrößen (< 8 Personen) extrem schnell ist.
     */
    fun calculateIdealSchedule(
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long = 30
    ): FamilySchedule {

        if (members.isEmpty()) return FamilySchedule(emptyList(), null, true, "Keine Mitglieder vorhanden.")

        // Alle möglichen Reihenfolgen für's Bad ermitteln (Permutation)
        val permutations = generatePermutations(members)

        var bestSchedule = findBestScheduleOverPermutations(permutations, members, breakfastDurationMinutes, 0)

        // Fallback 1: Try shifting wake-up times
        if (bestSchedule == null || !bestSchedule.isValid) {
            for (shiftMinutes in 5..15 step 5) {
                val flexibleSchedule = findBestScheduleOverPermutations(permutations, members, breakfastDurationMinutes, shiftMinutes)
                if (flexibleSchedule != null && flexibleSchedule.isValid) {
                    return flexibleSchedule.copy(message = "Zeiten wurden um $shiftMinutes Minuten flexibel angepasst, um Konflikte zu lösen.")
                }
            }
        }

        // Fallback 2: Reduce breakfast time
        if ((bestSchedule == null || !bestSchedule.isValid) && breakfastDurationMinutes >= 15) {
            for (reduceBreakfast in 5..10 step 5) {
                val reducedDuration = breakfastDurationMinutes - reduceBreakfast
                val sched = findBestScheduleOverPermutations(permutations, members, reducedDuration, 0)
                if (sched != null && sched.isValid) {
                    return sched.copy(message = "Frühstück wurde um $reduceBreakfast Minuten verkürzt, um Konflikte zu lösen.")
                }
                
                // Try shifts with reduced breakfast
                for (shiftMinutes in 5..15 step 5) {
                    val flexibleSchedule = findBestScheduleOverPermutations(permutations, members, reducedDuration, shiftMinutes)
                    if (flexibleSchedule != null && flexibleSchedule.isValid) {
                        return flexibleSchedule.copy(message = "Frühstück wurde um $reduceBreakfast Min. verkürzt & Zeiten um $shiftMinutes Min. angepasst.")
                    }
                }
            }
        }

        // Falls nix gefunden wurde
        return bestSchedule ?: FamilySchedule(
            emptyList(), null, false, 
            "Kein gültiger Zeitplan gefunden! Eure Zeiten (Bad/Frühstück) überschneiden sich so sehr, dass kein Kompromiss möglich ist."
        )
    }

    private fun findBestScheduleOverPermutations(
        permutations: List<List<FamilyMember>>,
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int
    ): FamilySchedule? {
        var bestSchedule: FamilySchedule? = null
        var bestScore = -1L

        for (perm in permutations) {
            val scheduleOpt = evaluatePermutation(perm, breakfastDurationMinutes, shiftToleranceMinutes)
            if (scheduleOpt != null) {
                val score = scheduleOpt.memberSchedules.sumOf { it.wakeUpTime.toSecondOfDay().toLong() }
                if (score > bestScore) {
                    bestScore = score
                    bestSchedule = scheduleOpt
                }
            }
        }
        return bestSchedule
    }

    private fun evaluatePermutation(
        orderedMembers: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int = 0
    ): FamilySchedule? {
        
        // Finde die gemeinsame Frühstückszeit (Startzeit), falls irgendjemand frühstücken möchte
        val breakfastEaters = orderedMembers.filter { it.wantsBreakfast }
        var breakfastTime: LocalTime? = null

        if (breakfastEaters.isNotEmpty()) {
            // Wer muss als erstes das Haus verlassen?
            var minLeaveForBreakfastEaters = LocalTime.MAX
            for (m in breakfastEaters) {
                val leave = m.leaveHomeTime ?: LocalTime.of(23, 59)
                if (leave.isBefore(minLeaveForBreakfastEaters)) {
                    minLeaveForBreakfastEaters = leave
                }
            }
            if (minLeaveForBreakfastEaters != LocalTime.MAX) {
                // Die Frühstückszeit wird so gelegt, dass auch der Erste pünktlich das Haus verlassen kann.
                breakfastTime = minLeaveForBreakfastEaters.minusMinutes(breakfastDurationMinutes)
            }
        }

        val schedules = mutableListOf<ScheduleResult>()

        // Wir planen rückwärts (vom Letzten im Bad zum Ersten). 
        // Jeder bekommt dadurch die so spät wie möglich machbare Zeit!
        var currentLatestBathroomEndTime = LocalTime.MAX

        for (member in orderedMembers.reversed()) {

            val allowedLatestWakeUp = member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
            val allowedEarliestWakeUp = member.earliestWakeUp.minusMinutes(shiftToleranceMinutes.toLong())

            // 1. Zuerst legen wir die spätestmögliche Bad-Endzeit basierend auf seiner reinen spätesten Weckzeit fest:
            var maxAllowedBathroomEnd = allowedLatestWakeUp.plusMinutes(member.bathroomDurationMinutes)

            // 2. Er darf nicht mit dem nächsten (der nach ihm ins Bad will) kollidieren:
            if (currentLatestBathroomEndTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = currentLatestBathroomEndTime
            }

            // 3. Fall: Wenn er frühstückt, muss er vor Beginn des gemeinsamen Frühstücks fertig sein:
            if (member.wantsBreakfast && breakfastTime != null && breakfastTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = breakfastTime
            }

            // 4. Fall: Er verlässt das Haus (ohne Frühstück) und muss davor fertig sein:
            val leaveTime = member.leaveHomeTime
            if (leaveTime != null && leaveTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = leaveTime
            }

            // Daraus ergibt sich seine endgültige Weck-Zeik = Bad-Start-Zeit:
            val wakeUpTime = maxAllowedBathroomEnd.minusMinutes(member.bathroomDurationMinutes)

            // 5. Letzter Check: Wurde er jetzt SO FRÜH eingeplant, dass es noch vor seiner "Frühesten Weckzeit" liegt?
            if (wakeUpTime.isBefore(allowedEarliestWakeUp)) {
                // Diese Reihefolge ist ungültig! Wir brechen diese Permutation ab.
                return null
            }

            schedules.add(
                ScheduleResult(
                    member = member,
                    wakeUpTime = wakeUpTime,               // Er wird instant in Bad geschickt
                    bathroomStartTime = wakeUpTime,
                    bathroomEndTime = maxAllowedBathroomEnd
                )
            )

            // Für den nächsten Schleifendurchlauf (Die Person, die VOR ihm ins Bad geht)
            currentLatestBathroomEndTime = wakeUpTime
        }

        return FamilySchedule(
            memberSchedules = schedules.reversed(),
            breakfastTime = breakfastTime,
            isValid = true,
            message = "Optimaler Plan berechnet."
        )
    }

    private fun <T> generatePermutations(list: List<T>): List<List<T>> {
        if (list.isEmpty()) return listOf(emptyList())
        val result = mutableListOf<List<T>>()
        for (i in list.indices) {
            val current = list[i]
            val remaining = list.toMutableList().apply { removeAt(i) }
            for (perm in generatePermutations(remaining)) {
                result.add(listOf(current) + perm)
            }
        }
        return result
    }
}

/**
 * 3. TEST SZENARIO (Mock-Daten)
 */
fun runTestSzenario() {
    val m1 = FamilyMember(
        id = "1", name = "Papa", 
        earliestWakeUp = LocalTime.of(5, 30), 
        latestWakeUp = LocalTime.of(7, 30), 
        bathroomDurationMinutes = 20, 
        wantsBreakfast = true, 
        leaveHomeTime = LocalTime.of(8, 0)
    )
    val m2 = FamilyMember(
        id = "2", name = "Mama", 
        earliestWakeUp = LocalTime.of(6, 0), 
        latestWakeUp = LocalTime.of(7, 45), 
        bathroomDurationMinutes = 30, 
        wantsBreakfast = true, 
        leaveHomeTime = LocalTime.of(8, 30) // Fährt später zur Arbeit
    )
    val m3 = FamilyMember(
        id = "3", name = "Sohn", 
        earliestWakeUp = LocalTime.of(6, 30), 
        latestWakeUp = LocalTime.of(7, 0), 
        bathroomDurationMinutes = 15, 
        wantsBreakfast = true, 
        leaveHomeTime = LocalTime.of(7, 45) // Bus zur Schule!
    )
    val m4 = FamilyMember(
        id = "4", name = "Tochter", 
        earliestWakeUp = LocalTime.of(6, 15), 
        latestWakeUp = LocalTime.of(8, 0), 
        bathroomDurationMinutes = 25, 
        wantsBreakfast = false, // Frühstückt auswärts
        leaveHomeTime = LocalTime.of(8, 30)
    )

    val family = listOf(m1, m2, m3, m4)
    val scheduler = Scheduler()

    println("Bilde Zeitplan für ${family.size} Personen...")
    val result = scheduler.calculateIdealSchedule(family, breakfastDurationMinutes = 25)

    if (result.isValid) {
        println("✅ ZEITPLAN ERFOLGREICH BERECHNET!")
        println(result.message)
        println("--------------------------------------------------")
        
        result.breakfastTime?.let {
            println("GEMEINSAMES FRÜHSTÜCK STARTET UM: $it")
            println("--------------------------------------------------")
        }

        // Sortiere nach Weckzeit für die Ausgabe
        result.memberSchedules.sortedBy { it.wakeUpTime }.forEach {
            println(
                "⏰ ${it.member.name} wird geweckt um: ${it.wakeUpTime} " +
                "(Bad: ${it.bathroomStartTime} - ${it.bathroomEndTime})" +
                (if (it.member.wantsBreakfast) " ☕ Frühstückt mit." else " 🏃 Geht ohne Frühstück.")
            )
        }
    } else {
        println("❌ FEHLER: ${result.message}")
    }
}

// Ausführen
runTestSzenario()
