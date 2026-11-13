package de.familienwecker.famwake.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aptabase.Aptabase
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.ui.components.TooltipBubble
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.viewmodel.*
import de.familienwecker.famwake.util.AlarmPermissionUtils
import de.familienwecker.famwake.util.BatteryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Bereich für Benutzerprofil-Zuordnung und persönliche Alarmeinstellungen (Ton, sanftes Wecken, Abend-Reminder).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileSection(
    viewModel: FamilyViewModel,
    members: List<FamilyMember>,
    myMemberId: String?,
    alarmSoundUri: String?,
    isGentleWakeEnabled: Boolean,
    isEveningReminderEnabled: Boolean,
    tooltipsEnabled: Boolean,
    tooltipAlarmSoundSeen: Boolean,
    isOffline: Boolean,
    isDarkTheme: Boolean,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    var showMemberPicker by remember { mutableStateOf(false) }

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
                Text(
                    stringResource(R.string.settings_profile_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.settings_profile_desc))
            Spacer(modifier = Modifier.height(16.dp))

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
                var memberToSteal by remember { mutableStateOf<FamilyMember?>(null) }
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
                                viewModel.setMyMemberId(null) { _: Boolean -> }
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
                                        viewModel.setMyMemberId(member.id) { success: Boolean ->
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
                                    viewModel.setMyMemberId(member.id, force = true) { _: Boolean -> }
                                    memberToSteal = null
                                    showMemberPicker = false
                                }) { Text(stringResource(R.string.settings_steal_confirm), color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { memberToSteal = null }) {
                                    Text(stringResource(R.string.settings_delete_family_dialog_cancel))
                                }
                            }
                        )
                    }
                }
            }

            // Alarm-Ton
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Sanftes Wecken
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_gentle_wake_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_gentle_wake_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isGentleWakeEnabled,
                    onCheckedChange = { viewModel.setGentleWakeEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Abendlicher Check-In Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_evening_reminder_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_evening_reminder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEveningReminderEnabled,
                    onCheckedChange = { viewModel.setEveningReminderEnabled(it) }
                )
            }
        }
    }
}

/**
 * System- und Akku-Berechtigungsprüfungen für zuverlässige Alarme.
 */
