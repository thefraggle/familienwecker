package de.familienwecker.famwake.ui.screens

import android.net.Uri
import android.media.AudioAttributes
import de.familienwecker.famwake.FamWakeApplication
import de.familienwecker.famwake.data.PreferencesRepository
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import de.familienwecker.famwake.ui.theme.FamilienweckerTheme
import androidx.compose.ui.res.stringResource
import de.familienwecker.famwake.R

class RingingActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val memberId = intent.getStringExtra("MEMBER_ID") ?: ""
        val memberName = intent.getStringExtra("MEMBER_NAME") ?: "Jemand"

        // WICHTIG: Wenn die Activity startet, canceln wir die Notification.
        // Das stoppt den dortigen Fallback-Sound und verhindert Dopplungen.
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(memberId.hashCode())

        showOnLockScreenAndTurnScreenOn()
        playRingtone()

        val prefsRepo = (application as FamWakeApplication).preferencesRepository
        val alarmScheduler = de.familienwecker.famwake.alarm.AlarmScheduler(this)

        setContent {
            FamilienweckerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    RingingScreen(
                        memberName = memberName,
                        onStopClicked = {
                            // Snooze-Status löschen, damit der Banner auf MainScreen verschwindet
                            prefsRepo.setSnoozeUntil(null)
                            // Snooze-Alarm-Slot aus dem System entfernen
                            alarmScheduler.cancelWakeUp(memberId, isSnooze = true)
                            stopRingtoneAndFinish()
                        },
                        onSnoozeClicked = {
                            val snoozeTime = java.time.LocalDateTime.now().plusMinutes(5)
                            prefsRepo.setSnoozeUntil(snoozeTime)
                            alarmScheduler.scheduleWakeUp(
                                wakeUpTime = snoozeTime,
                                memberId = memberId,
                                memberName = memberName,
                                soundUri = prefsRepo.alarmSoundUri.value,
                                isSnooze = true
                            )
                            stopRingtoneAndFinish()
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
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        // Zusätzliche Window-Flags für OEM-Kompatibilität (Samsung, Xiaomi etc.)
        // Auch auf neueren API-Levels nötig, da manche Hersteller die neuen APIs ignorieren
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
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
        val prefsRepo = (application as FamWakeApplication).preferencesRepository
        val savedUriString = prefsRepo.alarmSoundUri.value

        // Versuche zunächst den gespeicherten Ton, dann System-Alarm, dann System-Ringtone
        val uriChain = listOfNotNull(
            savedUriString?.let { Uri.parse(it) },
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

    private fun stopRingtoneAndFinish() {
        try {
            mediaPlayer?.stop()
        } catch (_: IllegalStateException) {}
        mediaPlayer?.release()
        mediaPlayer = null
        finish()
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
fun RingingScreen(memberName: String, onStopClicked: () -> Unit, onSnoozeClicked: () -> Unit) {
    // Lottie-Animation
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.wakeup))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // Gradient: Dunkellila oben → Warmes Pfirsich unten
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2D1B69), // Dunkellila
            Color(0xFF6B3FA0), // Mittleres Violett
            Color(0xFFFFB347)  // Warmes Pfirsich
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
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

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
                    text = stringResource(R.string.ringing_message),
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
                // Snooze (transparent/gläsern)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(30.dp)
                        )
                        .clickable(onClick = onSnoozeClicked),
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
                            text = stringResource(R.string.ringing_snooze),
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
                        .clip(RoundedCornerShape(30.dp))
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
                            tint = Color(0xFF2D1B69),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ringing_stop),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D1B69)
                        )
                    }
                }
            }
        }
    }
}
