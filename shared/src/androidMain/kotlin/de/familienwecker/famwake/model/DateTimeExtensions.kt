package de.familienwecker.famwake.model

import kotlinx.datetime.LocalTime as KmpLocalTime
import java.time.LocalTime as JvmLocalTime

fun KmpLocalTime.toJavaLocalTime(): JvmLocalTime {
    return JvmLocalTime.of(this.hour, this.minute)
}

fun JvmLocalTime.toKmpLocalTime(): KmpLocalTime {
    return KmpLocalTime(this.hour, this.minute)
}
