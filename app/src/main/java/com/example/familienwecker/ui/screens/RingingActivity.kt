package com.example.familienwecker.ui.screens

import android.net.Uri
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
                    RingingScreen(memberName = memberName) {
                        stopRingtoneAndFinish()
                    }
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

    private fun playRingtone() {
        val prefsRepo = PreferencesRepository(this)
        val savedUriString = prefsRepo.alarmSoundUri.value
        
        val alarmUri = if (savedUriString != null) {
            Uri.parse(savedUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }
        
        mediaPlayer = MediaPlayer.create(this, alarmUri)
        
        // Fallback falls die ausgewählte URI auf dem Gerät nicht abspielbar ist
        if (mediaPlayer == null) {
             val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
             mediaPlayer = MediaPlayer.create(this, fallbackUri)
        }
        
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun stopRingtoneAndFinish() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

@Composable
fun RingingScreen(memberName: String, onStopClicked: () -> Unit) {
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
        
        Button(
            onClick = onStopClicked,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.size(width = 200.dp, height = 64.dp)
        ) {
            Text(stringResource(R.string.ringing_stop), style = MaterialTheme.typography.titleMedium)
        }
    }
}
