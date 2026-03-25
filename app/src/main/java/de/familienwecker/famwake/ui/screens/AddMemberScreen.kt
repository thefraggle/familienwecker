package de.familienwecker.famwake.ui.screens
 
import androidx.activity.compose.BackHandler

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.model.toKmpLocalTime

// Mo=1 … So=7 nach java.time.DayOfWeek
private val WEEKDAY_KEYS = 1..7

private fun defaultDayProfiles(
    earliestWakeUp: LocalTime = LocalTime.of(6, 0),
    latestWakeUp: LocalTime = LocalTime.of(7, 30),
    bathroomDurationMinutes: Long = 20L,
    wantsBreakfast: Boolean = true,
    leaveHomeTime: LocalTime? = null
): Map<Int, DayProfile> = WEEKDAY_KEYS.associateWith { day ->
    DayProfile(
        isActive = day <= 5, // Mo–Fr aktiv, Sa–So aus
        earliestWakeUp = earliestWakeUp,
        latestWakeUp = latestWakeUp,
        bathroomDurationMinutes = bathroomDurationMinutes,
        wantsBreakfast = wantsBreakfast,
        leaveHomeTime = leaveHomeTime
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    viewModel: FamilyViewModel,
    memberId: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    val memberToEdit = remember(memberId, members) { members.find { it.id == memberId } }
    val tooltipsEnabled by viewModel.tooltipsEnabled.collectAsStateWithLifecycle()
    val tooltipWakeWindowSeen by viewModel.tooltipWakeWindowSeen.collectAsStateWithLifecycle()
    val tooltipBathroomSeen by viewModel.tooltipBathroomSeen.collectAsStateWithLifecycle()
    val tooltipWeekdaysSeen by viewModel.tooltipWeekdaysSeen.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Wir nutzen memberId als stabilen Key, damit Background-Syncs von 'members'
    // nicht den Bearbeitungs-Zustand zurücksetzen (da remember(memberToEdit) bei jedem Firestore-Sync triggert).
    val initialName = remember(memberId) { memberToEdit?.name ?: "" }
    val initialDayProfiles = remember(memberId) {
        memberToEdit?.dayProfiles ?: defaultDayProfiles(
            earliestWakeUp = memberToEdit?.earliestWakeUp?.toJavaLocalTime() ?: LocalTime.of(6, 0),
            latestWakeUp = memberToEdit?.latestWakeUp?.toJavaLocalTime() ?: LocalTime.of(7, 30),
            bathroomDurationMinutes = memberToEdit?.bathroomDurationMinutes ?: 20L,
            wantsBreakfast = memberToEdit?.wantsBreakfast ?: true,
            leaveHomeTime = memberToEdit?.leaveHomeTime?.toJavaLocalTime()
        )
    }
 
    var name by remember(memberId) { mutableStateOf(initialName) }
    var dayProfiles by remember(memberId) { mutableStateOf(initialDayProfiles) }
    var selectedDay by remember { mutableStateOf(1) } // 1=Mo
    var showCopyDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
 
    val hasChanges = name != initialName || dayProfiles != initialDayProfiles
 
    val handleBack = {
        if (hasChanges) {
            showDiscardConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }
 
    BackHandler(enabled = true) {
        handleBack()
    }

    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val isDarkTheme = when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> LocalDarkTheme.current
    }
    val backgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
        } else {
            listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.background)
        }
    )

    val hasAtLeastOneActiveDay = dayProfiles.values.any { it.isActive }
    val hasAnyValidationError = dayProfiles.entries.any { (_, profile) ->
        profile.isActive && validateDayProfile(profile).isNotEmpty()
    }

    // Copy-Dialog
    if (showCopyDialog) {
        val currentProfile = dayProfiles[selectedDay]
        val otherDays = WEEKDAY_KEYS.filter { it != selectedDay }
        val selectedTargets = remember { mutableStateMapOf<Int, Boolean>().apply { otherDays.forEach { put(it, false) } } }

        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text(stringResource(R.string.add_member_copy_dialog_title)) },
            text = {
                Column {
                    otherDays.forEach { day ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedTargets[day] == true,
                                onCheckedChange = { selectedTargets[day] = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dayLabel(day))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (currentProfile != null) {
                        val updated = dayProfiles.toMutableMap()
                        selectedTargets.filter { it.value }.keys.forEach { d ->
                            updated[d] = currentProfile
                        }
                        dayProfiles = updated
                    }
                    showCopyDialog = false
                }) { Text(stringResource(R.string.add_member_copy_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    // Bestätigungsdialog für ungespeicherte Änderungen
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.unsaved_changes_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text(stringResource(R.string.unsaved_changes_keep))
                }
            }
        )
    }
 
    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (memberId == null) stringResource(R.string.add_member_title_add)
                            else stringResource(R.string.add_member_title_edit)
                        )
                    },
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
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                val unknownStr = stringResource(R.string.add_member_unknown)
                val saveInteractionSource = remember { MutableInteractionSource() }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .bounceClick(saveInteractionSource),
                    interactionSource = saveInteractionSource,
                    onClick = {
                        // Legacyfelder aus dem Wochentag-Profil ableiten (Fallback: Mo-Profil oder Defaults)
                        val refProfile = dayProfiles[1] ?: dayProfiles.values.firstOrNull()
                        val memberToSave = FamilyMember(
                            id = memberId ?: java.util.UUID.randomUUID().toString(),
                            name = name.ifEmpty { unknownStr },
                            earliestWakeUp = (refProfile?.earliestWakeUp ?: LocalTime.of(6, 0)).toKmpLocalTime(),
                            latestWakeUp = (refProfile?.latestWakeUp ?: LocalTime.of(7, 30)).toKmpLocalTime(),
                            bathroomDurationMinutes = refProfile?.bathroomDurationMinutes ?: 20L,
                            wantsBreakfast = refProfile?.wantsBreakfast ?: true,
                            leaveHomeTime = refProfile?.leaveHomeTime?.toKmpLocalTime(),
                            isPaused = memberToEdit?.isPaused ?: false,
                            claimedByUserId = memberToEdit?.claimedByUserId,
                            claimedByUserName = memberToEdit?.claimedByUserName,
                            createdAt = memberToEdit?.createdAt,
                            dayProfiles = dayProfiles // DayProfile is already corrected in the logic? Wait.
                        )
                        viewModel.addOrUpdateMember(memberToSave)
                        
                        // Intelligenten Review-Prompt prüfen
                        (context as? android.app.Activity)?.let { activity ->
                            viewModel.checkAndShowReview(activity)
                        }
                        
                        onNavigateBack()
                    },
                    enabled = name.isNotBlank() && !hasAnyValidationError
                ) {
                    Text(stringResource(R.string.add_member_submit))
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Fehlermeldung (falls vorhanden)
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ ${error.asString()}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.add_member_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Wochentag-Chip-Leiste
                Text(
                    text = stringResource(R.string.add_member_day_profiles_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WEEKDAY_KEYS.forEach { day ->
                        val profile = dayProfiles[day]
                        val isSelected = selectedDay == day
                        val isActive = profile?.isActive == true
                        val hasError = profile != null && profile.isActive && validateDayProfile(profile).isNotEmpty()
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = day },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = dayLabelShort(day),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        hasError                -> MaterialTheme.colorScheme.error
                                        isSelected && isActive  -> MaterialTheme.colorScheme.onPrimary
                                        isSelected && !isActive -> MaterialTheme.colorScheme.onSurfaceVariant
                                        isActive                -> MaterialTheme.colorScheme.onSurface
                                        else                    -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    },
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 0.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when {
                                    hasError -> MaterialTheme.colorScheme.errorContainer
                                    isActive -> MaterialTheme.colorScheme.primary
                                    else     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                },
                                selectedLabelColor = if (isActive && !hasError)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = if (isActive)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = when {
                                    hasError -> MaterialTheme.colorScheme.error
                                    isActive -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    else     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                },
                                selectedBorderColor = when {
                                    hasError -> MaterialTheme.colorScheme.error
                                    isActive -> MaterialTheme.colorScheme.primary
                                    else     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                }
                            )
                        )
                    }
                }

                // Tooltip G – Wochentage
                if (tooltipsEnabled && !tooltipWeekdaysSeen) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TooltipBubble(
                        visible = true,
                        text = stringResource(R.string.tooltip_weekdays),
                        onDismiss = { viewModel.markTooltipSeen(viewModel.tooltipKeyWeekdays) }
                    )
                }

                // Tages-Card für selectedDay
                val selectedProfile = dayProfiles[selectedDay] ?: DayProfile()
                DayProfileCard(
                    dayLabel = dayLabel(selectedDay),
                    profile = selectedProfile,
                    onProfileChange = { updated ->
                        dayProfiles = dayProfiles.toMutableMap().apply { put(selectedDay, updated) }
                    },
                    showTooltipWakeWindow = tooltipsEnabled && !tooltipWakeWindowSeen,
                    onDismissTooltipWakeWindow = { viewModel.markTooltipSeen(viewModel.tooltipKeyWakeWindow) },
                    showTooltipBathroom = tooltipsEnabled && !tooltipBathroomSeen,
                    onDismissTooltipBathroom = { viewModel.markTooltipSeen(viewModel.tooltipKeyBathroom) }
                )


                // Copy-Button
                TextButton(
                    onClick = { showCopyDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.add_member_copy_to_days, dayLabel(selectedDay)))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Validiert ein DayProfile auf sinnvolle Zeitkombinationen.
 * Gibt eine Liste von Fehlermeldungs-Ressourcen-IDs zurück (leer = gültig).
 */
