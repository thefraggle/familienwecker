package de.familienwecker.famwake.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import de.familienwecker.famwake.ui.viewmodel.*
import de.familienwecker.famwake.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FamilyViewModel,
    donationViewModel: DonationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onLogout: () -> Unit,
    onLeaveFamily: () -> Unit,
    onStartOnboarding: () -> Unit = {},
    isAnonymous: Boolean = false,
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val handleBack = {
        activity?.let {
            viewModel.checkAndShowReview(it)
        }
        onNavigateBack()
    }

    val members by viewModel.members.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle(initialValue = false)
    val familyName by viewModel.familyName.collectAsStateWithLifecycle()
    val currentJoinCode by viewModel.joinCode.collectAsStateWithLifecycle()
    val vacationUntil by viewModel.vacationUntil.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val isGentleWakeEnabled by viewModel.isGentleWakeEnabled.collectAsStateWithLifecycle()
    val isEveningReminderEnabled by viewModel.isEveningReminderEnabled.collectAsStateWithLifecycle()
    val alarmSoundUri by viewModel.alarmSoundUri.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val tooltipsEnabled by viewModel.tooltipsEnabled.collectAsStateWithLifecycle()
    val tooltipAlarmSoundSeen by viewModel.tooltipAlarmSoundSeen.collectAsStateWithLifecycle()
    val tooltipInviteSeen by viewModel.tooltipInviteSeen.collectAsStateWithLifecycle()
    val pushNotificationsEnabled by viewModel.pushNotificationsEnabled.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showDonationDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (showDonationDialog) {
            showDonationDialog = false
        } else {
            handleBack()
        }
    }

    val offerings by donationViewModel.offerings.collectAsStateWithLifecycle()
    val purchaseState by donationViewModel.purchaseState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val systemDark = LocalDarkTheme.current
    val isDarkTheme = when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    // Zeige Error-Messages aus dem ViewModel im Snackbar an
    LaunchedEffect(errorMessage) {
        errorMessage?.let { text ->
            val message = text.asString(context)
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Donation Feedback
    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is PurchaseState.Success -> {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_donate_success))
                showDonationDialog = false
                donationViewModel.resetState()
            }
            is PurchaseState.Error -> {
                // Error is displayed directly in the DonationDialog
            }
            else -> {}
        }
    }

    val backgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
        } else {
            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.background)
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        val backInteractionSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier.bounceClick(backInteractionSource),
                            interactionSource = backInteractionSource
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            val scrollState = rememberScrollState()

            val infiniteTransition = rememberInfiniteTransition(label = "scrollHint")
            val scrollHintBounce by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scrollHintBounce"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Profil- & Alarmeinstellungen
                    SettingsProfileSection(
                        viewModel = viewModel,
                        members = members,
                        myMemberId = myMemberId,
                        alarmSoundUri = alarmSoundUri,
                        isGentleWakeEnabled = isGentleWakeEnabled,
                        isEveningReminderEnabled = isEveningReminderEnabled,
                        tooltipsEnabled = tooltipsEnabled,
                        tooltipAlarmSoundSeen = tooltipAlarmSoundSeen,
                        isOffline = isOffline,
                        isDarkTheme = isDarkTheme,
                        context = context,
                        coroutineScope = coroutineScope,
                        snackbarHostState = snackbarHostState
                    )

                    // Berechtigungen & Alarm-Zuverlässigkeit
                    SettingsPermissionsSection(
                        context = context,
                        isDarkTheme = isDarkTheme
                    )

                    // Urlaubs- & Pausenmodus
                    SettingsVacationSection(
                        viewModel = viewModel,
                        vacationUntil = vacationUntil,
                        isDarkTheme = isDarkTheme,
                        context = context
                    )

                    // Familie & Account
                    SettingsAccountSection(
                        viewModel = viewModel,
                        familyName = familyName,
                        currentJoinCode = currentJoinCode,
                        members = members,
                        myMemberId = myMemberId,
                        isAnonymous = isAnonymous,
                        isAdmin = isAdmin,
                        tooltipsEnabled = tooltipsEnabled,
                        tooltipInviteSeen = tooltipInviteSeen,
                        isDarkTheme = isDarkTheme,
                        context = context,
                        coroutineScope = coroutineScope,
                        snackbarHostState = snackbarHostState,
                        onNavigateToLogin = onNavigateToLogin,
                        onLeaveFamily = onLeaveFamily
                    )

                    // Sprache, Theme, Tooltips & Push
                    SettingsPreferencesSection(
                        viewModel = viewModel,
                        currentLanguage = currentLanguage,
                        themePreference = themePreference,
                        tooltipsEnabled = tooltipsEnabled,
                        pushNotificationsEnabled = pushNotificationsEnabled,
                        isDarkTheme = isDarkTheme
                    )

                    // Spenden, Hilfe & Feedback
                    SettingsSupportSection(
                        isDarkTheme = isDarkTheme,
                        context = context,
                        onStartOnboarding = onStartOnboarding,
                        onNavigateToFeedback = onNavigateToFeedback,
                        onOpenDonationDialog = { showDonationDialog = true }
                    )

                    // Abmelden
                    if (!isAnonymous) {
                        SettingsLogoutSection(
                            isDarkTheme = isDarkTheme,
                            context = context,
                            onLogout = onLogout
                        )
                    }

                    // Footer mit Version, Impressum, Datenschutz
                    SettingsLegalFooter(context = context)
                }

                // Scroll-Hint Overlay
                AnimatedVisibility(
                    visible = scrollState.canScrollForward,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .graphicsLayer { translationY = scrollHintBounce },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(showDonationDialog) {
        if (showDonationDialog) {
            donationViewModel.fetchOfferings()
        }
    }

    if (showDonationDialog) {
        if (activity != null) {
            DonationDialog(
                onDismiss = { showDonationDialog = false },
                onDonate = { pkg -> donationViewModel.purchasePackage(activity, pkg) },
                offerings = offerings,
                purchaseState = purchaseState
            )
        }
    }
}