@Composable
fun SettingsPermissionsSection(
    context: Context,
    isDarkTheme: Boolean
) {
    if (de.familienwecker.famwake.FamWakeApplication.isScreenshotMode) return

    val lifecycleOwnerSettings = LocalLifecycleOwner.current
    val isBatteryOptimized = remember { mutableStateOf(!BatteryUtils.isBatteryOptimizationIgnored(context)) }
    val isExactAlarmPermitted = remember { mutableStateOf(AlarmPermissionUtils.hasExactAlarmPermission(context)) }
    val isFullScreenIntentPermitted = remember { mutableStateOf(AlarmPermissionUtils.hasFullScreenIntentPermission(context)) }

    DisposableEffect(lifecycleOwnerSettings) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized.value = !BatteryUtils.isBatteryOptimizationIgnored(context)
                isExactAlarmPermitted.value = AlarmPermissionUtils.hasExactAlarmPermission(context)
                isFullScreenIntentPermitted.value = AlarmPermissionUtils.hasFullScreenIntentPermission(context)
            }
        }
        lifecycleOwnerSettings.lifecycle.addObserver(observer)
        onDispose { lifecycleOwnerSettings.lifecycle.removeObserver(observer) }
    }

    if (isBatteryOptimized.value) {
        PermissionWarningCard(
            title = stringResource(R.string.settings_battery_warning_title),
            body = stringResource(R.string.settings_battery_warning_text),
            buttonLabel = stringResource(R.string.settings_battery_warning_button),
            isDarkTheme = isDarkTheme,
            onAction = { BatteryUtils.requestIgnoreBatteryOptimizations(context) }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (!isExactAlarmPermitted.value) {
        PermissionWarningCard(
            title = stringResource(R.string.settings_exact_alarm_warning_title),
            body = stringResource(R.string.settings_exact_alarm_warning_text),
            buttonLabel = stringResource(R.string.settings_exact_alarm_warning_button),
            isDarkTheme = isDarkTheme,
            onAction = { AlarmPermissionUtils.requestExactAlarmPermission(context) }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (!isFullScreenIntentPermitted.value) {
        PermissionWarningCard(
            title = stringResource(R.string.permission_fullscreen_title),
            body = stringResource(R.string.permission_fullscreen_message),
            buttonLabel = stringResource(R.string.permission_fullscreen_open),
            isDarkTheme = isDarkTheme,
            onAction = { AlarmPermissionUtils.requestFullScreenIntentPermission(context) }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = Manifest.permission.POST_NOTIFICATIONS
        var notifGranted by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(context, notifPermission) == PackageManager.PERMISSION_GRANTED)
        }

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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Urlaubs- und Pausenmodus-Bereich.
 */
@Composable
fun SettingsVacationSection(
    viewModel: FamilyViewModel,
    vacationUntil: String?,
    isDarkTheme: Boolean,
    context: Context
) {
    var showVacationDatePicker by remember { mutableStateOf(false) }

    if (showVacationDatePicker) {
        val nowCal = java.util.Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                viewModel.setVacationUntil(selectedDate)
                showVacationDatePicker = false
            },
            nowCal.get(java.util.Calendar.YEAR),
            nowCal.get(java.util.Calendar.MONTH),
            nowCal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.setOnDismissListener { showVacationDatePicker = false }
        datePickerDialog.show()
    }

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
            if (!vacationUntil.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌴", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.vacation_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.vacation_mode_active_badge),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                val formattedVac = viewModel.formatVacationDate(vacationUntil)
                Text(
                    text = stringResource(R.string.vacation_mode_last_day_off, formattedVac),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val firstAlarm = viewModel.getFirstAlarmDateAfterVacation(vacationUntil)
                val firstAlarmText = if (firstAlarm != null) {
                    stringResource(R.string.vacation_mode_first_alarm, firstAlarm)
                } else {
                    stringResource(R.string.vacation_mode_no_alarm_after)
                }
                Text(
                    text = firstAlarmText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.clearVacation() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        stringResource(R.string.vacation_mode_end_button),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌴", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.vacation_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.vacation_mode_duration_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setVacationPreset(7) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.vacation_mode_preset_1week),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.setVacationPreset(14) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.vacation_mode_preset_2weeks),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setVacationPresetEndOfMonth() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.vacation_mode_preset_month_end),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { showVacationDatePicker = true },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.vacation_mode_select_custom_date),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Familien-Verwaltung (Beitrittscode, Teilen, Verlassen, Löschen).
 */
@Composable
fun SettingsAccountSection(
    viewModel: FamilyViewModel,
    familyName: String?,
    currentJoinCode: String?,
    members: List<FamilyMember>,
    myMemberId: String?,
    isAnonymous: Boolean,
    isAdmin: Boolean,
    tooltipsEnabled: Boolean,
    tooltipInviteSeen: Boolean,
    isDarkTheme: Boolean,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onNavigateToLogin: () -> Unit,
    onLeaveFamily: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteWarningDialog by remember { mutableStateOf(false) }

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
                            Aptabase.instance.trackEvent("family_invite_shared")
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
            val canDeleteFamily = isAdmin || members.none { it.id != myMemberId }
            if (canDeleteFamily) {
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
                            viewModel.deleteFamily { success: Boolean ->
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
                        viewModel.deleteFamily { success: Boolean ->
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
}

/**
 * Einstellungen für Anzeige, Sprache, Theme, Tooltips und Push-Nachrichten.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPreferencesSection(
    viewModel: FamilyViewModel,
    currentLanguage: String,
    themePreference: String,
    tooltipsEnabled: Boolean,
    pushNotificationsEnabled: Boolean,
    isDarkTheme: Boolean
) {
    var showLanguagePicker by remember { mutableStateOf(false) }

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
                        Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

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
                    val knownCodes = (mainLanguages.map { it.code } + dialects.map { it.code }).toSet()

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
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
}

/**
 * Spenden-, Hilfs- und Feedback-Optionen.
 */
@Composable
fun SettingsSupportSection(
    isDarkTheme: Boolean,
    context: Context,
    onStartOnboarding: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onOpenDonationDialog: () -> Unit
) {
    // Support-Karte (Spende)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

            val donateInteractionSource = remember { MutableInteractionSource() }
            Button(
                onClick = onOpenDonationDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(donateInteractionSource),
                interactionSource = donateInteractionSource,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.cd_donate_button))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_support_donate))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Hilfe & Feedback-Karte
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
                Text(
                    stringResource(R.string.settings_help_feedback_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

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

            val supportInteractionSource = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    val subject = Uri.encode("Feedback: FamWake App")
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:famwake@goork.de?subject=$subject".toUri()
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, context.getString(R.string.error_no_app_found), android.widget.Toast.LENGTH_SHORT).show()
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
        }
    }
}

/**
 * Abmelde- und Konto-Lösch-Bereich.
 */
@Composable
fun SettingsLogoutSection(
    isDarkTheme: Boolean,
    context: Context,
    onLogout: () -> Unit
) {
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
                        android.widget.Toast.makeText(context, context.getString(R.string.error_no_app_found), android.widget.Toast.LENGTH_SHORT).show()
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