private fun validateDayProfile(profile: DayProfile): List<Int> {
    val errors = mutableListOf<Int>()
    // 1. latestWakeUp muss NACH earliestWakeUp liegen
    if (profile.latestWakeUp <= profile.earliestWakeUp) {
        errors.add(R.string.validation_latest_before_earliest)
    }
    // 2. leaveHomeTime (effektiv: gesetzter Wert oder UI-Default 08:00)
    //    muss NACH latestWakeUp + Baddauer liegen
    val effectiveLeaveTime = profile.leaveHomeTime?.toJavaLocalTime() ?: java.time.LocalTime.of(8, 0)
    val latestBathroomEnd = profile.latestWakeUp.toJavaLocalTime().plusMinutes(profile.bathroomDurationMinutes)
    if (!effectiveLeaveTime.isAfter(latestBathroomEnd)) {
        errors.add(R.string.validation_leave_too_early)
    }
    return errors
}

@Composable
private fun DayProfileCard(
    dayLabel: String,
    profile: DayProfile,
    onProfileChange: (DayProfile) -> Unit,
    showTooltipWakeWindow: Boolean = false,
    onDismissTooltipWakeWindow: () -> Unit = {},
    showTooltipBathroom: Boolean = false,
    onDismissTooltipBathroom: () -> Unit = {}
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val errors = if (profile.isActive) validateDayProfile(profile) else emptyList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = if (errors.isNotEmpty())
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header: Tag + Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.add_member_day_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = profile.isActive,
                        onCheckedChange = { onProfileChange(profile.copy(isActive = it)) }
                    )
                }
            }

            AnimatedVisibility(visible = profile.isActive) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider()

                    val latestBeforeEarliestError = profile.latestWakeUp <= profile.earliestWakeUp

                    // Früheste Weckzeit
                    TimePickerRow(
                        label = stringResource(R.string.add_member_earliest_wake),
                        time = profile.earliestWakeUp.toJavaLocalTime(),
                        context = context,
                        formatter = formatter,
                        onTimeSelected = { onProfileChange(profile.copy(earliestWakeUp = it.toKmpLocalTime())) }
                    )

                    // Späteste Weckzeit
                    TimePickerRow(
                        label = stringResource(R.string.add_member_latest_wake),
                        time = profile.latestWakeUp.toJavaLocalTime(),
                        context = context,
                        formatter = formatter,
                        onTimeSelected = { onProfileChange(profile.copy(latestWakeUp = it.toKmpLocalTime())) },
                        isError = latestBeforeEarliestError
                    )
                    if (latestBeforeEarliestError) {
                        Text(
                            text = stringResource(R.string.validation_latest_before_earliest),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                                    // Tooltip C – Weckzeitfenster
                                    TooltipBubble(
                                        visible = showTooltipWakeWindow,
                                        text = stringResource(R.string.tooltip_wake_window),
                                        onDismiss = onDismissTooltipWakeWindow
                                    )

                    // Baddauer (+/-)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.add_member_bathroom_duration), style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (profile.bathroomDurationMinutes > 5) onProfileChange(profile.copy(bathroomDurationMinutes = profile.bathroomDurationMinutes - 5)) }
                            ) { Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                            Text(
                                "${profile.bathroomDurationMinutes} min",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.widthIn(min = 64.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = { if (profile.bathroomDurationMinutes < 120) onProfileChange(profile.copy(bathroomDurationMinutes = profile.bathroomDurationMinutes + 5)) }
                            ) { Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                        }
                    }

                    // Tooltip D – Baddauer
                    TooltipBubble(
                        visible = showTooltipBathroom,
                        text = stringResource(R.string.tooltip_bathroom),
                        onDismiss = onDismissTooltipBathroom
                    )

                    // Frühstück
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.add_member_wants_breakfast), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = profile.wantsBreakfast,
                            onCheckedChange = { onProfileChange(profile.copy(wantsBreakfast = it)) }
                        )
                    }

                    // Abfahrtszeit
                    val effectiveLeaveTime = profile.leaveHomeTime?.toJavaLocalTime() ?: java.time.LocalTime.of(8, 0)
                    val leaveTooEarlyError =
                        !effectiveLeaveTime.isAfter(profile.latestWakeUp.toJavaLocalTime().plusMinutes(profile.bathroomDurationMinutes))

                    TimePickerRow(
                        label = stringResource(R.string.add_member_leave_home),
                        time = effectiveLeaveTime,
                        context = context,
                        formatter = formatter,
                        onTimeSelected = { onProfileChange(profile.copy(leaveHomeTime = it.toKmpLocalTime())) },
                        isError = leaveTooEarlyError
                    )
                    if (leaveTooEarlyError) {
                        Text(
                            text = stringResource(R.string.validation_leave_too_early),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePickerRow(
    label: String,
    time: LocalTime,
    context: android.content.Context,
    formatter: DateTimeFormatter,
    onTimeSelected: (LocalTime) -> Unit,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
                time.hour,
                time.minute,
                true
            ).show()
        }) {
            Text(
                time.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

// TimePickerRow benötigt für Rückwärtskompatibilität noch als top-level Funktion
@Composable
fun TimePickerRow(label: String, time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    TimePickerRow(label = label, time = time, context = context, formatter = formatter, onTimeSelected = onTimeSelected)
}

private fun dayNameRes(day: Int): Int = when (day) {
    1 -> R.string.weekday_1; 2 -> R.string.weekday_2; 3 -> R.string.weekday_3
    4 -> R.string.weekday_4; 5 -> R.string.weekday_5; 6 -> R.string.weekday_6
    7 -> R.string.weekday_7; else -> R.string.weekday_1
}

private fun dayShortRes(day: Int): Int = when (day) {
    1 -> R.string.weekday_short_1; 2 -> R.string.weekday_short_2; 3 -> R.string.weekday_short_3
    4 -> R.string.weekday_short_4; 5 -> R.string.weekday_short_5; 6 -> R.string.weekday_short_6
    7 -> R.string.weekday_short_7; else -> R.string.weekday_short_1
}

@Composable
private fun dayLabel(day: Int) = stringResource(dayNameRes(day))

@Composable
private fun dayLabelShort(day: Int) = stringResource(dayShortRes(day))
