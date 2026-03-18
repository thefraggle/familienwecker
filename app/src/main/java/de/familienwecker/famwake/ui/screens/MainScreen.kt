package de.familienwecker.famwake.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.alpha
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.ui.components.EmptyState
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.util.BatteryUtils
import de.familienwecker.famwake.model.FamilySchedule
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FamilyViewModel,
    onNavigateToAddMember: () -> Unit,
    onNavigateToEditMember: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    onLeaveFamily: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
    val pendingJoinCode by viewModel.pendingJoinCode.collectAsStateWithLifecycle()
    val currentFamilyName by viewModel.familyName.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val snoozeUntil by viewModel.snoozeUntil.collectAsStateWithLifecycle()

    var showDeleteMemberDialog by remember { mutableStateOf<FamilyMember?>(null) }
    var showJoinConflictDialog by remember { mutableStateOf(false) }

    // Sofort Conflict-Dialog zeigen wenn ein pendingJoinCode eintrifft (z.B. nach Deep-Link aus Hintergrund)
    LaunchedEffect(pendingJoinCode, familyId) {
        if (pendingJoinCode != null && familyId != null) {
            showJoinConflictDialog = true
        }
    }

    if (showJoinConflictDialog && pendingJoinCode != null) {
        AlertDialog(
            onDismissRequest = {
                showJoinConflictDialog = false
                viewModel.clearPendingJoinCode()
            },
            title = { Text(stringResource(R.string.join_conflict_title)) },
            text = { Text(stringResource(R.string.join_conflict_text, currentFamilyName ?: "", pendingJoinCode ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    showJoinConflictDialog = false
                    viewModel.leaveAndJoinPendingCode { success ->
                        if (success) onLeaveFamily()
                        // success=false: Dialog geschlossen, pendingCode gecleard, User bleibt in Familie
                    }
                }) {
                    Text(stringResource(R.string.join_conflict_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showJoinConflictDialog = false
                    viewModel.clearPendingJoinCode()
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(familyId, isSyncing) {
        // Guard: onLeaveFamily nur wenn familyId null UND kein aktiver Sync läuft.
        // Verhindert Race Condition: direkt nach createFamily() ist isSyncing kurz true
        // während familyId noch nicht im StateFlow propagiert ist.
        if (familyId == null && !isSyncing) {
            onLeaveFamily()
        }
    }

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

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        val appShortName = stringResource(R.string.app_name_short)
                        Text(
                            buildAnnotatedString {
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("FamWake")
                                }
                                append(" ")
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Normal)) {
                                    append(appShortName)
                                }
                            }
                        )
                    },
                    actions = {
                        val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
                        val isSyncingAnim by animateFloatAsState(
                            targetValue = if (syncStatus.hasPendingWrites) 1f else 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "syncAnimation"
                        )

                        if (isOffline) {
                            Box(modifier = Modifier.padding(end = 4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else if (syncStatus.hasPendingWrites) {
                            // Pending Writes: Sync-Icon rotierend anzeigen (nur wenn online)
                            Box(modifier = Modifier.padding(end = 4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = isSyncingAnim * 360f }
                                )
                            }
                        }

                        val settingsInteractionSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.bounceClick(settingsInteractionSource),
                            interactionSource = settingsInteractionSource
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

            // Scroll-Indicator: nur wenn members leer und noch nicht gescrollt
            val showScrollHint = members.isEmpty() &&
                lazyListState.firstVisibleItemIndex == 0 &&
                lazyListState.firstVisibleItemScrollOffset == 0

            // Echter infinite Bounce – muss rememberInfiniteTransition verwenden
            val infiniteTransition = rememberInfiniteTransition(label = "scrollHint")
            val scrollHintBounce by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scrollHintBounce"
            )

            Box(modifier = Modifier.fillMaxSize()) {
            val itemHeightPx = remember(lazyListState.layoutInfo) {
                lazyListState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key?.toString()?.startsWith("sched_") == true }
                    ?.size?.toFloat() ?: with(android.util.DisplayMetrics()) { 110f * (context.resources.displayMetrics.density) }
            }
            
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Fehlermeldung (falls vorhanden)
                item {
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                                 else MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⚠️ ${error.asString()}",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onLeaveFamily() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.settings_leave_family))
                                }
                            }
                        }
                    }
                }
                


                // 0b. Wecker Ein/Aus Schalter
                item {
                    val toggleCardColor by animateColorAsState(
                        targetValue = if (isDarkTheme) {
                            if (isAlarmEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        } else {
                            if (isAlarmEnabled) MaterialTheme.colorScheme.surface 
                            else MaterialTheme.colorScheme.surfaceVariant
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "toggleCardColor"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = toggleCardColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isAlarmEnabled) stringResource(R.string.main_alarm_enabled) else stringResource(R.string.main_alarm_disabled),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = if (isAlarmEnabled) stringResource(R.string.main_alarm_enabled_desc) else stringResource(R.string.main_alarm_disabled_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAlarmEnabled,
                                    onCheckedChange = { viewModel.setAlarmEnabled(it) },
                                    enabled = myMemberId != null
                                )
                            }

                            // "Ich bin wach" button for the current user
                            if (myMemberId != null) {
                                val myMember = members.find { it.id == myMemberId }
                                if (myMember != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val awakeInteractionSource = remember { MutableInteractionSource() }
                                    
                                    Button(
                                        onClick = { viewModel.toggleAwakeMember(myMemberId!!) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .bounceClick(awakeInteractionSource),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                        interactionSource = awakeInteractionSource,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (myMember.isAwakeToday) 
                                                MaterialTheme.colorScheme.secondary 
                                            else 
                                                MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.awake_today_desc),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Snooze-Banner (sichtbar wenn Snooze aktiv)
                item {
                    AnimatedVisibility(
                        visible = snoozeUntil != null && myMemberId != null,
                        enter = androidx.compose.animation.expandVertically() + fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + fadeOut()
                    ) {
                        snoozeUntil?.let { snoozeTime ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkTheme) Color(0xFF1B321B) else Color(0xFFE8F5E9)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Snooze,
                                            contentDescription = null,
                                            tint = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.main_snooze_active,
                                                snoozeTime.toLocalTime().toString().substring(0, 5)
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDarkTheme) Color(0xFFE8F5E9) else Color(0xFF1B5E20)
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.cancelSnooze(myMemberId!!) },
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
                                        )
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.cancel_button),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Fallback: Warnung wenn kein Profil ausgewÃ¤hlt ist (nur wenn Mitglieder vorhanden sind)
                item {
                    if (myMemberId == null && members.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSettings() },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                                 else MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⚠️ " + stringResource(R.string.main_no_profile_warning),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.main_no_profile_warning_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Errechneter Wecker-Plan
                item {
                    Text(stringResource(R.string.main_current_schedule), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                
                item {
                    val currentSchedule = schedule
                    val planCardColor by animateColorAsState(
                        targetValue = if (isDarkTheme) {
                            if (isAlarmEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        } else {
                            if (isAlarmEnabled) MaterialTheme.colorScheme.surface 
                            else MaterialTheme.colorScheme.surfaceVariant
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "planCardColor"
                    )

                    if (currentSchedule == null || currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.NoActiveSchedule) {
                        EmptyState(
                            lottieRes = R.raw.mond,
                            title = stringResource(R.string.empty_schedule_title),
                            description = stringResource(R.string.empty_schedule_description)
                        )
                    } else if (!currentSchedule.isValid) {
                        Card(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                                 else MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "❌ " + viewModel.scheduleMessageToUiText(currentSchedule.scheduleMessage).asString(),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        Card(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = planCardColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isAlarmEnabled) "✅ " + stringResource(R.string.main_optimal_plan) else "⏸️ " + stringResource(R.string.main_plan_paused), 
                                    fontWeight = FontWeight.Bold
                                )
                                // Flexible Anpassungsmeldung aus ScheduleMessage anzeigen
                                val msgText = viewModel.scheduleMessageToUiText(currentSchedule.scheduleMessage).asString()
                                val isFlexibleAdjustment = currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.TimeAdjusted ||
                                    currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BreakfastReduced ||
                                    currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BreakfastAndTimeAdjusted
                                if (isFlexibleAdjustment) {
                                    Text(
                                        text = "⚠️ $msgText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                currentSchedule.breakfastTime?.let {
                                    Text(text = "☕ " + stringResource(R.string.main_shared_breakfast, it.toString()), modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }

                // 1b. Die verschiebbaren Weckzeiten-Kacheln (Drag & Drop)
                val currentSched = schedule
                if (currentSched != null && currentSched.isValid && currentSched.memberSchedules.isNotEmpty()) {
                    val totalItems = currentSched.memberSchedules.size

                    itemsIndexed(
                        items = currentSched.memberSchedules,
                        key = { _, s -> "sched_${s.member.id}" }
                    ) { index, sched ->
                        val isDragging = draggedItemId == sched.member.id
                        
                        // Berechnung des Versatzes für andere Kacheln während des Drags (Gap-Preview)
                        val otherItemTranslationY by animateFloatAsState(
                            targetValue = if (draggedItemId != null && !isDragging) {
                                val draggedIdx = currentSched.memberSchedules.indexOfFirst { it.member.id == draggedItemId }
                                if (draggedIdx != -1) {
                                    val offsetItems = (draggingOffset / itemHeightPx).roundToInt()
                                    val targetIdx = (draggedIdx + offsetItems).coerceIn(0, totalItems - 1)
                                    
                                    if (draggedIdx < targetIdx && index > draggedIdx && index <= targetIdx) {
                                        -itemHeightPx
                                    } else if (draggedIdx > targetIdx && index < draggedIdx && index >= targetIdx) {
                                        itemHeightPx
                                    } else 0f
                                } else 0f
                            } else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "gapAnimation"
                        )

                        val cardBgColor by animateColorAsState(
                            targetValue = if (isDragging) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            } else if (isDarkTheme) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "dragCardBgColor"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isDragging) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "dragContentColor"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) draggingOffset else otherItemTranslationY
                                    scaleX = if (isDragging) 1.08f else 1f
                                    scaleY = if (isDragging) 1.08f else 1f
                                }
                                .pointerInput(currentSched.memberSchedules) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedItemId = sched.member.id },
                                        onDragEnd = {
                                            val offsetItems = (draggingOffset / itemHeightPx).roundToInt()
                                            val targetIdx = (index + offsetItems).coerceIn(0, totalItems - 1)
                                            if (targetIdx != index) {
                                                viewModel.moveMemberOrder(index, targetIdx)
                                                viewModel.saveMemberOrder()
                                            }
                                            draggedItemId = null
                                            draggingOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedItemId = null
                                            draggingOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggingOffset += dragAmount.y
                                        }
                                    )
                                },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 32.dp else 6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp, 
                                color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = cardBgColor,
                                contentColor = contentColor
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⏰ ${sched.wakeUpTime} - ${sched.member.name}", 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator, // Modernes Grip-Icon
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp).alpha(if (isDragging) 1.0f else 0.6f)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.main_schedule_bathroom, sched.bathroomStartTime.toString(), sched.bathroomEndTime.toString()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor.copy(alpha = if (isDragging) 0.9f else 0.7f)
                                )
                                if (sched.member.leaveHomeTime != null) {
                                    Text(
                                        text = stringResource(R.string.main_schedule_leave, sched.member.leaveHomeTime.toString()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor.copy(alpha = if (isDragging) 0.9f else 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // Liste der Familienmitglieder
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.main_family_members),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        val memberLimitReached = members.size >= 6
                        val addMemberInteractionSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = onNavigateToAddMember,
                            enabled = !memberLimitReached,
                            modifier = Modifier.bounceClick(addMemberInteractionSource),
                            interactionSource = addMemberInteractionSource
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.main_add_member_desc),
                                tint = if (memberLimitReached) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (members.size >= 6) {
                    item {
                        Text(
                            text = stringResource(R.string.main_member_limit_reached),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                if (members.isEmpty()) {
                    item {
                        EmptyState(
                            lottieRes = R.raw.family,
                            title = stringResource(R.string.empty_members_title),
                            description = stringResource(R.string.empty_members_description),
                            action = {
                                Button(onClick = onNavigateToAddMember) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.main_add_member_desc))
                                }
                            }
                        )
                    }
                }
                
                // Member Cards
                items(
                    items = members,
                    key = { it.id }
                ) { member ->
                    MemberCard(
                        member = member, 
                        myMemberId = myMemberId,
                        onEdit = { onNavigateToEditMember(member.id) },
                        onDelete = { showDeleteMemberDialog = member },
                        onTogglePause = { viewModel.togglePauseMember(member.id) },
                        onToggleAwake = { viewModel.toggleAwakeMember(member.id) },
                        isAlarmEnabled = isAlarmEnabled
                    )
                }
            }

            // Scroll-Indicator Overlay
            AnimatedVisibility(
                visible = showScrollHint,
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
                    // Pill-Hintergrund: Icon hebt sich klar vom Text ab
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

            } // end Box
        }
    }

    if (showDeleteMemberDialog != null) {
        val member = showDeleteMemberDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteMemberDialog = null },
            title = { Text(stringResource(R.string.delete_member_title)) },
            text = { Text(stringResource(R.string.delete_member_text, member.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(member.id)
                        showDeleteMemberDialog = null
                    }
                ) {
                    Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMemberDialog = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    if (pendingJoinCode != null && familyId != null) {
        var isJoining by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isJoining) viewModel.clearPendingJoinCode() },
            title = { Text(stringResource(R.string.join_conflict_title)) },
            text = { Text(stringResource(R.string.join_conflict_text, currentFamilyName ?: "---", pendingJoinCode!!)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isJoining) {
                            isJoining = true
                            viewModel.leaveAndJoinPendingCode { _ ->
                                isJoining = false
                                // Dialog schliesst sich automatisch da pendingJoinCode = null
                            }
                        }
                    },
                    enabled = !isJoining,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.join_conflict_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.clearPendingJoinCode() },
                    enabled = !isJoining
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
fun MemberCard(
    member: FamilyMember, 
    myMemberId: String?, 
    onEdit: () -> Unit, 
    onDelete: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleAwake: () -> Unit,
    isAlarmEnabled: Boolean
) {
    val isDarkTheme = LocalDarkTheme.current
    val backgroundColor = if (member.isPaused) {
        if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = if (member.isPaused)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    val isOtherUserClaim = member.claimedByUserId != null && member.id != myMemberId

    Card(
        onClick = { if (!isOtherUserClaim) onEdit() },
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1
                    )
                    
                    if (member.claimedByUserId != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        val statusText = when {
                            member.id == myMemberId -> when {
                                // Eigenes Mitglied: lokalen Alarm-Status verwenden
                                !isAlarmEnabled -> stringResource(R.string.main_member_alarm_off)
                                member.isPaused -> stringResource(R.string.main_member_alarm_off)
                                else -> stringResource(R.string.main_member_alarm_on)
                            }
                            else -> when {
                                // Fremdes Mitglied: Firestore-gesyncten Status des anderen Geräts verwenden
                                member.deviceAlarmEnabled == false -> stringResource(R.string.main_member_alarm_off)
                                member.isPaused -> stringResource(R.string.main_member_alarm_off)
                                else -> stringResource(R.string.main_member_alarm_on)
                            }
                        }
                        
                        val statusColor = when {
                            member.id == myMemberId -> if (!isAlarmEnabled || member.isPaused) MaterialTheme.colorScheme.error else textColor.copy(alpha = 0.7f)
                            else -> if (member.deviceAlarmEnabled == false || member.isPaused) MaterialTheme.colorScheme.error else textColor.copy(alpha = 0.7f)
                        }
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    if (member.isAwakeToday) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "☀️",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(stringResource(R.string.main_wake_time, member.earliestWakeUp.toString(), member.latestWakeUp.toString()), color = textColor)
                Text(stringResource(R.string.main_bathroom_info, member.bathroomDurationMinutes.toString(), if(member.wantsBreakfast) stringResource(R.string.yes) else stringResource(R.string.no)), color = textColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (member.claimedByUserId == null) {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.size(32.dp)
                    ) {
                        val icon = if (member.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(R.string.pause_today_desc),
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (!isOtherUserClaim) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.add_member_title_edit),
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (member.claimedByUserId == null || member.id == myMemberId) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_desc), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
