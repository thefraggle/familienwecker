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
import de.familienwecker.famwake.ui.theme.*
import androidx.compose.ui.input.nestedscroll.nestedScroll

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
    val tooltipsEnabled by viewModel.tooltipsEnabled.collectAsStateWithLifecycle()
    val tooltipAwakeSeen by viewModel.tooltipAwakeSeen.collectAsStateWithLifecycle()
    val tooltipDragSeen by viewModel.tooltipDragSeen.collectAsStateWithLifecycle()
    val tooltipSwitchSeen by viewModel.tooltipSwitchSeen.collectAsStateWithLifecycle()
    val isJoiningFamily by viewModel.isJoiningFamily.collectAsStateWithLifecycle()
    val pendingPauseIds by viewModel.pendingPauseIds.collectAsStateWithLifecycle()
    val isAutoClaimInProgress by viewModel.isAutoClaimInProgress.collectAsStateWithLifecycle()
    val globalBufferMinutes by viewModel.globalBufferMinutes.collectAsStateWithLifecycle()

    var showDeleteMemberDialog by remember { mutableStateOf<FamilyMember?>(null) }

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
    val timeFormatter = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "hh:mm a") }

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
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
                contentPadding = PaddingValues(16.dp),
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
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
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
                                    onCheckedChange = { 
                                        viewModel.setAlarmEnabled(it)
                                        // Intelligenten Review-Prompt prüfen
                                        context.findActivity()?.let { activity ->
                                            viewModel.checkAndShowReview(activity)
                                        }
                                    },
                                    enabled = myMemberId != null
                                )
                            }

                            // Tooltip F – Wecker-Switch
                            if (tooltipsEnabled && !tooltipSwitchSeen && myMemberId != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TooltipBubble(
                                    visible = true,
                                    text = stringResource(R.string.tooltip_alarm_switch),
                                    onDismiss = { viewModel.markTooltipSeen(viewModel.tooltipKeySwitch) },
                                    isDark = isDarkTheme
                                )
                            }

                            val myMember = members.find { it.id == myMemberId }
                            val isAwakeButtonVisible = remember(myMember, isAlarmEnabled, schedule, isAwakeTodayLocal) {
                                if (myMember == null || !isAlarmEnabled) return@remember false
                                if (isAwakeTodayLocal) return@remember true

                                val nowDt = java.time.LocalDateTime.now()
                                val todayDate = nowDt.toLocalDate()
                                
                                val nextActiveProfile = (0..6).mapNotNull { offset ->
                                    val date = todayDate.plusDays(offset.toLong())
                                    val dow = date.dayOfWeek.value
                                    val profile = myMember.dayProfiles?.get(dow)
                                    if (profile != null && profile.isActive) date to profile else null
                                }.firstOrNull { (date, profile) ->
                                    if (date == todayDate) nowDt.toLocalTime() < profile.latestWakeUp.toJavaLocalTime()
                                    else true
                                }
                                
                                if (nextActiveProfile != null) {
                                    val (targetDate, profile) = nextActiveProfile
                                    val myScheduledTime = schedule?.memberSchedules
                                        ?.find { it.member.id == myMemberId }
                                        ?.wakeUpTime?.toJavaLocalTime()
                                    val alarmTime = myScheduledTime ?: profile.earliestWakeUp.toJavaLocalTime()
                                    val targetDt = java.time.LocalDateTime.of(targetDate, alarmTime)
                                    val windowStart = targetDt.minusHours(2)
                                    nowDt >= windowStart && nowDt < targetDt
                                } else {
                                    false
                                }
                            }

                            // "Ich bin wach" button for the current user (only when alarm is enabled)
                            AnimatedVisibility(
                                visible = isAwakeButtonVisible,
                                enter = androidx.compose.animation.expandVertically() + fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + fadeOut()
                            ) {
                                if (myMember != null) {
                                    Column {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        val awakeInteractionSource = remember { MutableInteractionSource() }

                                        Button(
                                            onClick = { myMemberId?.let { viewModel.toggleAwakeMember(it) } },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .bounceClick(awakeInteractionSource),
                                            shape = MaterialTheme.shapes.medium,
                                            interactionSource = awakeInteractionSource,
                                            enabled = true,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAwakeTodayLocal)
                                                    MaterialTheme.colorScheme.secondary
                                                else
                                                    MaterialTheme.colorScheme.primary,
                                                contentColor = if (isAwakeTodayLocal)
                                                    MaterialTheme.colorScheme.onSecondary
                                                else
                                                    MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WbSunny,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (isAwakeTodayLocal) stringResource(R.string.awake_active_desc) else stringResource(R.string.awake_today_desc),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (isAwakeTodayLocal) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Tooltip A – "Bin schon wach"-Button
                                        if (tooltipsEnabled && !tooltipAwakeSeen) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TooltipBubble(
                                                visible = true,
                                                text = stringResource(R.string.tooltip_awake_button),
                                                onDismiss = { viewModel.markTooltipSeen(viewModel.tooltipKeyAwake) },
                                                isDark = isDarkTheme
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Snooze-Banner (sichtbar wenn Snooze aktiv)
                item {
                    SnoozeBanner(
                        snoozeUntil = snoozeUntil,
                        myMemberId = myMemberId,
                        isDarkTheme = isDarkTheme,
                        timeFormatter = timeFormatter,
                        onCancelSnooze = { myMemberId?.let { viewModel.cancelSnooze(it) } }
                    )
                }

                // Fallback-Warnung wenn kein Profil ausgewählt ist (nur wenn Mitglieder vorhanden und kein Auto-Claim läuft)
                // AnimatedVisibility mit delayMillis: erscheint erst nach 2s – damit kurze Auto-Claim-Phasen
                // (typisch <2s) keinen Flicker erzeugen.
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

                // Warnung: ungeclaimter Member an erster Stelle – nur zeigen wenn kein "Kein Profil"-Banner aktiv ist
                val firstScheduledMember = schedule?.memberSchedules?.firstOrNull()?.member
                if (myMemberId != null && firstScheduledMember != null
                    && firstScheduledMember.claimedByUserId == null
                    && firstScheduledMember.id != myMemberId) {
                    item(key = "unclaimed_first_warning") {
                        UnclaimedWarningBanner(
                            memberName = firstScheduledMember.name,
                            isDarkTheme = isDarkTheme
                        )
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
                        val cardColor = if (isDarkTheme) SnoozeAmberDark else SnoozeAmberLight // Warm orange/amber
                        val textColor = if (isDarkTheme) SnoozeTextDark else SnoozeTextLight
                        
                        Card(
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
                                        text = viewModel.scheduleMessageToUiText(currentSchedule.scheduleMessage).asString(),
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                                
                                val descKey = when (currentSchedule.scheduleMessage) {
                                    is de.familienwecker.famwake.model.ScheduleMessage.MemberConflict -> R.string.schedule_message_member_conflict_desc
                                    else -> R.string.schedule_message_no_valid_desc
                                }
                                Text(
                                    text = stringResource(descKey),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 4.dp, start = 28.dp)
                                )

                                // Fallback info for claimed user
                                val myMember = currentSchedule.memberSchedules.find { it.member.id == myMemberId }
                                if (myMember != null && isAlarmEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.1f)),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = stringResource(R.string.main_fallback_alarm_active, myMember.wakeUpTime.toJavaLocalTime().format(timeFormatter)),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            color = textColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.applyAutoFix() },
                                    modifier = Modifier.padding(start = 28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.schedule_auto_fix),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = planCardColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAlarmEnabled) Icons.Default.CheckCircle else Icons.Default.PauseCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isAlarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAlarmEnabled) stringResource(R.string.main_optimal_plan) else stringResource(R.string.main_plan_paused),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // Datum anzeigen wenn Schedule für einen zukünftigen Tag berechnet wurde
                                val scheduleTargetDate = currentSchedule.targetDate
                                val todayJava = java.time.LocalDate.now()
                                val targetJava = scheduleTargetDate?.let { java.time.LocalDate.of(it.year, it.monthNumber, it.dayOfMonth) }
                                if (targetJava != null && targetJava != todayJava) {
                                    // App-Locale aus den lokalisierten Resources – nicht Geräte-Locale
                                    val appLocale = context.resources.configuration.locales[0]
                                    val dayName = targetJava.dayOfWeek
                                        .getDisplayName(java.time.format.TextStyle.FULL, appLocale)
                                        .replaceFirstChar { it.uppercase() }
                                    val dateStr = targetJava.format(java.time.format.DateTimeFormatter.ofPattern("d. MMMM", appLocale))
                                    Text(
                                        text = "$dayName, $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                // Flexible Anpassungsmeldung aus ScheduleMessage anzeigen
                                val msgText = viewModel.scheduleMessageToUiText(currentSchedule.scheduleMessage).asString()
                                val isFlexibleAdjustment = currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.TimeAdjusted ||
                                    currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BreakfastReduced ||
                                    currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BreakfastAndTimeAdjusted ||
                                    currentSchedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BufferReduced
                                if (isFlexibleAdjustment) {
                                    Text(
                                        text = "⚠️ $msgText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                currentSchedule.breakfastTime?.let {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FreeBreakfast,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = stringResource(R.string.main_shared_breakfast, it.toJavaLocalTime().format(timeFormatter)))
                                    }
                                }
                            }
                        }
                    }
                }

                // 1b. Die verschiebbaren Weckzeiten-Kacheln (Drag & Drop)
                val currentSched = schedule
                if (currentSched != null && currentSched.isValid && currentSched.memberSchedules.isNotEmpty()) {
                    val totalItems = currentSched.memberSchedules.size

                    // Tooltip B – Drag-Handle (nur wenn >1 Mitglied)
                    if (totalItems > 1) {
                        item(key = "tooltip_drag") {
                            if (tooltipsEnabled && !tooltipDragSeen) {
                                TooltipBubble(
                                    visible = true,
                                    text = stringResource(R.string.tooltip_drag_handle),
                                    onDismiss = { viewModel.markTooltipSeen(viewModel.tooltipKeyDrag) },
                                    isDark = isDarkTheme
                                )
                            }
                        }
                    }

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
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else if (isDarkTheme) 0.dp else 2.dp),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.AccessAlarm,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = contentColor.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${sched.wakeUpTime.toJavaLocalTime().format(timeFormatter)} - ${sched.member.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator, // Modernes Grip-Icon
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp).alpha(if (isDragging) 1.0f else 0.6f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bathtub,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = contentColor.copy(alpha = if (isDragging) 0.8f else 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.main_schedule_bathroom, sched.bathroomStartTime.toJavaLocalTime().format(timeFormatter), sched.bathroomEndTime.toJavaLocalTime().format(timeFormatter)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor.copy(alpha = if (isDragging) 0.9f else 0.7f)
                                    )
                                }
                                if (sched.member.leaveHomeTime != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = contentColor.copy(alpha = if (isDragging) 0.8f else 0.5f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.main_schedule_leave, sched.member.leaveHomeTime?.toJavaLocalTime()?.format(timeFormatter) ?: ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor.copy(alpha = if (isDragging) 0.9f else 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // Puffer-Anzeige zwischen Slots (nicht nach dem letzten)
                        if (index < totalItems - 1 && sched.bufferAfter > 0 && !isDragging) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp).padding(horizontal = 2.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = stringResource(R.string.buffer_between_display, sched.bufferAfter),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }


                // Liste der Familienmitglieder
                item {
                    Text(
                        text = stringResource(R.string.main_family_members),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }

                // Puffer-Regler (nur bei 2+ Mitgliedern sichtbar)
                if (members.size > 1) {
                    item(key = "buffer_stepper") {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.buffer_after_bath),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.setGlobalBufferMinutes((globalBufferMinutes - 5).coerceAtLeast(0)) },
                                        enabled = globalBufferMinutes > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "$globalBufferMinutes min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.setGlobalBufferMinutes((globalBufferMinutes + 5).coerceAtMost(15)) },
                                        enabled = globalBufferMinutes < 15,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
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
                            description = stringResource(R.string.empty_members_description)
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
                        isAlarmEnabled = isAlarmEnabled,
                        isPauseLoading = pendingPauseIds.contains(member.id)
                    )
                }

                // Platzhalter am Ende, damit der FAB die letzte Karte nicht verdeckt
                item {
                    Spacer(modifier = Modifier.height(88.dp))
                }
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
                            text = stringResource(R.string.main_snooze_active, snoozeTime.toLocalTime().format(timeFormatter)),
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
private fun UnclaimedWarningBanner(memberName: String, isDarkTheme: Boolean) {
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
