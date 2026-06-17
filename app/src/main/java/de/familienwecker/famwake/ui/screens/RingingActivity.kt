package de.familienwecker.famwake.ui.screens

import android.net.Uri
import android.media.AudioAttributes
import de.familienwecker.famwake.FamWakeApplication
import de.familienwecker.famwake.data.AppSettings
import de.familienwecker.famwake.data.FirebaseRepository
import de.familienwecker.famwake.model.SnoozeConfig
import de.familienwecker.famwake.model.toKmpLocalDateTime
import de.familienwecker.famwake.MainActivity
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import de.familienwecker.famwake.ui.theme.FamilienweckerTheme
import de.familienwecker.famwake.ui.theme.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import de.familienwecker.famwake.R
import androidx.core.net.toUri
import com.telemetrydeck.sdk.TelemetryDeck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RingingActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val memberId = intent.getStringExtra("MEMBER_ID") ?: ""
        val memberName = intent.getStringExtra("MEMBER_NAME") ?: getString(R.string.default_someone)

        // WICHTIG: Wenn die Activity startet, canceln wir die Notification.
        // Das stoppt den dortigen Fallback-Sound und verhindert Dopplungen.
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Notification canceln – gleiche ID-Berechnung wie in AlarmReceiver.notify()
        notificationManager.cancel(memberId.hashCode().and(0x7fffffff))

        showOnLockScreenAndTurnScreenOn()
        playRingtone()
        // Tracking: Wecker wurde tatsächlich ausgelöst (getrennt von Snooze tracken)
        TelemetryDeck.signal("alarm.triggered")

        val appSettings = (application as FamWakeApplication).appSettings
        val alarmScheduler = de.familienwecker.famwake.alarm.AlarmScheduler(this)
        val currentSnoozeCount = appSettings.snoozeCount.value

        setContent {
            FamilienweckerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    RingingScreen(
                        memberName = memberName,
                        snoozeCount = currentSnoozeCount,
                        onStopClicked = {
                            // Snooze-Status löschen, damit der Banner auf MainScreen verschwindet
                            appSettings.setSnoozeUntil(null)
                            appSettings.setSnoozeCount(0)
                            // isAwakeToday setzen – verhindert, dass applyAlarms() sofort
                            // einen neuen regulären Alarm für heute plant (Loop-Prevention)
                            appSettings.setAwakeToday(true)
                            // BEIDE Alarm-Slots aus dem System entfernen
                            alarmScheduler.cancelWakeUp(memberId, isSnooze = true)
                            alarmScheduler.cancelWakeUp(memberId, isSnooze = false)

                            // Firestore: Snooze-State zurücksetzen (best-effort)
                            val familyId = appSettings.familyId.value
                            if (familyId != null) {
                                MainScope().launch {
                                    try {
                                        FirebaseRepository().updateMemberSnoozeState(familyId, memberId, null, 0)
                                    } catch (_: CancellationException) { throw CancellationException() }
                                    catch (_: Exception) { /* best-effort */ }
                                }
                            }

                            // Tracking: Nutzer hat den Alarm aktiv abgebrochen (nicht durch Snooze)
                            TelemetryDeck.signal("alarm.dismissed")
                            // Zur Haupt-App wechseln (ohne Begrüßung)
                            val intent = android.content.Intent(this@RingingActivity, MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            
                            stopRingtoneAndOpenApp()
                        },
                        onSnoozeClicked = {
                            val count = appSettings.snoozeCount.value
                            if (count >= SnoozeConfig.MAX_SNOOZE_COUNT) {
                                Toast.makeText(
                                    this@RingingActivity,
                                    getString(R.string.snooze_max_reached),
                                    Toast.LENGTH_LONG
                                ).show()
                                return@RingingScreen
                            }

                            val newCount = count + 1
                            val snoozeMinutes = SnoozeConfig.SNOOZE_DURATION_MINUTES
                            val snoozeTime = java.time.LocalDateTime.now().plusMinutes(snoozeMinutes.toLong())

                            appSettings.setSnoozeUntil(snoozeTime.toKmpLocalDateTime())
                            appSettings.setSnoozeCount(newCount)

                            alarmScheduler.scheduleWakeUp(
                                wakeUpTime = snoozeTime.toKmpLocalDateTime(),
                                memberId = memberId,
                                memberName = memberName,
                                soundUri = appSettings.alarmSoundUri.value,
                                isSnooze = true
                            )

                            // Firestore-Sync (best-effort, non-blocking)
                            val familyId = appSettings.familyId.value
                            if (familyId != null) {
                                MainScope().launch {
                                    try {
                                        FirebaseRepository().updateMemberSnoozeState(
                                            familyId, memberId,
                                            snoozeTime.toKmpLocalDateTime(), newCount
                                        )
                                    } catch (_: CancellationException) { throw CancellationException() }
                                    catch (_: Exception) { /* best-effort */ }
                                }
                            }

                            TelemetryDeck.signal("alarm.snoozed", mapOf("snoozeCount" to newCount.toString()))
                            stopRingtoneAndLock()
                        }
                    )
                }
            }
        }
    }

    private fun showOnLockScreenAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Kein requestDismissKeyguard: Wecker soll ÜBER dem Lockscreen angezeigt werden,
            // ohne PIN/Fingerprint-Abfrage. User muss Alarm stoppen können ohne zu entsperren.
        }
        // Zusätzliche Window-Flags für OEM-Kompatibilität (Samsung, Xiaomi etc.)
        // Auch auf neueren API-Levels nötig, da manche Hersteller die neuen APIs ignorieren
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            // Kein FLAG_DISMISS_KEYGUARD: würde PIN/Fingerprint-Dialog triggern
        )
    }

    private val alarmAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun buildMediaPlayer(uri: Uri): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setAudioAttributes(alarmAudioAttributes)
                setDataSource(this@RingingActivity, uri)
                prepare()
                isLooping = true
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun playRingtone() {
        val appSettings = (application as FamWakeApplication).appSettings
        val savedUriString = appSettings.alarmSoundUri.value

        // Versuche zunächst den gespeicherten Ton, dann System-Alarm, dann System-Ringtone
        val uriChain = listOfNotNull(
            savedUriString?.let { it.toUri() },
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )

        for (uri in uriChain) {
            val player = buildMediaPlayer(uri)
            if (player != null) {
                mediaPlayer = player
                player.start()
                return
            }
        }
        // Wenn alle Versuche scheitern, klingelt die App lautlos (besser als Crash)
    }

    // Stop: Sound aus, Flags behalten damit MainActivity sichtbar wird
    private fun stopRingtoneAndOpenApp() {
        try {
            val appSettings = (application as FamWakeApplication).appSettings
            appSettings.setLastAlarmTime(System.currentTimeMillis())
            mediaPlayer?.stop()
        } catch (_: IllegalStateException) {}
        mediaPlayer?.release()
        mediaPlayer = null
        // KEINE Flags löschen – Keyguard bleibt dismissed, damit die App sichtbar ist
        finish()
    }

    // Snooze: Sound aus, Flags löschen damit Handy gesperrt bleibt
    private fun stopRingtoneAndLock() {
        try {
            val appSettings = (application as FamWakeApplication).appSettings
            appSettings.setLastAlarmTime(System.currentTimeMillis())
            mediaPlayer?.stop()
        } catch (_: IllegalStateException) {}
        mediaPlayer?.release()
        mediaPlayer = null
        
        // Window-Flags zurücksetzen → Handy kehrt zum Sperrbildschirm zurück
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        }
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.stop()
        } catch (_: IllegalStateException) {
            // MediaPlayer war evtl. noch nicht gestartet
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@Composable
fun RingingScreen(
    memberName: String,
    snoozeCount: Int,
    onStopClicked: () -> Unit,
    onSnoozeClicked: () -> Unit
) {
    val snoozeMaxReached = snoozeCount >= SnoozeConfig.MAX_SNOOZE_COUNT

    // Lottie-Animation
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.wakeup))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // Zufälligen Grußtext einmalig beim Start wählen (stabil über Recompositions)
    val messages = stringArrayResource(R.array.ringing_messages)
    val randomMessage = remember { messages.random() }

    // Gradient: Dunkellila oben → Warmes Pfirsich unten
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            RingingPurpleDark,
            RingingPurpleMed,
            RingingPeach
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Panda Lottie-Animation
            LottieAnimation(
                composition = composition,
                progress = { lottieProgress },
                modifier = Modifier
                    .size(280.dp)
            )

            // Texte
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ringing_wake_up, memberName),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Text(
                    text = randomMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Buttons mit Glassmorphism
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Snooze (transparent/gläsern) – disabled wenn Max erreicht
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = if (snoozeMaxReached) 0.08f else 0.18f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (snoozeMaxReached) 0.15f else 0.30f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .then(
                            if (snoozeMaxReached) Modifier.alpha(0.5f)
                            else Modifier.clickable(onClick = onSnoozeClicked)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (snoozeMaxReached) {
                                stringResource(R.string.snooze_max_reached)
                            } else {
                                stringResource(
                                    R.string.snooze_counter,
                                    snoozeCount + 1,
                                    SnoozeConfig.MAX_SNOOZE_COUNT
                                )
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Stop (solider weißer Hintergrund)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .clickable(onClick = onStopClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOff,
                            contentDescription = null,
                            tint = RingingPurpleDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ringing_stop),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RingingPurpleDark
                        )
                    }
                }
            }
        }
    }
}
