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

/**
 * Empfängt FCM-Nachrichten und verwaltet den Token-Lifecycle.
 * Token wird bei onNewToken() in users/{uid}/fcmTokens gespeichert,
 * damit Cloud Functions Pushes an dieses Gerät senden können.
 */
class FamWakeMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FamWakeMessaging"

        /**
         * Beim App-Start oder nach Login aufrufen: aktuellen Token holen und speichern.
         * Stellt sicher, dass der Token auch nach App-Neuinstallationen aktuell ist.
         */
        fun refreshAndSaveToken() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                saveTokenToFirestore(uid, token)
            }
        }

        /**
         * Beim Logout aufrufen: Token aus Firestore entfernen, damit keine
         * Pushes mehr an dieses Gerät gesendet werden.
         */
        fun deleteTokenOnLogout() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                // Firestore-Entry entfernen (ggf. offline gequeuet, daher auch FCM-seitig löschen)
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("fcmTokens").document(token)
                    .delete()
                    .addOnFailureListener { Log.w(TAG, "Token-Delete fehlgeschlagen: ${it.message}") }
            }
            // FCM-Token auf Gerät ungültig machen – nächster Server-Push gibt 404
            // → sendPushToUser cleanup-Code löscht den Eintrag dann serverseitig.
            // Löst das Problem: Offline-Logout queued Firestore-Delete, der nach Auth-Expiry scheitert.
            FirebaseMessaging.getInstance().deleteToken()
                .addOnFailureListener { Log.w(TAG, "FCM deleteToken fehlgeschlagen: ${it.message}") }
        }

        private fun saveTokenToFirestore(uid: String, token: String) {
            val tokenData = mapOf(
                "platform"    to "android",
                "lastRefresh" to com.google.firebase.Timestamp.now()
            )
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("fcmTokens").document(token)
                .set(tokenData, SetOptions.merge())
                .addOnFailureListener { Log.w(TAG, "Token-Save fehlgeschlagen: ${it.message}") }
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
        showNotification(title, body, channel)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
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
        // Eindeutige ID via Timestamp verhindert, dass Notifications sich gegenseitig überschreiben
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
