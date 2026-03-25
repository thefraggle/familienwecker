package de.familienwecker.famwake.model

import kotlinx.datetime.LocalTime as KmpLocalTime
import java.time.LocalTime as JvmLocalTime

fun KmpLocalTime.toJavaLocalTime(): JvmLocalTime {
    return JvmLocalTime.of(this.hour, this.minute)
}

fun JvmLocalTime.toKmpLocalTime(): KmpLocalTime {
    return KmpLocalTime(this.hour, this.minute)
}

fun kotlinx.datetime.LocalDateTime.toJavaLocalDateTime(): java.time.LocalDateTime {
    return java.time.LocalDateTime.of(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
}

fun java.time.LocalDateTime.toKmpLocalDateTime(): kotlinx.datetime.LocalDateTime {
    return kotlinx.datetime.LocalDateTime(year, monthValue, dayOfMonth, hour, minute, second, nano)
}
