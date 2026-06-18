package de.familienwecker.famwake

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import de.familienwecker.famwake.BuildConfig
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.security.MessageDigest

/**
 * Empfängt FCM-Nachrichten und verwaltet den Token-Lifecycle.
 * Token wird bei onNewToken() in users/{uid}/fcmTokens gespeichert,
 * damit Cloud Functions Pushes an dieses Gerät senden können.
 */
class FamWakeMessagingService : FirebaseMessagingService() {

    private val lastNotifTimestamps = HashMap<String, Long>()

    companion object {
        private const val TAG = "FamWakeMessaging"

        /**
         * Beim App-Start oder nach Login aufrufen: aktuellen Token holen und speichern.
         * Prüft den Push-Toggle – bei deaktiviertem Push wird der Token aus Firestore gelöscht.
         */
        fun refreshAndSaveToken(context: Context? = null) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                if (BuildConfig.DEBUG) Log.w(TAG, "refreshAndSaveToken: kein eingeloggter User – abgebrochen")
                return
            }
            // Push-Toggle aus SharedPreferences lesen (synchron, DataStore evtl. nicht geladen).
            // Ohne expliziten Context den Application-Context verwenden.
            val appContext = context ?: try { FamWakeApplication.instance } catch (_: Exception) { null }
            val pushEnabled = appContext?.getSharedPreferences("famwake_push_prefs", Context.MODE_PRIVATE)
                ?.getBoolean("push_enabled", true) ?: true
            
            if (BuildConfig.DEBUG) Log.d(TAG, "refreshAndSaveToken: push=$pushEnabled, fordere Token an")
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (pushEnabled) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "FCM-Token erhalten → speichere in Firestore")
                        saveTokenToFirestore(uid, token)
                    } else {
                        if (BuildConfig.DEBUG) Log.d(TAG, "FCM-Token erhalten, Push OFF → lösche aus Firestore")
                        deleteTokenFromFirestore(uid, token)
                    }
                }
                .addOnFailureListener { e ->
                    if (BuildConfig.DEBUG) Log.e(TAG, "FCM-Token anfordern fehlgeschlagen: ${e.message}")
                }
        }

        /**
         * Löscht den FCM Token aus Firestore (Push-Toggle OFF).
         * Der lokale Token bleibt – bei Toggle-ON sofort wieder registrierbar.
         */
        fun deleteToken() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                deleteTokenFromFirestore(uid, token)
            }
        }

        /**
         * Beim Logout aufrufen: Token aus Firestore entfernen + lokalen Token löschen.
         */
        fun deleteTokenOnLogout() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                deleteTokenFromFirestore(uid, token)
            }
            // FCM-Token auf Gerät ungültig machen
            FirebaseMessaging.getInstance().deleteToken()
                .addOnFailureListener { if (BuildConfig.DEBUG) Log.w(TAG, "FCM deleteToken fehlgeschlagen: ${it.message}") }
        }

        private fun deleteTokenFromFirestore(uid: String, token: String) {
            val docId = sha256(token)
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("fcmTokens").document(docId)
                .delete()
                .addOnSuccessListener { if (BuildConfig.DEBUG) Log.d(TAG, "Token aus Firestore gelöscht") }
                .addOnFailureListener { if (BuildConfig.DEBUG) Log.w(TAG, "Token-Delete fehlgeschlagen: ${it.message}") }
        }

        private fun saveTokenToFirestore(uid: String, token: String) {
            // FCM-Tokens enthalten `:` – als Firestore-ID verboten.
            // SHA-256-Hash als sichere Dokument-ID; Token selbst als Feld.
            val docId = sha256(token)
            val tokenData = mapOf(
                "token"       to token,
                "platform"    to "android",
                "lastRefresh" to com.google.firebase.Timestamp.now()
            )
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("fcmTokens").document(docId)
                .set(tokenData, SetOptions.merge())
                .addOnSuccessListener { if (BuildConfig.DEBUG) Log.d(TAG, "Token erfolgreich in Firestore gespeichert") }
                .addOnFailureListener { if (BuildConfig.DEBUG) Log.e(TAG, "Token-Save fehlgeschlagen: ${it.message}") }
        }

        private fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    /** Wird von Firebase aufgerufen wenn ein neuer Token generiert wird. */
    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Push-Toggle prüfen – bei OFF keinen neuen Token in Firestore speichern
        val pushEnabled = getSharedPreferences("famwake_push_prefs", MODE_PRIVATE)
            .getBoolean("push_enabled", true)
        if (pushEnabled) {
            saveTokenToFirestore(uid, token)
        }
    }

    /** Verarbeitet eingehende FCM-Datennachrichten und zeigt Notifications an. */
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return

        // Push-Benachrichtigungen in der App deaktiviert → lautlos ignorieren.
        // SharedPreferences direkt lesen (synchron), weil DataStore bei Kaltstart
        // durch den FCM-Service noch nicht geladen sein kann (gibt Default=true zurück).
        val prefs = getSharedPreferences("famwake_push_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("push_enabled", true)) return

        // Client-Debounce: doppelte Pushes desselben Typs innerhalb 10s ignorieren
        val now = System.currentTimeMillis()
        val lastTime = lastNotifTimestamps[type] ?: 0L
        if (now - lastTime < 10_000) return
        lastNotifTimestamps[type] = now
        val channel = when (type) {
            "schedule_change" -> NotificationChannels.SCHEDULE_CHANGE
            "family_joined", "family_left" -> NotificationChannels.FAMILY_EVENTS
            else -> NotificationChannels.FAMILY_EVENTS
        }
        // Lokalisierung via String-Ressourcen – unabhängig vom Server-Text
        val (title, body) = when (type) {
            "schedule_change" ->
                getString(R.string.notif_schedule_changed_title) to
                getString(R.string.notif_schedule_changed_body)
            "family_joined" ->
                getString(R.string.notif_member_joined_title) to
                getString(R.string.notif_member_joined_body)
            "family_left" ->
                getString(R.string.notif_member_left_title) to
                getString(R.string.notif_member_left_body)
            else -> return
        }
        // Feste ID pro Typ: Falls doch ein Doppel-Push durchkommt, überschreibt er sich selbst
        val notifId = when (type) {
            "schedule_change" -> 1001
            "family_joined"   -> 1002
            "family_left"     -> 1003
            else              -> 1000
        }
        showNotification(title, body, channel, notifId)
    }

    private fun showNotification(title: String, body: String, channelId: String, notifId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }
}
