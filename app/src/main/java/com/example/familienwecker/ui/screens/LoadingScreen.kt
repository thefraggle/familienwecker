package com.example.familienwecker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.familienwecker.ui.viewmodel.AuthViewModel
import com.example.familienwecker.ui.viewmodel.FamilyViewModel

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

    LaunchedEffect(authState, isRestoring, familyId) {
        if (isRestoring) return@LaunchedEffect

        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                if (familyId != null) {
                    onNavigateToMain()
                } else {
                    onNavigateToSetup()
                }
            }
            is AuthViewModel.AuthState.Error, AuthViewModel.AuthState.Idle, AuthViewModel.AuthState.PasswordResetSuccess -> {
                onNavigateToLogin()
            }
            AuthViewModel.AuthState.Loading -> {
                // Wait in loading state
            }
            AuthViewModel.AuthState.AwaitingEmailVerification -> {
                // Show login screen which handles the verification pending UI
                onNavigateToLogin()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
