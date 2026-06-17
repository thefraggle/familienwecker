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
import androidx.compose.foundation.border
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import java.time.format.DateTimeFormatter
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
import de.familienwecker.famwake.model.SnoozeConfig
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.ui.components.EmptyState
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.ui.viewmodel.*
import de.familienwecker.famwake.util.BatteryUtils
import de.familienwecker.famwake.util.findActivity
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.ui.components.TooltipBubble
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import de.familienwecker.famwake.ui.theme.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition


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
    val isAwakeTodayLocal by viewModel.isAwakeTodayLocal.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val snoozeUntil by viewModel.snoozeUntil.collectAsStateWithLifecycle()
    val snoozeCount by viewModel.snoozeCount.collectAsStateWithLifecycle()
    val tooltipsEnabled by viewModel.tooltipsEnabled.collectAsStateWithLifecycle()
    val tooltipAwakeSeen by viewModel.tooltipAwakeSeen.collectAsStateWithLifecycle()
    val tooltipDragSeen by viewModel.tooltipDragSeen.collectAsStateWithLifecycle()
    val tooltipSwitchSeen by viewModel.tooltipSwitchSeen.collectAsStateWithLifecycle()
    val isJoiningFamily by viewModel.isJoiningFamily.collectAsStateWithLifecycle()
    val pendingPauseIds by viewModel.pendingPauseIds.collectAsStateWithLifecycle()
    val isAutoClaimInProgress by viewModel.isAutoClaimInProgress.collectAsStateWithLifecycle()
    val globalBufferMinutes by viewModel.globalBufferMinutes.collectAsStateWithLifecycle()
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsStateWithLifecycle()
    val deviceSchedule by viewModel.deviceSchedule.collectAsStateWithLifecycle()

    var showDeleteMemberDialog by remember { mutableStateOf<FamilyMember?>(null) }
    var pendingReorder by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val isExactAlarmPermitted = remember { mutableStateOf(de.familienwecker.famwake.util.AlarmPermissionUtils.hasExactAlarmPermission(context)) }
    val isFullScreenIntentPermitted = remember { mutableStateOf(de.familienwecker.famwake.util.AlarmPermissionUtils.hasFullScreenIntentPermission(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isExactAlarmPermitted.value = de.familienwecker.famwake.util.AlarmPermissionUtils.hasExactAlarmPermission(context)
                isFullScreenIntentPermitted.value = de.familienwecker.famwake.util.AlarmPermissionUtils.hasFullScreenIntentPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(familyId, isSyncing) {
        // Guard: onLeaveFamily nur wenn familyId null UND kein aktiver Sync läuft.
        // Verhindert Race Condition: direkt nach createFamily() ist isSyncing kurz true
        // während familyId noch nicht im StateFlow propagiert ist.
        if (familyId == null && !isSyncing) {
            onLeaveFamily()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val joiningText = stringResource(R.string.join_loading_text)
    LaunchedEffect(isJoiningFamily) {
        if (isJoiningFamily) {
            snackbarHostState.showSnackbar(
                message = joiningText,
                duration = SnackbarDuration.Indefinite
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
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
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormatter = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a") }

    // Scroll-Verhalten für LargeTopAppBar: Titel kollabiert beim Scrollen nach oben
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                val memberLimitReached = members.size >= 6
                val currentJoinCode by viewModel.joinCode.collectAsStateWithLifecycle()
                val isAnonymous = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true

                Column(horizontalAlignment = Alignment.End) {
                    // Share-FAB: Familie teilen (nur für eingeloggte User)
                    if (currentJoinCode != null && !isAnonymous) {
                        SmallFloatingActionButton(
                            onClick = {
                                val shareText = context.getString(R.string.settings_share_message, currentFamilyName ?: "", currentJoinCode ?: "")
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.settings_share_code))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Haupt-FAB: Mitglied hinzufügen
                    if (!memberLimitReached) {
                        ExtendedFloatingActionButton(
                            onClick = onNavigateToAddMember,
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.main_add_member_desc)) },
                            expanded = members.isEmpty(),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            topBar = {
                LargeTopAppBar(
                    title = {
                        val appShortName = stringResource(R.string.app_name_short)
                        Text(
                            buildAnnotatedString {
                                val prefix = "FamWake"
                                val suffix = appShortName.removePrefix(prefix).trim()
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append(prefix)
                                }
                                if (suffix.isNotEmpty()) {
                                    withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Normal)) {
                                        append(" $suffix")
                                    }
                                }
                            }
                        )
                    },
                    actions = {
                        val syncRotationTransition = rememberInfiniteTransition(label = "syncRotation")
                        val syncRotation by syncRotationTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "syncRotationAngle"
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
                        } else {
                            AnimatedVisibility(
                                visible = isSyncing,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(modifier = Modifier.padding(end = 4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer { rotationZ = syncRotation }
                                    )
                                }
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
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.Transparent,
                        scrolledContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { padding ->
            val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()


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
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Exakter Alarm Warnung (Android 14)
                item {
                    val hasActiveSchedule = members.any { !it.isPaused }
                    ExactAlarmWarningBanner(
                        isVisible = !isExactAlarmPermitted.value && isAlarmEnabled && hasActiveSchedule && myMemberId != null,
                        isDarkTheme = isDarkTheme,
                        onRequestPermission = { de.familienwecker.famwake.util.AlarmPermissionUtils.requestExactAlarmPermission(context) }
                    )
                }
                
                // Full Screen Intent Warnung (Android 14+) - wird erst gezeigt, wenn Exact Alarm erlaubt ist
                item {
                    val hasActiveSchedule = members.any { !it.isPaused }
                    FullScreenIntentWarningBanner(
                        isVisible = isExactAlarmPermitted.value && !isFullScreenIntentPermitted.value && isAlarmEnabled && hasActiveSchedule && myMemberId != null,
                        isDarkTheme = isDarkTheme,
                        onRequestPermission = { de.familienwecker.famwake.util.AlarmPermissionUtils.requestFullScreenIntentPermission(context) }
                    )
                }
                
                // Fehlermeldung (falls vorhanden)
                item {
                    ErrorMessageBanner(
                        errorMessage = errorMessage,
                        isDarkTheme = isDarkTheme,
                        onClearError = { viewModel.clearError() },
                        onLeaveFamily = onLeaveFamily
                    )
                }
                


                // 0b. Wecker Ein/Aus Schalter
                item {
                    AlarmToggleSection(
                        viewModel = viewModel,
                        context = context,
                        isDarkTheme = isDarkTheme,
                        isAlarmEnabled = isAlarmEnabled,
                        tooltipsEnabled = tooltipsEnabled,
                        tooltipSwitchSeen = tooltipSwitchSeen,
                        tooltipAwakeSeen = tooltipAwakeSeen,
                        myMemberId = myMemberId,
                        members = members,
                        deviceSchedule = deviceSchedule,
                        isAwakeTodayLocal = isAwakeTodayLocal
                    )
                }

                item {
                    SnoozeBanner(
                        snoozeUntil = snoozeUntil,
                        snoozeCount = snoozeCount,
                        myMemberId = myMemberId,
                        isDarkTheme = isDarkTheme,
                        timeFormatter = timeFormatter,
                        onCancelSnooze = { myMemberId?.let { viewModel.cancelSnooze(it) } }
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = myMemberId == null && members.isNotEmpty() && !isAutoClaimInProgress,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 2000)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 200))
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSettings() },
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
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
                scheduleSection(
                    schedule = schedule,
                    viewModel = viewModel,
                    context = context,
                    isDarkTheme = isDarkTheme,
                    isAlarmEnabled = isAlarmEnabled,
                    myMemberId = myMemberId,
                    tooltipsEnabled = tooltipsEnabled,
                    tooltipDragSeen = tooltipDragSeen,
                    selectedDayOfWeek = selectedDayOfWeek,
                    draggedItemId = draggedItemId,
                    setDraggedItemId = { draggedItemId = it },
                    draggingOffset = draggingOffset,
                    setDraggingOffset = { draggingOffset = it },
                    setPendingReorder = { pendingReorder = it },
                    itemHeightPx = itemHeightPx,
                    timeFormatter = timeFormatter
                )

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // Liste der Familienmitglieder
                memberSection(
                    members = members,
                    myMemberId = myMemberId,
                    isDarkTheme = isDarkTheme,
                    isAlarmEnabled = isAlarmEnabled,
                    globalBufferMinutes = globalBufferMinutes,
                    pendingPauseIds = pendingPauseIds,
                    viewModel = viewModel,
                    onNavigateToEditMember = onNavigateToEditMember,
                    showDeleteMemberDialog = { showDeleteMemberDialog = it }
                )
            }

            } // end Box
        }
    }

    if (showDeleteMemberDialog != null) {
        showDeleteMemberDialog?.let { member ->
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
    }

    if (pendingReorder != null) {
        pendingReorder?.let { (fromIdx, toIdx) ->
            val targetDate = schedule?.targetDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
            val dayOfWeekNum = selectedDayOfWeek ?: targetDate.dayOfWeek.value
            val dayNameRes = when (dayOfWeekNum) {
                1 -> R.string.weekday_1
                2 -> R.string.weekday_2
                3 -> R.string.weekday_3
                4 -> R.string.weekday_4
                5 -> R.string.weekday_5
                6 -> R.string.weekday_6
                7 -> R.string.weekday_7
                else -> R.string.weekday_1
            }
            val dayName = stringResource(dayNameRes)

            AlertDialog(
                onDismissRequest = { pendingReorder = null },
                title = { Text(text = stringResource(R.string.reorder_dialog_title)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.reorder_dialog_message, dayName),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.moveMemberOrder(fromIdx, toIdx, wholeWeek = false)
                                viewModel.saveMemberOrder()
                                pendingReorder = null
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(text = stringResource(R.string.reorder_dialog_today, dayName))
                        }
                        Button(
                            onClick = {
                                viewModel.moveMemberOrder(fromIdx, toIdx, wholeWeek = true)
                                viewModel.saveMemberOrder()
                                pendingReorder = null
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(text = stringResource(R.string.reorder_dialog_week))
                        }
                        OutlinedButton(
                            onClick = { pendingReorder = null },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(text = stringResource(R.string.cancel_button))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }
    }

    if (pendingJoinCode != null && familyId != null) {
        var isJoining by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isJoining) viewModel.clearPendingJoinCode() },
            title = { Text(stringResource(R.string.join_conflict_title)) },
            text = { Text(stringResource(R.string.join_conflict_text, currentFamilyName ?: "---", pendingJoinCode ?: "")) },
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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

// ─── Private Composables ──────────────────────────────────────────────────────

@Composable
private fun ExactAlarmWarningBanner(
    isVisible: Boolean,
    isDarkTheme: Boolean,
    onRequestPermission: () -> Unit
) {
    if (!isVisible) return
    val cardColor = if (isDarkTheme) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.errorContainer
    val textColor = MaterialTheme.colorScheme.onErrorContainer
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onRequestPermission() },
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.main_exact_alarm_banner),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun FullScreenIntentWarningBanner(
    isVisible: Boolean,
    isDarkTheme: Boolean,
    onRequestPermission: () -> Unit
) {
    if (!isVisible) return
    val cardColor = if (isDarkTheme) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.errorContainer
    val textColor = MaterialTheme.colorScheme.onErrorContainer
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onRequestPermission() },
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.permission_fullscreen_title) + " - " + stringResource(R.string.permission_fullscreen_message),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ErrorMessageBanner(
    errorMessage: de.familienwecker.famwake.ui.util.UiText?,
    isDarkTheme: Boolean,
    onClearError: () -> Unit,
    onLeaveFamily: () -> Unit
) {
    errorMessage?.let { error ->
        val errorString = error.asString()
        val isJoinError = errorString.contains(stringResource(R.string.error_family_not_found)) ||
                          errorString.contains(stringResource(R.string.error_invalid_code))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                 else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⚠️ $errorString", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    if (isJoinError) {
                        TextButton(onClick = onClearError) { Text(stringResource(R.string.cancel_button)) }
                    } else {
                        TextButton(
                            onClick = onLeaveFamily,
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
    }
}

@Composable
private fun SnoozeBanner(
    snoozeUntil: java.time.LocalDateTime?,
    snoozeCount: Int,
    myMemberId: String?,
    isDarkTheme: Boolean,
    timeFormatter: java.time.format.DateTimeFormatter,
    onCancelSnooze: () -> Unit
) {
    AnimatedVisibility(
        visible = snoozeUntil != null && myMemberId != null,
        enter = androidx.compose.animation.expandVertically() + fadeIn(),
        exit = androidx.compose.animation.shrinkVertically() + fadeOut()
    ) {
        snoozeUntil?.let { snoozeTime ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) de.familienwecker.famwake.ui.theme.OnlineGreenDark
                                     else de.familienwecker.famwake.ui.theme.OnlineGreenLight
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Snooze, contentDescription = null,
                            tint = if (isDarkTheme) de.familienwecker.famwake.ui.theme.OnlineIconDark
                                   else de.familienwecker.famwake.ui.theme.OnlineIconLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${stringResource(R.string.main_snooze_active, snoozeTime.toLocalTime().format(timeFormatter))} ($snoozeCount/${SnoozeConfig.MAX_SNOOZE_COUNT})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) de.familienwecker.famwake.ui.theme.OnlineGreenLight
                                    else Color(0xFF1B5E20)
                        )
                    }
                    TextButton(
                        onClick = onCancelSnooze,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDarkTheme) de.familienwecker.famwake.ui.theme.OnlineIconDark
                                           else de.familienwecker.famwake.ui.theme.OnlineIconLight
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.cancel_button), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun UnclaimedWarningBanner(memberName: String, isDarkTheme: Boolean) {
    val cardColor = if (isDarkTheme) de.familienwecker.famwake.ui.theme.SnoozeAmberDark
                    else de.familienwecker.famwake.ui.theme.SnoozeAmberLight
    val textColor = if (isDarkTheme) de.familienwecker.famwake.ui.theme.SnoozeTextDark
                    else de.familienwecker.famwake.ui.theme.SnoozeTextLight
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚠️ " + stringResource(R.string.main_unclaimed_first_title, memberName),
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.main_unclaimed_first_desc, memberName),
                style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.85f)
            )
        }
    }
}
