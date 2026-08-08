package de.familienwecker.famwake

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.res.Configuration
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
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
import com.telemetrydeck.sdk.TelemetryDeck

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
        
        // Erkennung aus adb-Intent (direkter Launch ohne Test-Runner)
        android.util.Log.d("FamWakeDebug", "Intent: " + intent.toString())
        intent.extras?.keySet()?.forEach { key ->
            android.util.Log.d("FamWakeDebug", "  Extra $key = ${intent.extras?.get(key)}")
        }
        if (intent.getBooleanExtra("screenshot_mode", false) || intent.getStringExtra("screenshot_mode") == "true") {
            FamWakeApplication.isScreenshotMode = true
            android.util.Log.d("FamWakeDebug", "Screenshot mode enabled via intent extra!")
        }
        if (FamWakeApplication.isScreenshotMode) {
            setupMockDataForScreenshots()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !FamWakeApplication.isScreenshotMode) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleDeepLink(intent, familyViewModel, authViewModel)

        // Edge-to-Edge muss vor setContent() aufgerufen werden, damit AppCompat
        // keine veralteten setStatusBarColor/setNavigationBarColor-Aufrufe absetzt
        enableEdgeToEdge()
        // Tracking: App wurde kalt gestartet – gibt grundlegende Nutzungsfrequenz wieder
        TelemetryDeck.signal("app.launched")

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
 
    private var lastRefreshTime = 0L

    override fun onResume() {
        super.onResume()
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastRefreshTime > 30_000L) {
            familyViewModel.triggerRefresh()
            lastRefreshTime = now
        }
    }
 
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent, familyViewModel, authViewModel)
    }

    private fun handleDeepLink(intent: Intent?, familyViewModel: FamilyViewModel, authViewModel: AuthViewModel) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "https" && (data.host == "familienwecker.de" || data.host == "www.familienwecker.de")) {
            if (data.path?.startsWith("/join/") == true) {
                val code = data.lastPathSegment
                if (!code.isNullOrBlank() && code != "join") {
                    val sanitized = code.filter { it.isLetterOrDigit() }.uppercase().take(6)
                    familyViewModel.setPendingJoinCode(sanitized)
                }
            } else if (data.path?.contains("/verify-email") == true) {
                val oobCode = data.getQueryParameter("oobCode")
                if (!oobCode.isNullOrBlank()) {
                    authViewModel.applyActionCode(oobCode)
                }
            }
        }
    }

    private fun setupMockDataForScreenshots() {
        val appSettings = (application as FamWakeApplication).appSettings
        val memberRepository = (application as FamWakeApplication).memberRepository
        
        appSettings.clearAll()
        appSettings.setAlarmEnabled(true)
        appSettings.setOnboardingCompleted(true)
        appSettings.setLocalOnlyFamily(true)
        appSettings.setFamilyId("MOCK_FAMILY_ID")
        appSettings.setJoinCode("FW-982-XYZ")
        appSettings.setTooltipsEnabled(false)
        
        val lang = intent.getStringExtra("lang") ?: java.util.Locale.getDefault().language
        appSettings.setLanguage(lang)
        var fatherName = "Papa"
        var motherName = "Mama"
        var childName = "Paul"
        var familyName = "Familie Müller"
        
        if (lang == "en") {
            fatherName = "Dad"
            motherName = "Mom"
            childName = "Alex"
            familyName = "The Millers"
        } else if (lang == "no" || lang == "nb") {
            fatherName = "Pappa"
            motherName = "Mamma"
            childName = "Jonas"
            familyName = "Familien"
        } else if (lang == "da") {
            fatherName = "Far"
            motherName = "Mor"
            childName = "Lucas"
            familyName = "Familien"
        } else if (lang == "nl") {
            fatherName = "Papa"
            motherName = "Mama"
            childName = "Daan"
            familyName = "Familie"
        } else if (lang == "fr") {
            fatherName = "Papa"
            motherName = "Maman"
            childName = "Lucas"
            familyName = "Famille"
        } else if (lang == "es") {
            fatherName = "Papá"
            motherName = "Mamá"
            childName = "Mateo"
            familyName = "Familia"
        } else if (lang == "it") {
            fatherName = "Papà"
            motherName = "Mamma"
            childName = "Leonardo"
            familyName = "Famiglia"
        }
        
        appSettings.setFamilyName(familyName)
        val dadId = "mock_dad"
        appSettings.setMyMemberId(dadId)
        appSettings.setMyMemberName(fatherName)
        
        fun makeDayProfiles(earliest: kotlinx.datetime.LocalTime, latest: kotlinx.datetime.LocalTime, bathroom: Long, leave: kotlinx.datetime.LocalTime?): Map<Int, de.familienwecker.famwake.model.DayProfile> {
            val profiles = mutableMapOf<Int, de.familienwecker.famwake.model.DayProfile>()
            for (day in 1..7) {
                profiles[day] = de.familienwecker.famwake.model.DayProfile(
                    isActive = true,
                    earliestWakeUp = earliest,
                    latestWakeUp = latest,
                    bathroomDurationMinutes = bathroom,
                    wantsBreakfast = true,
                    leaveHomeTime = leave,
                    isSimpleMode = false
                )
            }
            return profiles
        }
        
        val earliestWakeDad = kotlinx.datetime.LocalTime(6, 0)
        val latestWakeDad = kotlinx.datetime.LocalTime(7, 15)
        val leaveDad = kotlinx.datetime.LocalTime(8, 0)
        val dadProfiles = makeDayProfiles(earliest = earliestWakeDad, latest = latestWakeDad, bathroom = 15, leave = leaveDad)
        
        val earliestWakeMom = kotlinx.datetime.LocalTime(6, 0)
        val latestWakeMom = kotlinx.datetime.LocalTime(7, 30)
        val leaveMom = kotlinx.datetime.LocalTime(8, 15)
        val momProfiles = makeDayProfiles(earliest = earliestWakeMom, latest = latestWakeMom, bathroom = 20, leave = leaveMom)
        
        val earliestWakeChild = kotlinx.datetime.LocalTime(6, 0)
        val latestWakeChild = kotlinx.datetime.LocalTime(7, 45)
        val leaveChild = kotlinx.datetime.LocalTime(8, 15)
        val childProfiles = makeDayProfiles(earliest = earliestWakeChild, latest = latestWakeChild, bathroom = 10, leave = leaveChild)
        
        val dad = de.familienwecker.famwake.model.FamilyMember(
            id = dadId,
            name = fatherName,
            earliestWakeUp = earliestWakeDad,
            latestWakeUp = latestWakeDad,
            bathroomDurationMinutes = 15,
            wantsBreakfast = true,
            leaveHomeTime = leaveDad,
            isPaused = false,
            isAwakeToday = false,
            claimedByUserId = "mock_user_id",
            claimedByUserName = fatherName,
            claimedByDeviceId = appSettings.deviceId,
            sequenceOrder = 0,
            deviceAlarmEnabled = true,
            dayProfiles = dadProfiles
        )
        
        val mom = de.familienwecker.famwake.model.FamilyMember(
            id = "mock_mom",
            name = motherName,
            earliestWakeUp = earliestWakeMom,
            latestWakeUp = latestWakeMom,
            bathroomDurationMinutes = 20,
            wantsBreakfast = true,
            leaveHomeTime = leaveMom,
            isPaused = false,
            isAwakeToday = false,
            sequenceOrder = 1,
            dayProfiles = momProfiles
        )
        
        val child = de.familienwecker.famwake.model.FamilyMember(
            id = "mock_child",
            name = childName,
            earliestWakeUp = earliestWakeChild,
            latestWakeUp = latestWakeChild,
            bathroomDurationMinutes = 10,
            wantsBreakfast = true,
            leaveHomeTime = leaveChild,
            isPaused = false,
            isAwakeToday = false,
            sequenceOrder = 2,
            dayProfiles = childProfiles
        )
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            memberRepository.cacheMembers(listOf(dad, mom, child))
        }
    }
}

