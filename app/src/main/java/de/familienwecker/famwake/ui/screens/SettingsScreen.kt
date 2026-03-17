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
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FamilyViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onLogout: () -> Unit,
    onLeaveFamily: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val alarmSoundUri by viewModel.alarmSoundUri.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val familyName by viewModel.familyName.collectAsStateWithLifecycle()
    val isAdmin by viewModel.familyCreatorId.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
                        if (alarmSoundUri != null) {
                            RingtoneManager.getRingtone(context, Uri.parse(alarmSoundUri)).getTitle(context)
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
                                if (alarmSoundUri != null) {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarmSoundUri))
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
                    if (viewModel.isAdmin) {
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
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(deleteAccountUrl))
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
                            "de" -> stringResource(R.string.settings_language_german)
                            else -> stringResource(R.string.settings_language_english)
                        }
                        
                        OutlinedTextField(
                            value = languageLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_english)) },
                                onClick = { viewModel.setLanguage("en"); languageExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_german)) },
                                onClick = { viewModel.setLanguage("de"); languageExpanded = false }
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
                                data = Uri.parse("mailto:daniel.notthoff@gmail.com?subject=$subject")
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_footer_version, de.familienwecker.famwake.BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.settings_footer_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.settings_footer_rights),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
}

@Composable
fun HelpBulletPoint(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = text.replace("<b>", "").replace("</b>", ""), 
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
