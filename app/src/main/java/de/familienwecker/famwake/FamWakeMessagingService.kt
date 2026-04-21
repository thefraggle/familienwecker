package de.familienwecker.famwake

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
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
         * Stellt sicher, dass der Token auch nach App-Neuinstallationen aktuell ist.
         */
        fun refreshAndSaveToken() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Log.w(TAG, "refreshAndSaveToken: kein eingeloggter User – abgebrochen")
                return
            }
            Log.d(TAG, "refreshAndSaveToken: Fordere FCM-Token an für UID=$uid")
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    Log.d(TAG, "FCM-Token erhalten (${token.take(20)}…) → speichere in Firestore")
                    saveTokenToFirestore(uid, token)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM-Token anfordern fehlgeschlagen: ${e.message}")
                }
        }

        /**
         * Beim Logout aufrufen: Token aus Firestore entfernen, damit keine
         * Pushes mehr an dieses Gerät gesendet werden.
         */
        fun deleteTokenOnLogout() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                val docId = sha256(token)
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("fcmTokens").document(docId)
                    .delete()
                    .addOnFailureListener { Log.w(TAG, "Token-Delete fehlgeschlagen: ${it.message}") }
            }
            // FCM-Token auf Gerät ungültig machen – nächster Server-Push gibt 404
            // → sendPushToUser cleanup-Code löscht den Eintrag dann serverseitig.
            FirebaseMessaging.getInstance().deleteToken()
                .addOnFailureListener { Log.w(TAG, "FCM deleteToken fehlgeschlagen: ${it.message}") }
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
                .addOnSuccessListener { Log.d(TAG, "Token erfolgreich in Firestore gespeichert (docId=$docId)") }
                .addOnFailureListener { Log.e(TAG, "Token-Save fehlgeschlagen: ${it.message}") }
        }

        private fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    /** Wird von Firebase aufgerufen wenn ein neuer Token generiert wird. */
    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveTokenToFirestore(uid, token)
    }

    /** Verarbeitet eingehende FCM-Datennachrichten und zeigt Notifications an. */
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return

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
