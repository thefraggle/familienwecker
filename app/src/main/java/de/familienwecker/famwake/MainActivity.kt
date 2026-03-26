package de.familienwecker.famwake

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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.familienwecker.famwake.ui.screens.AddMemberScreen
import de.familienwecker.famwake.ui.screens.FamilySetupScreen
import de.familienwecker.famwake.ui.screens.LoadingScreen
import de.familienwecker.famwake.ui.screens.LoginScreen
import de.familienwecker.famwake.ui.screens.MainScreen
import de.familienwecker.famwake.ui.screens.OnboardingScreen
import de.familienwecker.famwake.ui.screens.SettingsScreen
import de.familienwecker.famwake.ui.screens.FeedbackScreen
import de.familienwecker.famwake.ui.theme.FamilienweckerTheme
import de.familienwecker.famwake.ui.viewmodel.AuthViewModel
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.ui.viewmodel.*
import de.familienwecker.famwake.ui.Routes
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private val familyViewModel: FamilyViewModel by viewModels {
        de.familienwecker.famwake.ui.viewmodel.FamilyViewModelFactory(application)
    }

    private val authViewModel: AuthViewModel by viewModels {
        de.familienwecker.famwake.ui.viewmodel.AuthViewModelFactory(application)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* Berechtigung wird im ViewModel verarbeitet */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        checkFullScreenIntentPermission()
        handleDeepLink(intent, familyViewModel)

        enableEdgeToEdge()
        setContent {
            val themePref by familyViewModel.themePreference.collectAsStateWithLifecycle()
            val darkTheme = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            FamilienweckerTheme(darkTheme = darkTheme) {
                FamilienweckerApp(familyViewModel, authViewModel)
            }
        }
    }
 
    override fun onResume() {
        super.onResume()
        familyViewModel.triggerRefresh()
    }
 
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent, familyViewModel)
    }

    private fun handleDeepLink(intent: Intent?, viewModel: FamilyViewModel) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "https" && data.host == "familienwecker.de" && data.path?.startsWith("/join/") == true) {
            val code = data.lastPathSegment
            if (!code.isNullOrBlank() && code != "join") {
                val sanitized = code.filter { it.isLetterOrDigit() }.uppercase().take(6)
                viewModel.setPendingJoinCode(sanitized)
            }
        }
    }

    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission_fullscreen_title))
                    .setMessage(getString(R.string.permission_fullscreen_message))
                    .setPositiveButton(getString(R.string.permission_fullscreen_open)) { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            intent.data = "package:$packageName".toUri()
                            startActivity(intent)
                        } catch (e: Exception) {
                            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                android.util.Log.w("MainActivity", "FullScreenIntent settings not available", e)
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }
}

@Composable
fun FamilienweckerApp(familyViewModel: FamilyViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    val currentLanguage by familyViewModel.language.collectAsStateWithLifecycle()

    LaunchedEffect(currentLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(currentLanguage)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOADING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOADING) {
                LoadingScreen(
                    authViewModel = authViewModel,
                    familyViewModel = familyViewModel,
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                        }
                    },
                    onNavigateToSetup = {
                        navController.navigate(Routes.SETUP) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.ONBOARDING) {
                val language by familyViewModel.language.collectAsStateWithLifecycle()
                OnboardingScreen(
                    language   = language,
                    onFinished = {
                        familyViewModel.setOnboardingCompleted(true)
                        val dest = if (familyViewModel.familyId.value != null) Routes.MAIN else Routes.SETUP
                        navController.navigate(dest) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    familyViewModel = familyViewModel,
                    onLoginSuccess = {
                        navController.navigate(Routes.LOADING) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.SETUP) {
                FamilySetupScreen(
                    viewModel = familyViewModel,
                    onSetupComplete = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.SETUP) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        authViewModel.logout()
                        familyViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.MAIN) {
                MainScreen(
                    viewModel = familyViewModel,
                    onNavigateToAddMember = { navController.navigate(Routes.ADD_MEMBER) },
                    onNavigateToEditMember = { id -> navController.navigate(Routes.editMember(id)) },
                    onNavigateToSettings = { 
                        navController.navigate(Routes.SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        authViewModel.logout()
                        familyViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onLeaveFamily = {
                        familyViewModel.leaveFamily { success ->
                            if (success) {
                                navController.navigate(Routes.SETUP) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                )
            }
            composable(Routes.ADD_MEMBER) {
                AddMemberScreen(
                    viewModel = familyViewModel,
                    memberId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.EDIT_MEMBER) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId")
                AddMemberScreen(
                    viewModel = familyViewModel,
                    memberId = memberId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = familyViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFeedback = { navController.navigate(Routes.FEEDBACK) },
                    onLogout = {
                        authViewModel.logout()
                        familyViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onLeaveFamily = {
                        familyViewModel.leaveFamily { success ->
                            if (success) {
                                navController.navigate(Routes.SETUP) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                    onStartOnboarding = {
                        familyViewModel.setOnboardingCompleted(false)
                        navController.navigate(Routes.ONBOARDING) {
                            // Pop SETTINGS so that we don't have MAIN -> SETTINGS -> MAIN after finishing tour
                            popUpTo(Routes.SETTINGS) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.FEEDBACK) {
                FeedbackScreen(
                    viewModel = familyViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}