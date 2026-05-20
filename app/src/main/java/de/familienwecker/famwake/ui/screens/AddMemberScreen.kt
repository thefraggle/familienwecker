package de.familienwecker.famwake.ui.screens
 
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Timer
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
import de.familienwecker.famwake.ui.viewmodel.*
import de.familienwecker.famwake.util.findActivity
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.model.toKmpLocalTime
import de.familienwecker.famwake.ui.components.TooltipBubble

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
        earliestWakeUp = earliestWakeUp.toKmpLocalTime(),
        latestWakeUp = latestWakeUp.toKmpLocalTime(),
        bathroomDurationMinutes = bathroomDurationMinutes,
        wantsBreakfast = wantsBreakfast,
        leaveHomeTime = leaveHomeTime?.toKmpLocalTime()
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
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    // O2: Offline-Status für Hinweis-Banner
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val offlineWriteHint by viewModel.offlineWriteHint.collectAsStateWithLifecycle()
    val globalBufferMinutes by viewModel.globalBufferMinutes.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

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
    val initialSelectedDay = remember(memberId, initialDayProfiles) {
        val today = java.time.LocalDate.now().dayOfWeek.value
        var targetDay = today
        if (initialDayProfiles[today]?.isActive != true) {
            for (offset in 1..6) {
                val checkDay = (today - 1 + offset) % 7 + 1
                if (initialDayProfiles[checkDay]?.isActive == true) {
                    targetDay = checkDay
                    break
                }
            }
        }
        targetDay
    }
    var selectedDay by remember(memberId) { mutableStateOf(initialSelectedDay) }
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
 
    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundGradient)
        // Tastatur schließen, wenn der User außerhalb des Namensfeldes tippt
        .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {
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
                            earliestWakeUp = refProfile?.earliestWakeUp ?: LocalTime.of(6, 0).toKmpLocalTime(),
                            latestWakeUp = refProfile?.latestWakeUp ?: LocalTime.of(7, 30).toKmpLocalTime(),
                            bathroomDurationMinutes = refProfile?.bathroomDurationMinutes ?: 20L,
                            wantsBreakfast = refProfile?.wantsBreakfast ?: true,
                            leaveHomeTime = refProfile?.leaveHomeTime,
                            isPaused = memberToEdit?.isPaused ?: false,
                            // Sicherheitsnetz: Falls Room den Claim noch nicht hat (Stale-Cache),
                            // aber memberId == myMemberId, nehmen wir die UID aus dem Auth-State.
                            // Verhindert, dass beim Speichern claimedByUserId auf null überschrieben
                            // wird und die Firestore Security Rule PERMISSION DENIED zurückgibt.
                            claimedByUserId = memberToEdit?.claimedByUserId
                                ?: if (memberId != null && memberId == myMemberId) viewModel.currentUserId else null,
                            claimedByUserName = memberToEdit?.claimedByUserName
                                ?: if (memberId != null && memberId == myMemberId) viewModel.auth.currentUser?.displayName else null,
                            claimedByDeviceId = memberToEdit?.claimedByDeviceId
                                ?: if (memberId != null && memberId == myMemberId) viewModel.appSettings.deviceId else null,
                            createdAt = memberToEdit?.createdAt ?: System.currentTimeMillis(),
                            // Neuer Member → ans Ende stellen (members.size = nächster freier Index).
                            // Bestehender Member → sequenceOrder beibehalten, kein Reset durch Update.
                            sequenceOrder = memberToEdit?.sequenceOrder ?: members.size,
                            dayProfiles = dayProfiles,
                            isSimpleMode = refProfile?.isSimpleMode ?: false
                        )
                        viewModel.addOrUpdateMember(memberToSave)
                        
                        // Intelligenten Review-Prompt prüfen
                        context.findActivity()?.let {
                            viewModel.checkAndShowReview(it)
                        }
                        
                        onNavigateBack()
                    },
                    enabled = name.isNotBlank() && !hasAnyValidationError
                ) {
                    Text(stringResource(R.string.add_member_submit))
                }
            }
        ) { padding ->
            val scrollState = rememberScrollState()

            // Scroll-Indicator: sichtbar solange noch Inhalt unter dem sichtbaren Bereich liegt
            val infiniteTransition = rememberInfiniteTransition(label = "scrollHint")
            val scrollHintBounce by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(600, easing = LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "scrollHintBounce"
            )

            Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // O2: Offline-Hinweis – Änderungen werden nach Reconnect synchronisiert
                androidx.compose.animation.AnimatedVisibility(visible = isOffline || offlineWriteHint != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("☁️", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = offlineWriteHint?.asString()
                                    ?: stringResource(R.string.offline_write_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            if (offlineWriteHint != null) {
                                androidx.compose.material3.IconButton(
                                    onClick = { viewModel.clearOfflineWriteHint() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text("✕", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                    }
                }

                // Fehlermeldung (falls vorhanden)
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
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

                // Tooltip + Copy-Link als kompakte Einheit (Sub-Column vermeidet 16dp-Abstand der Parent-Column)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Tooltip G – Wochentage
                    if (tooltipsEnabled && !tooltipWeekdaysSeen) {
                        TooltipBubble(
                            visible = true,
                            text = stringResource(R.string.tooltip_weekdays),
                            onDismiss = { viewModel.markTooltipSeen(viewModel.tooltipKeyWeekdays) }
                        )
                    }

                    // Copy-Link
                    TextButton(
                        onClick = { showCopyDialog = true },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.add_member_copy_to_days, dayLabel(selectedDay)))
                    }
                }

                // Tages-Card für selectedDay
                val selectedProfile = dayProfiles[selectedDay] ?: DayProfile()
                DayProfileCard(
                    dayLabel = dayLabel(selectedDay),
                    profile = selectedProfile,
                    onProfileChange = { updated ->
                        dayProfiles = dayProfiles.toMutableMap().apply { put(selectedDay, updated) }
                    },
                    globalBufferMinutes = globalBufferMinutes,
                    showTooltipWakeWindow = tooltipsEnabled && !tooltipWakeWindowSeen,
                    onDismissTooltipWakeWindow = { viewModel.markTooltipSeen(viewModel.tooltipKeyWakeWindow) },
                    showTooltipBathroom = tooltipsEnabled && !tooltipBathroomSeen,
                    onDismissTooltipBathroom = { viewModel.markTooltipSeen(viewModel.tooltipKeyBathroom) }
                )

                Spacer(modifier = Modifier.height(8.dp))
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
                        .padding(bottom = 88.dp) // über dem Save-Button
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
    }
}

/**
 * Validiert ein DayProfile auf sinnvolle Zeitkombinationen.
 * Gibt eine Liste von Fehlermeldungs-Ressourcen-IDs zurück (leer = gültig).
 */
private fun validateDayProfile(profile: DayProfile): List<Int> {
    if (profile.isSimpleMode) return emptyList()
    val errors = mutableListOf<Int>()
    // 1. latestWakeUp muss NACH earliestWakeUp liegen
    if (profile.latestWakeUp <= profile.earliestWakeUp) {
        errors.add(R.string.validation_latest_before_earliest)
    }
    // 2. leaveHomeTime (effektiv: gesetzter Wert oder UI-Default 08:00)
    //    muss NACH latestWakeUp + Baddauer liegen
    val effectiveLeaveTime = profile.leaveHomeTime?.toJavaLocalTime() ?: java.time.LocalTime.of(8, 0)
    val latestBathroomEnd = profile.latestWakeUp.toJavaLocalTime().plusMinutes(profile.bathroomDurationMinutes)
    if (effectiveLeaveTime.isBefore(latestBathroomEnd)) {
        errors.add(R.string.validation_leave_too_early)
    }
    return errors
}

@Composable
private fun DayProfileCard(
    dayLabel: String,
    profile: DayProfile,
    onProfileChange: (DayProfile) -> Unit,
    globalBufferMinutes: Long = 0L,
    showTooltipWakeWindow: Boolean = false,
    onDismissTooltipWakeWindow: () -> Unit = {},
    showTooltipBathroom: Boolean = false,
    onDismissTooltipBathroom: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDarkTheme = LocalDarkTheme.current
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val formatter = DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a")
    val errors = if (profile.isActive) validateDayProfile(profile) else emptyList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        // Dark: tonal (kein Schatten), Light: 2dp Schatten für Tiefe
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
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

                    // Einfacher Modus Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.simple_mode_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.simple_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = profile.isSimpleMode,
                            onCheckedChange = { isSimple ->
                                onProfileChange(
                                    profile.copy(
                                        isSimpleMode = isSimple,
                                        bathroomDurationMinutes = if (isSimple) 0L else 20L,
                                        wantsBreakfast = if (isSimple) false else profile.wantsBreakfast,
                                        earliestWakeUp = if (isSimple) profile.latestWakeUp else profile.earliestWakeUp,
                                        bufferMinutes = if (isSimple) null else profile.bufferMinutes,
                                        leaveHomeTime = if (isSimple) null else profile.leaveHomeTime
                                    )
                                )
                            }
                        )
                    }

                    HorizontalDivider()

                    if (profile.isSimpleMode) {
                        // Nur die Weckzeit (latestWakeUp) anzeigen
                        TimePickerRowWithIcon(
                            icon = Icons.Default.AccessAlarm,
                            label = stringResource(R.string.add_member_latest_wake),
                            time = profile.latestWakeUp.toJavaLocalTime(),
                            context = context,
                            formatter = formatter,
                            onTimeSelected = {
                                onProfileChange(
                                    profile.copy(
                                        latestWakeUp = it.toKmpLocalTime(),
                                        earliestWakeUp = it.toKmpLocalTime()
                                    )
                                )
                            }
                        )
                    } else {
                        val latestBeforeEarliestError = profile.latestWakeUp <= profile.earliestWakeUp

                        // Früheste Weckzeit
                        TimePickerRowWithIcon(
                            icon = Icons.Default.AccessAlarm,
                            label = stringResource(R.string.add_member_earliest_wake),
                            time = profile.earliestWakeUp.toJavaLocalTime(),
                            context = context,
                            formatter = formatter,
                            onTimeSelected = { onProfileChange(profile.copy(earliestWakeUp = it.toKmpLocalTime())) }
                        )

                        // Späteste Weckzeit
                        TimePickerRowWithIcon(
                            icon = Icons.Default.AccessAlarm,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bathtub,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.add_member_bathroom_duration),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
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

                        // Puffer nach Bad (individueller Override)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.buffer_after_bath),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val effectiveValue = profile.bufferMinutes ?: globalBufferMinutes
                                IconButton(
                                    onClick = {
                                        val newVal = effectiveValue - 5
                                        when {
                                            newVal < 0 -> {} // Minimum erreicht
                                            newVal == globalBufferMinutes -> onProfileChange(profile.copy(bufferMinutes = null))
                                            else -> onProfileChange(profile.copy(bufferMinutes = newVal))
                                        }
                                    },
                                    enabled = effectiveValue > 0
                                ) { Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                                Text(
                                    "$effectiveValue min",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (profile.bufferMinutes != null) FontWeight.SemiBold else FontWeight.Normal,
                                    fontStyle = if (profile.bufferMinutes == null) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(
                                    onClick = {
                                        val newVal = effectiveValue + 5
                                        when {
                                            newVal > 15 -> {} // Maximum erreicht
                                            newVal == globalBufferMinutes -> onProfileChange(profile.copy(bufferMinutes = null))
                                            else -> onProfileChange(profile.copy(bufferMinutes = newVal))
                                        }
                                    },
                                    enabled = effectiveValue < 15
                                ) { Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                            }
                        }

                        // Abfahrtszeit
                        val effectiveLeaveTime = profile.leaveHomeTime?.toJavaLocalTime() ?: java.time.LocalTime.of(8, 0)
                        val leaveTooEarlyError =
                            effectiveLeaveTime.isBefore(profile.latestWakeUp.toJavaLocalTime().plusMinutes(profile.bathroomDurationMinutes))

                        TimePickerRowWithIcon(
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
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

                        // Frühstück (zuletzt – optional, weniger kritisch)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FreeBreakfast,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_member_wants_breakfast), style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = profile.wantsBreakfast,
                                onCheckedChange = { onProfileChange(profile.copy(wantsBreakfast = it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerRow(
    label: String,
    time: LocalTime,
    context: android.content.Context,
    formatter: DateTimeFormatter,
    onTimeSelected: (LocalTime) -> Unit,
    isError: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    // Dialog wird nur gerendert wenn sichtbar – State wird damit jedes Mal frisch
    // mit den aktuellen Zeitwerten initialisiert (verhindert veraltete Vorauswahl)
    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
            // TimeInput = Tastatur-Eingabe, kein Uhrzeiger → kein Auto-Sprung zur Minute
            text = { TimeInput(state = pickerState) }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        // Kein SpaceBetween mehr – Label mit weight() drängt den Button nach rechts
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // weight(1f): Label bekommt den verfügbaren Platz und bricht bei langen
        // Übersetzungen um – verhindert, dass die Uhrzeit rechts gequetscht wird
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        // wrapContentWidth: Uhrzeit behält ihre natürliche Breite und bricht nie um
        TextButton(
            onClick = { showPicker = true },
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                time.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                softWrap = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerRowWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    time: LocalTime,
    context: android.content.Context,
    formatter: DateTimeFormatter,
    onTimeSelected: (LocalTime) -> Unit,
    isError: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
            text = { TimeInput(state = pickerState) }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        TextButton(
            onClick = { showPicker = true },
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                time.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                softWrap = false
            )
        }
    }
}

// TimePickerRow benötigt für Rückwärtskompatibilität noch als top-level Funktion
@Composable
fun TimePickerRow(label: String, time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a")
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