@Composable
fun FamilienweckerApp(familyViewModel: FamilyViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val baseContext = LocalContext.current
    val currentLanguage by familyViewModel.language.collectAsStateWithLifecycle()

    // Nur getResources() überschreiben – alle anderen Context-Aufrufe (WindowManager,
    // ActivityResultRegistry, etc.) delegieren sicher zur echten Activity.
    // createConfigurationContext() bräche diese Kette und crasht Settings.
    val localizedContext = remember(currentLanguage) {
        if (currentLanguage == "system" || currentLanguage.isEmpty()) {
            baseContext
        } else {
            val locale = Locale.forLanguageTag(currentLanguage)
            val config = Configuration(baseContext.resources.configuration)
            config.setLocale(locale)
            val localizedResources = baseContext.createConfigurationContext(config).resources
            object : android.content.ContextWrapper(baseContext) {
                override fun getResources(): android.content.res.Resources = localizedResources
            }
        }
    }

    // System-Level Locale-Präferenz setzen (persistiert über Cold Starts,
    // löst auf Android ≤12 zusätzlich eine Activity-Recreation aus)
    LaunchedEffect(currentLanguage) {
        when {
            currentLanguage == "system" || currentLanguage.isEmpty() ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            currentLanguage in de.familienwecker.famwake.data.AppSettingsImpl.SUPPORTED_LANGUAGE_CODES ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(currentLanguage))
            else ->
                // Unknown code – fall back to English so valid strings are shown
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
    }


    CompositionLocalProvider(LocalContext provides localizedContext) {
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
                    },
                    onNavigateToOnboardingWelcome = {
                        navController.navigate(Routes.ONBOARDING_WELCOME) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.ONBOARDING) {
                val language by familyViewModel.language.collectAsStateWithLifecycle()
                val initialTooltipsEnabled by familyViewModel.tooltipsEnabled.collectAsStateWithLifecycle()
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val isLoggedIn = authState is AuthViewModel.AuthState.Authenticated || authViewModel.isAnonymous
                OnboardingScreen(
                    language   = language,
                    startAtWelcome = false,
                    initialTooltipsEnabled = initialTooltipsEnabled,
                    isLoggedIn = isLoggedIn,
                    onStartAnonymously = { tooltipsEnabled ->
                        familyViewModel.setTooltipsEnabled(tooltipsEnabled)
                        familyViewModel.setOnboardingCompleted(true)
                        TelemetryDeck.signal("onboarding.completed_anonymously")
                        authViewModel.signInAnonymously {
                            val dest = if (familyViewModel.familyId.value != null) Routes.MAIN else Routes.SETUP
                            navController.navigate(dest) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onLogin = { tooltipsEnabled ->
                        familyViewModel.setTooltipsEnabled(tooltipsEnabled)
                        familyViewModel.setOnboardingCompleted(true)
                        authViewModel.clearError()
                        navController.navigate(Routes.LOGIN) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.ONBOARDING_WELCOME) {
                val language by familyViewModel.language.collectAsStateWithLifecycle()
                val initialTooltipsEnabled by familyViewModel.tooltipsEnabled.collectAsStateWithLifecycle()
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val isLoggedIn = authState is AuthViewModel.AuthState.Authenticated || authViewModel.isAnonymous
                OnboardingScreen(
                    language   = language,
                    startAtWelcome = true,
                    initialTooltipsEnabled = initialTooltipsEnabled,
                    isLoggedIn = isLoggedIn,
                    onStartAnonymously = { tooltipsEnabled ->
                        familyViewModel.setTooltipsEnabled(tooltipsEnabled)
                        familyViewModel.setOnboardingCompleted(true)
                        TelemetryDeck.signal("onboarding.completed_anonymously")
                        authViewModel.signInAnonymously {
                            val dest = if (familyViewModel.familyId.value != null) Routes.MAIN else Routes.SETUP
                            navController.navigate(dest) {
                                popUpTo(Routes.ONBOARDING_WELCOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onLogin = { tooltipsEnabled ->
                        familyViewModel.setTooltipsEnabled(tooltipsEnabled)
                        familyViewModel.setOnboardingCompleted(true)
                        authViewModel.clearError()
                        navController.navigate(Routes.LOGIN) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    familyViewModel = familyViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Routes.LOADING) {
                            popUpTo(0) { inclusive = true }
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
                        navController.navigate(Routes.ONBOARDING_WELCOME) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = {
                        authViewModel.clearError()
                        navController.navigate(Routes.LOGIN) {
                            launchSingleTop = true
                        }
                    },
                    isAnonymous = authViewModel.isAnonymous
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
                        navController.navigate(Routes.ONBOARDING_WELCOME) {
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
                        navController.navigate(Routes.ONBOARDING_WELCOME) {
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
                    },
                    isAnonymous = authViewModel.isAnonymous,
                    onNavigateToLogin = {
                        familyViewModel.setOnboardingCompleted(true)
                        authViewModel.clearError()
                        navController.navigate(Routes.LOGIN) {
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
    } // CompositionLocalProvider
}