package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.familienwecker.famwake.ui.viewmodel.AuthViewModel
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoadingScreen(
    authViewModel: AuthViewModel,
    familyViewModel: FamilyViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isRestoring by authViewModel.isRestoringFamily.collectAsStateWithLifecycle()
    val familyId by familyViewModel.familyId.collectAsStateWithLifecycle()
    val pendingJoinCode by familyViewModel.pendingJoinCode.collectAsStateWithLifecycle()
    val onboardingCompleted by familyViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    LaunchedEffect(authState, isRestoring, familyId, pendingJoinCode, onboardingCompleted) {
        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
            android.util.Log.d("LoadingScreen", "State: auth=$authState, isRestoring=$isRestoring, familyId=$familyId")
        }

        // Variable zur Vermeidung von Doppel-Navigation in diesem Effekt-Lauf
        var navigationTriggered = false

        // Notfall-Timeout: Wenn nach 2 Sekunden immer noch geladen wird, aber wir eine familyId haben, gehen wir direkt rein.
        val timeoutJob = launch {
            delay(2000)
            if (!navigationTriggered && authState is AuthViewModel.AuthState.Authenticated && familyId != null && isRestoring) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) android.util.Log.w("LoadingScreen", "Timeout reached, force navigating to Main")
                navigationTriggered = true
                onNavigateToMain()
            }
        }

        if (isRestoring) return@LaunchedEffect

        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                timeoutJob.cancel()
                if (navigationTriggered) return@LaunchedEffect
                
                if (!onboardingCompleted) {
                    navigationTriggered = true
                    onNavigateToOnboarding()
                } else if (familyId != null) {
                    navigationTriggered = true
                    onNavigateToMain()
                } else if (pendingJoinCode != null) {
                    // Netzwerk-Check vor automatischem Beitritt
                    if (de.familienwecker.famwake.util.NetworkUtils.isOnline(context)) {
                        familyViewModel.handlePendingJoin { success ->
                            if (success) {
                                onNavigateToMain()
                            } else {
                                onNavigateToSetup()
                            }
                        }
                    } else {
                        // Offline: Beitreten unmöglich, gehe zum Setup (ViewModel zeigt Fehler an)
                        familyViewModel.setError(de.familienwecker.famwake.ui.util.UiText.StringResource(de.familienwecker.famwake.R.string.error_sync_failed, context.getString(de.familienwecker.famwake.R.string.error_offline)))
                        onNavigateToSetup()
                    }
                } else {
                    onNavigateToSetup()
                }
            }
            is AuthViewModel.AuthState.Error, AuthViewModel.AuthState.Idle, AuthViewModel.AuthState.PasswordResetSuccess -> {
                timeoutJob.cancel()
                onNavigateToLogin()
            }
            AuthViewModel.AuthState.Loading -> { }
            AuthViewModel.AuthState.AwaitingEmailVerification -> {
                timeoutJob.cancel()
                onNavigateToLogin()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
