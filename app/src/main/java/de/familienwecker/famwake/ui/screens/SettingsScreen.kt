package de.familienwecker.famwake.ui.screens

import kotlinx.coroutines.launch

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import de.familienwecker.famwake.util.findActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.ui.viewmodel.*
import androidx.compose.ui.res.stringResource
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.components.bounceClick
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import de.familienwecker.famwake.util.BatteryUtils
import androidx.compose.material.icons.filled.BatteryAlert

import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.BrightnessAuto
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import de.familienwecker.famwake.ui.components.TooltipBubble
import androidx.core.net.toUri
import de.familienwecker.famwake.ui.viewmodel.DonationViewModel
import de.familienwecker.famwake.ui.viewmodel.PurchaseState
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Offerings
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler

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

    BackHandler(enabled = true) {
        handleBack()
    }

    val members by viewModel.members.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val alarmSoundUri by viewModel.alarmSoundUri.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val familyName by viewModel.familyName.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isGlobalAdmin by viewModel.isGlobalAdmin.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val tooltipsEnabled by viewModel.tooltipsEnabled.collectAsStateWithLifecycle()
    val tooltipAlarmSoundSeen by viewModel.tooltipAlarmSoundSeen.collectAsStateWithLifecycle()
    val tooltipInviteSeen by viewModel.tooltipInviteSeen.collectAsStateWithLifecycle()
    val pushNotificationsEnabled by viewModel.pushNotificationsEnabled.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showMemberPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    
    val offerings by donationViewModel.offerings.collectAsStateWithLifecycle()
    val purchaseState by donationViewModel.purchaseState.collectAsStateWithLifecycle()
    val isBatteryOptimized = remember { mutableStateOf(!BatteryUtils.isBatteryOptimizationIgnored(context)) }
    val isExactAlarmPermitted = remember { mutableStateOf(de.familienwecker.famwake.util.AlarmPermissionUtils.hasExactAlarmPermission(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher for the RingtonePicker Activity
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                viewModel.setAlarmSoundUri(uri.toString())
            }
        }
    }

    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
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
                // Error is displayed directly in the DonationDialog, 
                // we don't reset the state automatically here to keep it visible.
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

        // Scroll-Indicator: sichtbar solange noch Inhalt unter dem sichtbaren Bereich liegt
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
            
            // Profilauswahl (Wer bin ich?) + Weckton
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_profile_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_profile_desc))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Profil-Claim: Button öffnet BottomSheet mit 2-spaltigem FilterChip-Grid
                    val selectedMember = members.find { it.id == myMemberId }
                    val memberButtonLabel = when {
                        members.isEmpty() -> stringResource(R.string.settings_no_members)
                        selectedMember != null -> selectedMember.name
                        else -> stringResource(R.string.settings_please_select)
                    }
                    OutlinedButton(
                        onClick = {
                            if (isOffline) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.error_profile_claim_offline))
                                }
                            } else if (members.isNotEmpty()) {
                                showMemberPicker = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        enabled = members.isNotEmpty()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(memberButtonLabel, style = MaterialTheme.typography.bodyMedium)
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showMemberPicker) {
                        val memberSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        var memberToSteal by remember { mutableStateOf<de.familienwecker.famwake.model.FamilyMember?>(null) }
                        ModalBottomSheet(
                            onDismissRequest = { showMemberPicker = false },
                            sheetState = memberSheetState
                        ) {
                            Text(
                                text = stringResource(R.string.settings_profile_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
                            )
                            if (myMemberId != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.setMyMemberId(null) { }
                                        showMemberPicker = false
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = 12.dp)
                                        .height(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.settings_no_profile),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            val currentUid = viewModel.currentUserId
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.6f)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(members.size) { i ->
                                    val member = members[i]
                                    val isClaimedByOther = member.claimedByUserId != null &&
                                        member.claimedByUserId != currentUid
                                    val isSelected = member.id == myMemberId
                                    FilterChip(
                                        selected = isSelected,
                                        enabled = true,
                                        onClick = {
                                            if (isClaimedByOther) {
                                                memberToSteal = member
                                            } else {
                                                val errorMsg = context.getString(R.string.error_profile_taken)
                                                viewModel.setMyMemberId(member.id) { success ->
                                                    if (!success) {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(errorMsg)
                                                        }
                                                    }
                                                }
                                                showMemberPicker = false
                                            }
                                        },
                                        label = {
                                            Column {
                                                Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                                if (isClaimedByOther) {
                                                    Text(
                                                        text = stringResource(R.string.settings_already_claimed),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            memberToSteal?.let { member ->
                                AlertDialog(
                                    onDismissRequest = { memberToSteal = null },
                                    title = { Text(stringResource(R.string.settings_steal_title)) },
                                    text = { Text(stringResource(R.string.settings_steal_text, member.name)) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.setMyMemberId(member.id, force = true) { }
                                            memberToSteal = null
                                            showMemberPicker = false
                                        }) { Text(stringResource(R.string.settings_steal_confirm), color = MaterialTheme.colorScheme.error) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { memberToSteal = null }) { Text(stringResource(R.string.settings_delete_family_dialog_cancel)) }
                                    }
                                )
                            }
                        }
                    }

                    // Alarm-Ton direkt in der Profil-Karte – gehört zum persönlichen Setup
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_alarm_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val ringtoneName = remember(alarmSoundUri) {
                        val currentUri = alarmSoundUri
                        if (currentUri != null) {
                            RingtoneManager.getRingtone(context, currentUri.toUri())?.getTitle(context)
                        } else {
                            context.getString(R.string.settings_alarm_default)
                        }
                    }

                    val ringtoneInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.settings_alarm_picker_title))
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                alarmSoundUri?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it.toUri())
                                }
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(ringtoneInteractionSource),
                        interactionSource = ringtoneInteractionSource
                    ) {
                        Text(stringResource(R.string.settings_alarm_select, ringtoneName ?: ""))
                    }

                    if (tooltipsEnabled && !tooltipAlarmSoundSeen) {
                        TooltipBubble(
                            visible = true,
                            text = stringResource(R.string.tooltip_alarm_sound),
                            onDismiss = { viewModel.markTooltipSeen(viewModel.tooltipKeyAlarmSound) }
                        )
                    }


                }
            }

            // Akku-Optimierungs-Warnung und Exakter Alarm
            val lifecycleOwnerSettings = LocalLifecycleOwner.current
            val isExactAlarmPermitted = remember { mutableStateOf(de.familienwecker.famwake.util.AlarmPermissionUtils.hasExactAlarmPermission(context)) }
            val isFullScreenIntentPermitted = remember { mutableStateOf(de.familienwecker.famwake.util.AlarmPermissionUtils.hasFullScreenIntentPermission(context)) }
            DisposableEffect(lifecycleOwnerSettings) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isBatteryOptimized.value = !BatteryUtils.isBatteryOptimizationIgnored(context)
                        isExactAlarmPermitted.value = de.familienwecker.famwake.util.AlarmPermissionUtils.hasExactAlarmPermission(context)
                        isFullScreenIntentPermitted.value = de.familienwecker.famwake.util.AlarmPermissionUtils.hasFullScreenIntentPermission(context)
                    }
                }
                lifecycleOwnerSettings.lifecycle.addObserver(observer)
                onDispose { lifecycleOwnerSettings.lifecycle.removeObserver(observer) }
            }

            if (isBatteryOptimized.value && !de.familienwecker.famwake.FamWakeApplication.isScreenshotMode) {
                PermissionWarningCard(
                    title = stringResource(R.string.settings_battery_warning_title),
                    body = stringResource(R.string.settings_battery_warning_text),
                    buttonLabel = stringResource(R.string.settings_battery_warning_button),
                    isDarkTheme = isDarkTheme,
                    onAction = { BatteryUtils.requestIgnoreBatteryOptimizations(context) }
                )
            }

            if (!isExactAlarmPermitted.value && !de.familienwecker.famwake.FamWakeApplication.isScreenshotMode) {
                PermissionWarningCard(
                    title = stringResource(R.string.settings_exact_alarm_warning_title),
                    body = stringResource(R.string.settings_exact_alarm_warning_text),
                    buttonLabel = stringResource(R.string.settings_exact_alarm_warning_button),
                    isDarkTheme = isDarkTheme,
                    onAction = { de.familienwecker.famwake.util.AlarmPermissionUtils.requestExactAlarmPermission(context) }
                )
            }

            if (!isFullScreenIntentPermitted.value && !de.familienwecker.famwake.FamWakeApplication.isScreenshotMode) {
                PermissionWarningCard(
                    title = stringResource(R.string.permission_fullscreen_title),
                    body = stringResource(R.string.permission_fullscreen_message),
                    buttonLabel = stringResource(R.string.permission_fullscreen_open),
                    isDarkTheme = isDarkTheme,
                    onAction = { de.familienwecker.famwake.util.AlarmPermissionUtils.requestFullScreenIntentPermission(context) }
                )
            }

            // Push-Benachrichtigungen: Warnung wenn deaktiviert (Android 13+)
            // Toggle removed: on Samsung/newer Android, the runtime permission dialog only fires once.
            // After denial, only system settings can re-enable notifications.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val notifPermission = Manifest.permission.POST_NOTIFICATIONS
                var notifGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, notifPermission) == PackageManager.PERMISSION_GRANTED
                    )
                }

                // Re-check on resume (user may return from system settings)
                val lifecycleOwnerNotif = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwnerNotif) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            notifGranted = ContextCompat.checkSelfPermission(context, notifPermission) == PackageManager.PERMISSION_GRANTED
                        }
                    }
                    lifecycleOwnerNotif.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwnerNotif.lifecycle.removeObserver(observer) }
                }

                if (!notifGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                             else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.notif_permission_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.notif_permission_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val notifBtnInteractionSource = remember { MutableInteractionSource() }
                            Button(
                                onClick = {
                                    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(notifBtnInteractionSource),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                interactionSource = notifBtnInteractionSource,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.notif_permission_button))
                            }
                        }
                    }
                }
            }

            // Familie & Account
            val currentJoinCode by viewModel.joinCode.collectAsStateWithLifecycle()
            
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_account_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAnonymous) {
                        val loginInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            onClick = onNavigateToLogin,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(56.dp).bounceClick(loginInteractionSource),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            interactionSource = loginInteractionSource,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.cd_login_button))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_anonymous_login_button))
                        }
                    }

                    currentJoinCode?.let { code ->
                        Text(stringResource(R.string.settings_join_code, familyName ?: ""))
                        Text(
                            text = if (isAnonymous) "******" else code, 
                            style = MaterialTheme.typography.headlineMedium, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                        )

                        // Tooltip E – Einladungscode
                        if (tooltipsEnabled && !tooltipInviteSeen) {
                            TooltipBubble(
                                visible = true,
                                text = stringResource(R.string.tooltip_invite_code),
                                onDismiss = { viewModel.markTooltipSeen(viewModel.tooltipKeyInvite) },
                                isDark = isDarkTheme
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val shareInteractionSource = remember { MutableInteractionSource() }
                        val shareMessage = stringResource(R.string.settings_share_message, familyName ?: "", code)
                        
                        Button(
                            onClick = {
                                if (isAnonymous) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.settings_share_code_locked))
                                    }
                                } else {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .height(56.dp)
                                .bounceClick(shareInteractionSource),
                            interactionSource = shareInteractionSource,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = stringResource(R.string.cd_share_invite))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_share_code))
                        }
                    }
                    val leaveInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onLeaveFamily,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(leaveInteractionSource),
                        interactionSource = leaveInteractionSource
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.cd_leave_family), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_leave_family))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val deleteFamilyInteractionSource = remember { MutableInteractionSource() }
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(deleteFamilyInteractionSource),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            interactionSource = deleteFamilyInteractionSource
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.cd_delete_family), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_delete_family))
                        }
                    } else {
                        val deleteNotAdminMsg = stringResource(R.string.error_delete_not_admin)
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(deleteNotAdminMsg)
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(deleteFamilyInteractionSource),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.outline),
                            interactionSource = deleteFamilyInteractionSource
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.cd_delete_family), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_delete_family))
                        }
                    }
                    

                }
            }

            var showDeleteWarningDialog by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.settings_delete_family_dialog_title)) },
                    text = { Text(stringResource(R.string.settings_delete_family_dialog_text)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val hasOtherMembers = members.any { it.id != myMemberId }
                                if (hasOtherMembers) {
                                    showDeleteDialog = false
                                    showDeleteWarningDialog = true
                                } else {
                                    showDeleteDialog = false
                                    viewModel.deleteFamily { success ->
                                        if (success) onLeaveFamily()
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.settings_delete_family_dialog_confirm), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.settings_delete_family_dialog_cancel))
                        }
                    }
                )
            }

            if (showDeleteWarningDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteWarningDialog = false },
                    title = { Text(stringResource(R.string.settings_delete_family_warning_title)) },
                    text = { Text(stringResource(R.string.settings_delete_family_warning_text)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteWarningDialog = false
                                viewModel.deleteFamily { success ->
                                    if (success) onLeaveFamily()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.settings_delete_family_warning_confirm), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteWarningDialog = false }) {
                            Text(stringResource(R.string.settings_delete_family_dialog_cancel))
                        }
                    }
                )
            }

            // Sprache (Language)
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Gemeinsamer Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_display_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sprache – Button öffnet BottomSheet
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_language_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val languageLabel = when (currentLanguage) {
                        "da"  -> "Dansk"
                        "de"  -> "Deutsch"
                        "en"  -> "English"
                        "es"  -> "Español"
                        "fr"  -> "Français"
                        "it"  -> "Italiano"
                        "ja"  -> "日本語"
                        "ko"  -> "한국어"
                        "nl"  -> "Nederlands"
                        "no"  -> "Norsk"
                        "pl"  -> "Polski"
                        "pt"  -> "Português"
                        "ru"  -> "Русский"
                        "sv"  -> "Svenska"
                        "tr"  -> "Türkçe"
                        "uk"  -> "Українська"
                        "zh"  -> "简体中文"
                        "id"  -> "Bahasa Indonesia"
                        "vi"  -> "Tiếng Việt"
                        "bn"  -> "বাংলা"
                        "mr"  -> "मराठी"
                        "hi"  -> "हिन्दी"
                        "gsw" -> "Schweizerdeutsch"
                        "swg" -> "Schwäbisch"
                        "ksh" -> "Ruhrpott"
                        else  -> "English"
                    }

                    OutlinedButton(
                        onClick = { showLanguagePicker = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(languageLabel, style = MaterialTheme.typography.bodyMedium)
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // BottomSheet für Sprachauswahl
                    if (showLanguagePicker) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            onDismissRequest = { showLanguagePicker = false },
                            sheetState = sheetState
                        ) {
                            Text(
                                text = stringResource(R.string.settings_language_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
                            )


                            // Alphabetisch nach nativem Namen
                            val mainLanguages = listOf(
                                LangEntry("id", "Bahasa Indonesia"),
                                LangEntry("da", "Dansk"),
                                LangEntry("de", "Deutsch"),
                                LangEntry("en", "English"),
                                LangEntry("es", "Español"),
                                LangEntry("fr", "Français"),
                                LangEntry("it", "Italiano"),
                                LangEntry("nl", "Nederlands"),
                                LangEntry("no", "Norsk"),
                                LangEntry("pl", "Polski"),
                                LangEntry("pt", "Português"),
                                LangEntry("sv", "Svenska"),
                                LangEntry("vi", "Tiếng Việt"),
                                LangEntry("tr", "Türkçe"),
                                LangEntry("ru", "Русский"),
                                LangEntry("uk", "Українська"),
                                LangEntry("bn", "বাংলা"),
                                LangEntry("mr", "मराठी"),
                                LangEntry("hi", "हिन्दी"),
                                LangEntry("ja", "日本語"),
                                LangEntry("zh", "简体中文"),
                                LangEntry("ko", "한국어")
                            )
                            val dialects = listOf(
                                LangEntry("gsw", "Schweizerdeutsch"),
                                LangEntry("ksh", "Ruhrpott"),
                                LangEntry("swg", "Schwäbisch"),
                            )

                            // Alle bekannten Hauptsprachcodes für EN-Fallback-Erkennung
                            val knownCodes = (mainLanguages.map { it.code } + dialects.map { it.code }).toSet()

                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Maximalhöhe damit auf kleinen Screens gescrollt werden kann
                                    .fillMaxHeight(0.85f)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(mainLanguages.size) { i ->
                                    val entry = mainLanguages[i]
                                    val isSelected = currentLanguage == entry.code ||
                                        (entry.code == "en" && currentLanguage !in knownCodes)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setLanguage(entry.code)
                                            showLanguagePicker = false
                                        },
                                        label = { Text(entry.label, style = MaterialTheme.typography.bodyMedium) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Trennlinie + Dialekte
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        text = stringResource(R.string.settings_language_dialects),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                items(dialects.size) { i ->
                                    val entry = dialects[i]
                                    val isSelected = currentLanguage == entry.code
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setLanguage(entry.code)
                                            showLanguagePicker = false
                                        },
                                        label = { Text(entry.label, style = MaterialTheme.typography.bodyMedium) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Erscheinungsbild
                    Text(
                        stringResource(R.string.settings_appearance_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val themeOptions = listOf(
                        ThemeOption("light",  Icons.Default.WbSunny,       stringResource(R.string.settings_theme_light)),
                        ThemeOption("system", Icons.Default.BrightnessAuto, stringResource(R.string.settings_theme_system)),
                        ThemeOption("dark",   Icons.Default.DarkMode,       stringResource(R.string.settings_theme_dark)),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = themePreference == option.code ||
                                    (option.code == "system" && themePreference != "light" && themePreference != "dark"),
                                onClick = { viewModel.setThemePreference(option.code) },
                                shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                                icon = {},
                                label = {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = option.description,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tipps & Hinweise
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_tooltips_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.settings_tooltips_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = tooltipsEnabled,
                            onCheckedChange = { viewModel.setTooltipsEnabled(it) }
                        )
                    }
                    if (tooltipsEnabled) {
                        TextButton(
                            onClick = { viewModel.resetAllTooltips() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                stringResource(R.string.settings_tooltips_reset),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Push-Benachrichtigungen
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_push_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.settings_push_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = pushNotificationsEnabled,
                            onCheckedChange = { viewModel.setPushNotificationsEnabled(it) }
                        )
                    }
                }
            }

            // Support the App
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.4f
                    )
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_support_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_support_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))



                    // Spenden Button
                    val donateInteractionSource = remember { MutableInteractionSource() }
                    Button(
                        onClick = { showDonationDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(donateInteractionSource),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        interactionSource = donateInteractionSource,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = stringResource(R.string.cd_donate_button),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_support_donate))
                    }
                }
            }

            // Hilfe & Feedback
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_help_feedback_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // App-Tour neu starten
                    val onboardingInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onStartOnboarding,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(onboardingInteractionSource),
                        interactionSource = onboardingInteractionSource
                    ) {
                        Text(stringResource(R.string.settings_start_onboarding))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Feedback-Formular-Button
                    val feedbackInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onNavigateToFeedback,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(feedbackInteractionSource),
                        interactionSource = feedbackInteractionSource
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = stringResource(R.string.cd_feedback_button), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_feedback_button))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // E-Mail an Entwickler
                    val supportInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            val subject = Uri.encode("Feedback: FamWake App")
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:daniel.notthoff@gmail.com?subject=$subject".toUri()
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: android.content.ActivityNotFoundException) {
                                // Kein E-Mail-Client installiert – ignorieren
                            }
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(supportInteractionSource),
                        interactionSource = supportInteractionSource
                    ) {
                        Icon(Icons.Default.Email, contentDescription = stringResource(R.string.cd_email_developer), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_support_button))
                    }

                    Spacer(modifier = Modifier.height(8.dp))


                }
            }


            // Abmelden – eigene Sektion ganz unten, um versehentliche Taps zu vermeiden
            if (!isAnonymous) {
                Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val logoutInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onLogout,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(logoutInteractionSource),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        interactionSource = logoutInteractionSource
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.cd_logout_button), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_logout))
                    }

                    val deleteAccountUrl = stringResource(R.string.settings_delete_account_url)
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, deleteAccountUrl.toUri())
                            try {
                                context.startActivity(intent)
                            } catch (_: android.content.ActivityNotFoundException) {
                                // Kein Browser installiert – ignorieren
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_delete_account),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Version
                Text(
                    text = "FamWake ${stringResource(R.string.app_name_short)} v${de.familienwecker.famwake.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                // Copyright
                Text(
                    text = stringResource(R.string.settings_footer_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Links: Nutzungsbedingungen · Datenschutz · Impressum
                // Einfache klickbare Texte statt TextButton – kein 48dp-Touch-Target-Overhead.
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_terms_of_use),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val url = context.getString(R.string.settings_terms_of_use_url)
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            } catch (_: android.content.ActivityNotFoundException) {
                                // Kein Browser installiert – ignorieren
                            }
                        }
                    )
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val url = context.getString(R.string.settings_privacy_policy_url)
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            } catch (_: android.content.ActivityNotFoundException) {
                                // Kein Browser installiert – ignorieren
                            }
                        }
                    )
                    Text(
                        text = stringResource(R.string.settings_imprint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val url = context.getString(R.string.settings_imprint_url)
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            } catch (_: android.content.ActivityNotFoundException) {
                                // Kein Browser installiert – ignorieren
                            }
                        }
                    )
                }
            }
        }

        // Scroll-Hint-Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = scrollState.canScrollForward,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
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
                            shape = androidx.compose.foundation.shape.CircleShape
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
        } // Box
    }
    
    // Frischen Abruf (oder Cache-Refetech) anstoßen, wenn der Dialog geöffnet wird
    LaunchedEffect(showDonationDialog) {
        if (showDonationDialog) {
            donationViewModel.fetchOfferings()
        }
    }

    if (showDonationDialog) {
        val activity = context.findActivity()
        if (activity != null) {
            DonationDialog(
                onDismiss = { showDonationDialog = false },
                onDonate = { pkg: com.revenuecat.purchases.Package ->
                    donationViewModel.purchasePackage(activity, pkg)
                },
                offerings = offerings,
                purchaseState = purchaseState
            )
        }
    }
}
}



// ─── Private Composables ──────────────────────────────────────────────────────

@Composable
private fun PermissionWarningCard(
    title: String,
    body: String,
    buttonLabel: String,
    isDarkTheme: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            val interactionSource = remember { MutableInteractionSource() }
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().bounceClick(interactionSource),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(buttonLabel) }
        }
    }
}

private data class LangEntry(val code: String, val label: String)

private data class ThemeOption(val code: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String)
