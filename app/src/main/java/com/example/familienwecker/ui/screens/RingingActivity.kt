package com.example.familienwecker.ui.screens

import android.net.Uri
import android.media.AudioAttributes
import com.example.familienwecker.data.PreferencesRepository
import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.familienwecker.ui.theme.FamilienweckerTheme
import androidx.compose.ui.res.stringResource
import com.example.familienwecker.R

class RingingActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnLockScreenAndTurnScreenOn()

        val memberName = intent.getStringExtra("MEMBER_NAME") ?: "Jemand"
        playRingtone()

        setContent {
            FamilienweckerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val familyViewModel = com.example.familienwecker.ui.viewmodel.FamilyViewModel(application)
                    val memberId = intent.getStringExtra("MEMBER_ID") ?: ""
                    
                    RingingScreen(
                        memberName = memberName,
                        onStopClicked = { stopRingtoneAndFinish() },
                        onSnoozeClicked = {
                            familyViewModel.snooze(memberId, memberName)
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
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
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
        val prefsRepo = PreferencesRepository(this)
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ringing_wake_up, memberName),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.ringing_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSnoozeClicked,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text(stringResource(R.string.ringing_snooze), style = MaterialTheme.typography.titleMedium)
            }
            
            Button(
                onClick = onStopClicked,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text(stringResource(R.string.ringing_stop), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
