package de.familienwecker.famwake.util

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.telemetrydeck.sdk.TelemetryDeck
import de.familienwecker.famwake.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class DeviceTrustLevel {
    TRUSTED,   // Gerät besteht den Integritätscheck
    UNTRUSTED, // Gerät ist manipuliert (Root, modifizierte App, etc.)
    UNKNOWN    // Check nicht möglich (kein Netz, API-Fehler, Timeout) → fail-open
}

/**
 * Führt den Play Integrity Check durch und loggt das Ergebnis via TelemetryDeck.
 *
 * Monitoring-Modus (v1.7.7): ENFORCEMENT_ENABLED = false.
 * In diesem Modus wird der Check immer durchgeführt und geloggt,
 * aber nie blockiert – der Rückgabewert ist immer UNKNOWN.
 * Erst wenn ENFORCEMENT_ENABLED = true gesetzt wird (v1.7.8+),
 * greift die echte Sperre für UNTRUSTED-Geräte.
 */
class DeviceTrustManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceTrustManager"

        // Monitoring-Phase: auf true setzen wenn echte Nutzungsdaten
        // aus TelemetryDeck zeigen, dass keine legitimen Nutzer betroffen sind.
        private const val ENFORCEMENT_ENABLED = false

        // Nonce muss mind. 16 Zeichen lang sein (Play Integrity Anforderung)
        private fun generateNonce(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            return (1..24).map { chars.random() }.joinToString("")
        }
    }

    /**
     * Prüft die Geräteintegrität via Play Integrity API.
     *
     * Im Monitoring-Modus (ENFORCEMENT_ENABLED = false):
     *   → Check + Log läuft, gibt aber immer UNKNOWN zurück (kein Blocking).
     *
     * Im Enforcement-Modus (ENFORCEMENT_ENABLED = true):
     *   → UNTRUSTED-Geräte erhalten keinen Firebase-Sync.
     */
    suspend fun checkTrust(): DeviceTrustLevel = withContext(Dispatchers.IO) {
        val verdict = fetchVerdict()
        logVerdict(verdict)

        // Monitoring: Immer fail-open, Sync wird nie gesperrt
        if (!ENFORCEMENT_ENABLED) return@withContext DeviceTrustLevel.UNKNOWN

        verdict
    }

    private suspend fun fetchVerdict(): DeviceTrustLevel {
        return try {
            val result = withTimeoutOrNull(5000L) {
                val manager = IntegrityManagerFactory.create(context)
                val request = IntegrityTokenRequest.builder()
                    .setNonce(generateNonce())
                    .build()
                manager.requestIntegrityToken(request).await()
            } ?: return DeviceTrustLevel.UNKNOWN // Timeout → fail-open

            // Token dekodieren: Base64 JWT (Payload ist der mittlere Teil)
            // Wir parsen nur den Verdict-Teil – vollständige Verifikation
            // sollte serverseitig erfolgen (für v1.7.8 mit Cloud Function).
            // In der Monitoring-Phase reicht der lokale Parse für die Telemetry.
            val token = result.token()
            parseVerdictFromToken(token)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Play Integrity check failed: ${e.message}")
            }
            DeviceTrustLevel.UNKNOWN // Jeder Fehler → fail-open
        }
    }

    /**
     * Dekodiert den JWT-Payload des Integrity-Tokens (Base64url) und prüft
     * ob "MEETS_DEVICE_INTEGRITY" im deviceRecognitionVerdict enthalten ist.
     *
     * Hinweis: Diese Client-seitige Auswertung dient ausschließlich dem
     * TelemetryDeck-Logging in der Monitoring-Phase. Für Enforcement (v1.7.8)
     * wird die Verifikation serverseitig in einer Cloud Function erfolgen.
     */
    private fun parseVerdictFromToken(token: String): DeviceTrustLevel {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return DeviceTrustLevel.UNKNOWN

            val payload = parts[1]
            // Base64url → Base64 → String
            val padded = payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')
            val decoded = android.util.Base64.decode(
                padded.replace('-', '+').replace('_', '/'),
                android.util.Base64.DEFAULT
            ).toString(Charsets.UTF_8)

            // Einfacher String-Check statt vollständigem JSON-Parse
            if (decoded.contains("MEETS_DEVICE_INTEGRITY")) {
                DeviceTrustLevel.TRUSTED
            } else {
                DeviceTrustLevel.UNTRUSTED
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Token parse failed: ${e.message}")
            }
            DeviceTrustLevel.UNKNOWN
        }
    }

    /**
     * Loggt das Ergebnis in TelemetryDeck (privacy-first, kein User-Identifier).
     * Sichtbar im TelemetryDeck-Dashboard unter "integrity.check".
     *
     * Nur in Release-Builds: Debug/Emulator-Daten würden die Monitoring-Statistik verfälschen.
     */
    private fun logVerdict(verdict: DeviceTrustLevel) {
        val verdictLabel = verdict.name // "TRUSTED", "UNTRUSTED", "UNKNOWN"
        if (!BuildConfig.DEBUG) {
            TelemetryDeck.signal(
                "integrity.check",
                mapOf(
                    "verdict" to verdictLabel,
                    "enforcement" to ENFORCEMENT_ENABLED.toString()
                )
            )
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Integrity verdict: $verdictLabel (enforcement=$ENFORCEMENT_ENABLED, telemetry skipped in debug)")
        }
    }
}
