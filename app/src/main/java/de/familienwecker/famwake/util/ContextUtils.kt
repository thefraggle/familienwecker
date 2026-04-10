package de.familienwecker.famwake.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traversiert die ContextWrapper-Kette aufwärts bis eine Activity gefunden wird.
 * Nötig weil LocalContext im Compose-Tree durch CompositionLocalProvider
 * mit einem ContextWrapper überschrieben sein kann (z.B. für Locale-Override).
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
