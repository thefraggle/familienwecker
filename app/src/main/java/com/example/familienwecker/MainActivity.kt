package com.example.familienwecker

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.familienwecker.ui.screens.AddMemberScreen
import com.example.familienwecker.ui.screens.FamilySetupScreen
import com.example.familienwecker.ui.screens.LoadingScreen
import com.example.familienwecker.ui.screens.LoginScreen
import com.example.familienwecker.ui.screens.MainScreen
import com.example.familienwecker.ui.screens.SettingsScreen
import com.example.familienwecker.ui.theme.FamilienweckerTheme
import com.example.familienwecker.ui.viewmodel.AuthViewModel
import com.example.familienwecker.ui.viewmodel.FamilyViewModel

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Berechtigung geloggt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Frage Notifizierungsrechte unter Android 13+ an, damit FullScreenIntents feuern
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Android 14+ Full Screen Intent check
        checkFullScreenIntentPermission()

        enableEdgeToEdge()
        setContent {
            FamilienweckerTheme {
                FamilienweckerApp()
            }
        }
    }

    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback for some devices/versions
                }
            }
        }
    }
}

@Composable
fun FamilienweckerApp() {
    val navController = rememberNavController()
    val familyViewModel: FamilyViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    val currentLanguage by familyViewModel.language.collectAsState()

    LaunchedEffect(currentLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(currentLanguage)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "loading",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("loading") {
                LoadingScreen(
                    authViewModel = authViewModel,
                    familyViewModel = familyViewModel,
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("loading") { inclusive = true }
                        }
                    },
                    onNavigateToSetup = {
                        navController.navigate("setup") {
                            popUpTo("loading") { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate("main") {
                            popUpTo("loading") { inclusive = true }
                        }
                    }
                )
            }
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate("loading") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("setup") {
                FamilySetupScreen(
                    viewModel = familyViewModel,
                    onSetupComplete = {
                        navController.navigate("main") {
                            popUpTo("setup") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        authViewModel.logout()
                        familyViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("main") {
                MainScreen(
                    viewModel = familyViewModel,
                    onNavigateToAddMember = { navController.navigate("addMember") },
                    onNavigateToEditMember = { id -> navController.navigate("editMember/$id") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("addMember") {
                AddMemberScreen(
                    viewModel = familyViewModel,
                    memberId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("editMember/{memberId}") { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId")
                AddMemberScreen(
                    viewModel = familyViewModel,
                    memberId = memberId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = familyViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.logout()
                        familyViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onLeaveFamily = {
                        familyViewModel.leaveFamily()
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}