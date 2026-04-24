package de.familienwecker.famwake.util

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.firebase.functions.FirebaseFunctions
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
 *
 * v1.7.10: Server-side Verification via Cloud Function statt lokalem JWT-Decode.
 * Gibt erstmals echte TRUSTED-Werte zurück (lokaler Decode lieferte immer UNKNOWN).
 */
class DeviceTrustManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceTrustManager"
        private const val FIREBASE_REGION = "europe-west3"

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
            val token = withTimeoutOrNull(5000L) {
                val manager = IntegrityManagerFactory.create(context)
                val request = IntegrityTokenRequest.builder()
                    .setNonce(generateNonce())
                    .build()
                manager.requestIntegrityToken(request).await().token()
            } ?: return DeviceTrustLevel.UNKNOWN // Timeout → fail-open

            // Token serverseitig via Cloud Function verifizieren.
            // Lokales JWT-Decoding liefert immer UNKNOWN, da das Token signiert ist
            // und die Signatur nur mit dem Google-Schlüssel serverseitig prüfbar ist.
            verifyTokenServerSide(token)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Play Integrity check failed: ${e.message}")
            }
            DeviceTrustLevel.UNKNOWN // Jeder Fehler → fail-open
        }
    }

    /**
     * Sendet das Integrity-Token an die Cloud Function `verifyIntegrityToken`,
     * die es serverseitig via Google Play Integrity API verifiziert.
     * Gibt UNKNOWN zurück wenn die Cloud Function nicht erreichbar ist (fail-open).
     */
    private suspend fun verifyTokenServerSide(token: String): DeviceTrustLevel {
        return try {
            val functions = FirebaseFunctions.getInstance(FIREBASE_REGION)
            val data = hashMapOf("token" to token)

            val result = withTimeoutOrNull(8000L) {
                functions.getHttpsCallable("verifyIntegrityToken")
                    .call(data)
                    .await()
            } ?: run {
                if (BuildConfig.DEBUG) Log.w(TAG, "Cloud Function timeout → UNKNOWN")
                return DeviceTrustLevel.UNKNOWN
            }

            @Suppress("UNCHECKED_CAST")
            val resultMap = result.data as? Map<String, Any>
            val trusted = resultMap?.get("trusted") as? Boolean

            when (trusted) {
                true  -> DeviceTrustLevel.TRUSTED
                false -> DeviceTrustLevel.UNTRUSTED
                null  -> DeviceTrustLevel.UNKNOWN // API_ERROR oder kein Ergebnis
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Server verification failed: ${e.message}")
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

