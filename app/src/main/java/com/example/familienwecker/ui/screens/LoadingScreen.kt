package com.example.familienwecker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
    val authState by authViewModel.authState.collectAsState()
    val familyId by familyViewModel.familyId.collectAsState()

    LaunchedEffect(authState, familyId) {
        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                if (familyId != null) {
                    onNavigateToMain()
                } else {
                    onNavigateToSetup()
                }
            }
            is AuthViewModel.AuthState.Error, AuthViewModel.AuthState.Idle -> {
                onNavigateToLogin()
            }
            AuthViewModel.AuthState.Loading -> {
                // Wait in loading state
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
