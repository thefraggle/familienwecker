package de.familienwecker.famwake.ui.screens

import kotlinx.coroutines.launch

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Tune
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
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Offerings
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FamilyViewModel,
    donationViewModel: DonationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onLogout: () -> Unit,
    onLeaveFamily: () -> Unit,
    onStartOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val alarmSoundUri by viewModel.alarmSoundUri.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val familyName by viewModel.familyName.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isGlobalAdmin by viewModel.isGlobalAdmin.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val tooltipsEnabled by viewModel.tooltipsEnabled.collectAsStateWithLifecycle()
    val tooltipInviteSeen by viewModel.tooltipInviteSeen.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    
    val offerings by donationViewModel.offerings.collectAsStateWithLifecycle()
    val purchaseState by donationViewModel.purchaseState.collectAsStateWithLifecycle()
    val isBatteryOptimized = remember { mutableStateOf(!BatteryUtils.isBatteryOptimizationIgnored(context)) }
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
    
    // Intelligenten Review-Prompt beim Öffnen der Settings prüfen
    val activity = context as? android.app.Activity
    LaunchedEffect(Unit) {
        activity?.let { viewModel.checkAndShowReview(it) }
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
                        onClick = onNavigateBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Profilauswahl (Wer bin ich?)
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = {
                            if (isOffline) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.error_profile_claim_offline))
                                }
                            } else if (members.isNotEmpty()) {
                                expanded = !expanded
                            }
                        }
                    ) {
                        val selectedMember = members.find { it.id == myMemberId }
                        OutlinedTextField(
                            value = when {
                                members.isEmpty() -> stringResource(R.string.settings_no_members)
                                selectedMember != null -> selectedMember.name
                                else -> stringResource(R.string.settings_please_select)
                            },
                            onValueChange = {},
                            readOnly = true,
                            enabled = members.isNotEmpty(),
                            trailingIcon = { if (members.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            members.forEach { member ->
                                val currentUid = viewModel.currentUserId
                                val isClaimedByOther = member.claimedByUserId != null && member.claimedByUserId != currentUid
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(member.name)
                                            if (isClaimedByOther) {
                                                Text(
                                                    text = stringResource(R.string.settings_already_claimed),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (!isClaimedByOther) {
                                            val errorMessage = context.getString(R.string.error_profile_taken)
                                            viewModel.setMyMemberId(member.id) { success ->
                                                if (!success) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(errorMessage)
                                                    }
                                                }
                                            }
                                            expanded = false
                                        }
                                    },
                                    enabled = !isClaimedByOther
                                )
                            }
                            if (members.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_no_members)) },
                                    onClick = { expanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // Weckereinstellungen (Ton)
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_alarm_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

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
                        modifier = Modifier.fillMaxWidth().bounceClick(ringtoneInteractionSource),
                        interactionSource = ringtoneInteractionSource
                    ) {
                        Text(stringResource(R.string.settings_alarm_select, ringtoneName ?: ""))
                    }
                }
            }

            // Akku-Optimierungs-Warnung
            val lifecycleOwnerSettings = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwnerSettings) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isBatteryOptimized.value = !BatteryUtils.isBatteryOptimizationIgnored(context)
                    }
                }
                lifecycleOwnerSettings.lifecycle.addObserver(observer)
                onDispose { lifecycleOwnerSettings.lifecycle.removeObserver(observer) }
            }

            if (isBatteryOptimized.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                         else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_battery_warning_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.settings_battery_warning_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val batteryInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            onClick = { BatteryUtils.requestIgnoreBatteryOptimizations(context) },
                            modifier = Modifier.fillMaxWidth().bounceClick(batteryInteractionSource),
                            interactionSource = batteryInteractionSource,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.settings_battery_warning_button))
                        }
                    }
                }
            }

            // Familie & Account
            val currentJoinCode by viewModel.joinCode.collectAsStateWithLifecycle()
            
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                    currentJoinCode?.let { code ->
                        Text(stringResource(R.string.settings_join_code, familyName ?: ""))
                        Text(
                            text = code, 
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
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .bounceClick(shareInteractionSource),
                            interactionSource = shareInteractionSource,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_share_code))
                        }
                    }
                    val leaveInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onLeaveFamily,
                        modifier = Modifier.fillMaxWidth().bounceClick(leaveInteractionSource),
                        interactionSource = leaveInteractionSource
                    ) {
                        Text(stringResource(R.string.settings_leave_family))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val deleteFamilyInteractionSource = remember { MutableInteractionSource() }
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth().bounceClick(deleteFamilyInteractionSource),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            interactionSource = deleteFamilyInteractionSource
                        ) {
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
                            modifier = Modifier.fillMaxWidth().bounceClick(deleteFamilyInteractionSource),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.outline),
                            interactionSource = deleteFamilyInteractionSource
                        ) {
                            Text(stringResource(R.string.settings_delete_family))
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    val logoutInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().bounceClick(logoutInteractionSource),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        interactionSource = logoutInteractionSource
                    ) {
                        Text(stringResource(R.string.settings_logout))
                    }

                    val deleteAccountUrl = stringResource(R.string.settings_delete_account_url)
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, deleteAccountUrl.toUri())
                            context.startActivity(intent)
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

                    // Sprache
                    Text(
                        stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = languageExpanded,
                        onExpandedChange = { languageExpanded = !languageExpanded }
                    ) {
                        val languageLabel = when (currentLanguage) {
                            "de"  -> stringResource(R.string.settings_language_german)
                            "es"  -> stringResource(R.string.settings_language_spanish)
                            "fr"  -> stringResource(R.string.settings_language_french)
                            "it"  -> stringResource(R.string.settings_language_italian)
                            "nl"  -> stringResource(R.string.settings_language_dutch)
                            "pl"  -> stringResource(R.string.settings_language_polish)
                            "pt"  -> stringResource(R.string.settings_language_portuguese)
                            "gsw" -> stringResource(R.string.settings_language_schweizerdeutsch)
                            "swg" -> stringResource(R.string.settings_language_schwaebisch)
                            "ksh" -> stringResource(R.string.settings_language_ruhrpott)
                            else  -> stringResource(R.string.settings_language_english)
                        }
                        
                        OutlinedTextField(
                            value = languageLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_german)) },
                                onClick = { viewModel.setLanguage("de"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_english)) },
                                onClick = { viewModel.setLanguage("en"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_spanish)) },
                                onClick = { viewModel.setLanguage("es"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_french)) },
                                onClick = { viewModel.setLanguage("fr"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_italian)) },
                                onClick = { viewModel.setLanguage("it"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_dutch)) },
                                onClick = { viewModel.setLanguage("nl"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_polish)) },
                                onClick = { viewModel.setLanguage("pl"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_portuguese)) },
                                onClick = { viewModel.setLanguage("pt"); languageExpanded = false }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_ruhrpott)) },
                                onClick = { viewModel.setLanguage("ksh"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_schwaebisch)) },
                                onClick = { viewModel.setLanguage("swg"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_schweizerdeutsch)) },
                                onClick = { viewModel.setLanguage("gsw"); languageExpanded = false }
                            )
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
                    
                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = !themeExpanded }
                    ) {
                        val themeLabel = when (themePreference) {
                            "dark" -> stringResource(R.string.settings_theme_dark)
                            "light" -> stringResource(R.string.settings_theme_light)
                            else -> stringResource(R.string.settings_language_system)
                        }
                        
                        OutlinedTextField(
                            value = themeLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_theme_light)) },
                                onClick = { viewModel.setThemePreference("light"); themeExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_theme_dark)) },
                                onClick = { viewModel.setThemePreference("dark"); themeExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_system)) },
                                onClick = { viewModel.setThemePreference("system"); themeExpanded = false }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tipps & Hinweise
                    Text(
                        stringResource(R.string.settings_tooltips_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                }
            }

            // Support the App
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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

                    val rateInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                // Manueller Aufruf ignoriert die Zeitbeschränkungen
                                viewModel.checkAndShowReview(activity, ignoreConstraints = true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().bounceClick(rateInteractionSource),
                        interactionSource = rateInteractionSource
                    ) {
                        Text(stringResource(R.string.settings_rate_app))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Spenden Button
                    val donateInteractionSource = remember { MutableInteractionSource() }
                    Button(
                        onClick = { showDonationDialog = true },
                        modifier = Modifier.fillMaxWidth().bounceClick(donateInteractionSource),
                        interactionSource = donateInteractionSource,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                        modifier = Modifier.fillMaxWidth().bounceClick(onboardingInteractionSource),
                        interactionSource = onboardingInteractionSource
                    ) {
                        Text(stringResource(R.string.settings_start_onboarding))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Feedback-Formular-Button
                    val feedbackInteractionSource = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onNavigateToFeedback,
                        modifier = Modifier.fillMaxWidth().bounceClick(feedbackInteractionSource),
                        interactionSource = feedbackInteractionSource
                    ) {
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
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().bounceClick(supportInteractionSource),
                        interactionSource = supportInteractionSource
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_support_button))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isGlobalAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val adminInteractionSource = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = { showAdminDialog = true },
                            modifier = Modifier.fillMaxWidth().bounceClick(adminInteractionSource),
                            interactionSource = adminInteractionSource,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Admin-Menü")
                        }

                        if (showAdminDialog) {
                            var adminAlarmConfirmed by remember { mutableStateOf(false) }
                            var adminReportConfirmed by remember { mutableStateOf(false) }

                            AlertDialog(
                                onDismissRequest = { showAdminDialog = false },
                                title = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Admin-Funktionen")
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Diese Funktionen sind nur für globale Administratoren verfügbar.")

                                        var adminSetupStatus by remember { mutableStateOf("") }
                                        val adminAlarmInteraction = remember { MutableInteractionSource() }
                                        Button(
                                            onClick = {
                                                viewModel.setupTestAlarmAndMembers { status ->
                                                    adminSetupStatus = status
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().bounceClick(adminAlarmInteraction),
                                            interactionSource = adminAlarmInteraction,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            ),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Notifications, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (adminSetupStatus.isNotEmpty()) "✓ $adminSetupStatus + 2-Min-Wecker"
                                                else "Test-Wecker (2 Min)"
                                            )
                                        }

                                        val adminReportInteraction = remember { MutableInteractionSource() }
                                        Button(
                                            onClick = {
                                                viewModel.requestAdminStatsReport { success ->
                                                    if (success) adminReportConfirmed = true
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().bounceClick(adminReportInteraction),
                                            interactionSource = adminReportInteraction,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            ),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.BarChart, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(if (adminReportConfirmed) "✓ Report angefordert" else "Statistik-Report (E-Mail)")
                                        }

                                        val adminReviewInteraction = remember { MutableInteractionSource() }
                                        Button(
                                            onClick = {
                                                if (activity != null) {
                                                    viewModel.checkAndShowReview(activity, ignoreConstraints = true)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().bounceClick(adminReviewInteraction),
                                            interactionSource = adminReviewInteraction,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Review Flow testen")
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showAdminDialog = false }) {
                                        Text("Schließen")
                                    }
                                }
                            )
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Version
                Text(
                    text = stringResource(R.string.settings_footer_version, de.familienwecker.famwake.BuildConfig.VERSION_NAME),
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
                // All rights reserved
                Text(
                    text = stringResource(R.string.settings_footer_rights),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Links: Nutzungsbedingungen • Datenschutz • Impressum
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val url = context.getString(R.string.settings_terms_of_use_url)
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_terms_of_use),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            val url = context.getString(R.string.settings_privacy_policy_url)
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_privacy_policy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            val url = context.getString(R.string.settings_imprint_url)
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_imprint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Frischen Abruf (oder Cache-Refetech) anstoßen, wenn der Dialog geöffnet wird
    LaunchedEffect(showDonationDialog) {
        if (showDonationDialog) {
            donationViewModel.fetchOfferings()
        }
    }

    if (showDonationDialog) {
        val activity = context as? Activity
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


