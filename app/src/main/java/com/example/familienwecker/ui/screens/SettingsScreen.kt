package com.example.familienwecker.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.familienwecker.ui.viewmodel.FamilyViewModel
import androidx.compose.ui.res.stringResource
import com.example.familienwecker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FamilyViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onLeaveFamily: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val myMemberId by viewModel.myMemberId.collectAsState()
    val alarmSoundUri by viewModel.alarmSoundUri.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()
    val familyName by viewModel.familyName.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) 
                        MaterialTheme.colorScheme.surface 
                    else 
                        MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
            
            // 1. Profilauswahl (Wer bin ich?)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
                        onExpandedChange = { if (members.isNotEmpty()) expanded = !expanded }
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
                                val isClaimedByOther = member.claimedByUserId != null && member.claimedByUserId != viewModel.myMemberId.value
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(member.name)
                                            if (isClaimedByOther) {
                                                Text(
                                                    text = "Bereits belegt durch ${member.claimedByUserName ?: "jemand anderen"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (!isClaimedByOther) {
                                            viewModel.setMyMemberId(member.id) { success ->
                                                if (!success) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Dieses Profil ist bereits belegt.")
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

            // 2. Weckereinstellungen (Ton)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_alarm_select, ringtoneName ?: ""))
                    }
                }
            }

            // 3. Hilfe
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_help_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_help_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 4. Familie & Account
            val currentJoinCode by viewModel.joinCode.collectAsState()
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_account_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (currentJoinCode != null) {
                        Text(stringResource(R.string.settings_join_code, familyName ?: ""))
                        Text(
                            text = currentJoinCode!!, 
                            style = MaterialTheme.typography.headlineMedium, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onLeaveFamily,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_leave_family))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.settings_delete_family))
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.settings_logout))
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

            // 5. Sprache (Language)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                }
            }

            // 6. Support
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_support_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_support_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            val subject = Uri.encode("Feedback: FamWake App")
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:daniel.notthoff@gmail.com?subject=$subject")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_support_button))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = {
                            val url = context.getString(R.string.settings_privacy_policy_url)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_privacy_policy))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val url = context.getString(R.string.settings_imprint_url)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_imprint))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val url = context.getString(R.string.settings_delete_account_url)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_delete_account))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.settings_version_info, stringResource(R.string.app_name), com.example.familienwecker.BuildConfig.VERSION_NAME, com.example.familienwecker.BuildConfig.COMMIT_HASH, com.example.familienwecker.BuildConfig.COMMIT_DATE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
