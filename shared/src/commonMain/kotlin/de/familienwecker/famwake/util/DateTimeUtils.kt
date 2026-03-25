package de.familienwecker.famwake.util

import kotlinx.datetime.LocalTime

fun LocalTime.plusMinutes(minutes: Long): LocalTime {
    val totalMinutes = (this.hour * 60 + this.minute + minutes) % (24 * 60)
    val adjustedMinutes = if (totalMinutes < 0) totalMinutes + (24 * 60) else totalMinutes
    return LocalTime((adjustedMinutes / 60).toInt(), (adjustedMinutes % 60).toInt())
}

fun LocalTime.minusMinutes(minutes: Long): LocalTime {
    return plusMinutes(-minutes)
}

fun LocalTime.isBefore(other: LocalTime): Boolean {
    return this < other
}

fun LocalTime.isAfter(other: LocalTime): Boolean {
    return this > other
}
