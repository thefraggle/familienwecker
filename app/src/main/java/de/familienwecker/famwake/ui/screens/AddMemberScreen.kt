package de.familienwecker.famwake.ui.screens

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
    val members by viewModel.members.collectAsStateWithLifecycle()
    val memberToEdit = remember(memberId, members) { members.find { it.id == memberId } }

    var name by remember(memberToEdit) { mutableStateOf(memberToEdit?.name ?: "") }

    // Starte mit bestehenden dayProfiles oder erzeuge Defaults aus alten Feldern
    var dayProfiles by remember(memberToEdit) {
        mutableStateOf(
            memberToEdit?.dayProfiles ?: defaultDayProfiles(
                earliestWakeUp = memberToEdit?.earliestWakeUp ?: LocalTime.of(6, 0),
                latestWakeUp = memberToEdit?.latestWakeUp ?: LocalTime.of(7, 30),
                bathroomDurationMinutes = memberToEdit?.bathroomDurationMinutes ?: 20L,
                wantsBreakfast = memberToEdit?.wantsBreakfast ?: true,
                leaveHomeTime = memberToEdit?.leaveHomeTime
            )
        )
    }
    var selectedDay by remember { mutableStateOf(1) } // 1=Mo
    var showCopyDialog by remember { mutableStateOf(false) }

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
                            earliestWakeUp = refProfile?.earliestWakeUp ?: LocalTime.of(6, 0),
                            latestWakeUp = refProfile?.latestWakeUp ?: LocalTime.of(7, 30),
                            bathroomDurationMinutes = refProfile?.bathroomDurationMinutes ?: 20L,
                            wantsBreakfast = refProfile?.wantsBreakfast ?: true,
                            leaveHomeTime = refProfile?.leaveHomeTime,
                            isPaused = memberToEdit?.isPaused ?: false,
                            claimedByUserId = memberToEdit?.claimedByUserId,
                            claimedByUserName = memberToEdit?.claimedByUserName,
                            createdAt = memberToEdit?.createdAt,
                            dayProfiles = dayProfiles
                        )
                        viewModel.addOrUpdateMember(memberToSave)
                        onNavigateBack()
                    },
                    enabled = name.isNotBlank() && hasAtLeastOneActiveDay
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WEEKDAY_KEYS.forEach { day ->
                        val profile = dayProfiles[day]
                        val isSelected = selectedDay == day
                        val isActive = profile?.isActive == true
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = day },
                            label = {
                                Text(
                                    text = dayLabelShort(day),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        isSelected && isActive  -> MaterialTheme.colorScheme.onPrimary
                                        isSelected && !isActive -> MaterialTheme.colorScheme.onSurfaceVariant
                                        isActive                -> MaterialTheme.colorScheme.onSurface
                                        else                    -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                // selektiert + aktiv → primary
                                selectedContainerColor = if (isActive)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                selectedLabelColor = if (isActive)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                // nicht selektiert
                                containerColor = if (isActive)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isActive)
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = if (isActive)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // Tages-Card für selectedDay
                val selectedProfile = dayProfiles[selectedDay] ?: DayProfile()
                DayProfileCard(
                    dayLabel = dayLabel(selectedDay),
                    profile = selectedProfile,
                    onProfileChange = { updated ->
                        dayProfiles = dayProfiles.toMutableMap().apply { put(selectedDay, updated) }
                    }
                )

                if (!hasAtLeastOneActiveDay) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.add_member_error_no_days),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

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

@Composable
private fun DayProfileCard(
    dayLabel: String,
    profile: DayProfile,
    onProfileChange: (DayProfile) -> Unit
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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

                    // Früheste Weckzeit
                    TimePickerRow(
                        label = stringResource(R.string.add_member_earliest_wake),
                        time = profile.earliestWakeUp,
                        context = context,
                        formatter = formatter,
                        onTimeSelected = { onProfileChange(profile.copy(earliestWakeUp = it)) }
                    )

                    // Späteste Weckzeit
                    TimePickerRow(
                        label = stringResource(R.string.add_member_latest_wake),
                        time = profile.latestWakeUp,
                        context = context,
                        formatter = formatter,
                        onTimeSelected = { onProfileChange(profile.copy(latestWakeUp = it)) }
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
                    TimePickerRow(
                        label = stringResource(R.string.add_member_leave_home),
                        time = profile.leaveHomeTime ?: LocalTime.of(8, 0),
                        context = context,
                        formatter = formatter,
                        onTimeSelected = { onProfileChange(profile.copy(leaveHomeTime = it)) }
                    )
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
    onTimeSelected: (LocalTime) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
                time.hour,
                time.minute,
                true
            ).show()
        }) {
            Text(time.format(formatter), style = MaterialTheme.typography.titleMedium)
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
