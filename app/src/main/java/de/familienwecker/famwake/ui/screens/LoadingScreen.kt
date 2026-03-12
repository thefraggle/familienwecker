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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoadingScreen(
    authViewModel: AuthViewModel,
    familyViewModel: FamilyViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isRestoring by authViewModel.isRestoringFamily.collectAsStateWithLifecycle()
    val familyId by familyViewModel.familyId.collectAsStateWithLifecycle()
    val pendingJoinCode by familyViewModel.pendingJoinCode.collectAsStateWithLifecycle()

    LaunchedEffect(authState, isRestoring, familyId, pendingJoinCode) {
        // Notfall-Timeout: Wenn nach 2 Sekunden immer noch geladen wird, aber wir eine familyId haben, gehen wir direkt rein.
        val timeoutJob = launch {
            delay(2000)
            if (authState is AuthViewModel.AuthState.Authenticated && familyId != null && isRestoring) {
                onNavigateToMain()
            }
        }

        if (isRestoring) return@LaunchedEffect

        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                timeoutJob.cancel()
                if (familyId != null) {
                    onNavigateToMain()
                } else if (pendingJoinCode != null) {
                    // Kein Konflikt, direktes Joinen zulässig
                    familyViewModel.handlePendingJoin { success ->
                        if (success) onNavigateToMain() else onNavigateToSetup()
                    }
                } else {
                    onNavigateToSetup()
                }
            }
            is AuthViewModel.AuthState.Error, AuthViewModel.AuthState.Idle, AuthViewModel.AuthState.PasswordResetSuccess -> {
                timeoutJob.cancel()
                onNavigateToLogin()
            }
            AuthViewModel.AuthState.Loading -> {
                // Wait in loading state
            }
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
