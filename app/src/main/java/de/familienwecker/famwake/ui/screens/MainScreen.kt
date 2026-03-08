package de.familienwecker.famwake.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
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
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsStateWithLifecycle()
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showDeleteMemberDialog by remember { mutableStateOf<FamilyMember?>(null) }
    val whatsNewContent by viewModel.whatsNewContent.collectAsStateWithLifecycle()

    if (whatsNewContent != null) {
        val content = whatsNewContent!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissWhatsNew() },
            title = { Text(content.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text(content.text, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissWhatsNew() },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    val isBatteryOptimized = remember { mutableStateOf<Boolean>(!BatteryUtils.isBatteryOptimizationIgnored(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized.value = !BatteryUtils.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isSyncing, familyId) {
        if (familyId == null) {
            onLogout()
        }
    }

    val themePreferenceVal by viewModel.themePreference.collectAsStateWithLifecycle()
    val isDarkTheme = when (themePreferenceVal) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // 0. Fehlermeldung (falls vorhanden)
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
                
                // 0. Akku-Optimierung Warnung
                item {
                    if (isBatteryOptimized.value && isAlarmEnabled) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                BatteryUtils.requestIgnoreBatteryOptimizations(context)
                            },
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
                                    text = "🔋 " + stringResource(R.string.main_battery_warning),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.main_battery_warning_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
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

                // 1. Errechneter Wecker-Plan
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

                    if (currentSchedule == null || currentSchedule.message == "no_active_schedule") {
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
                                text = "❌ " + stringResource(R.string.main_error, currentSchedule.message), 
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
                                // If there is a flexible adjustment message, show it explicitly
                                if (currentSchedule.message.contains("flexibel") || currentSchedule.message.contains("angepasst")) {
                                    Text(
                                        text = "⚠️ " + currentSchedule.message,
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
                    itemsIndexed(
                        items = currentSched.memberSchedules,
                        key = { _, s -> "sched_${s.member.id}" }
                    ) { index, sched ->
                        val isDragging = draggedItemId == sched.member.id
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) draggingOffset else 0f
                                    scaleX = if (isDragging) 1.08f else 1f
                                    scaleY = if (isDragging) 1.08f else 1f
                                    alpha = if (isDragging) 1.0f else 1f
                                }
                                .pointerInput(currentSched.memberSchedules) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedItemId = sched.member.id },
                                        onDragEnd = {
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
                                            
                                            // Swap logic based on item height (approx 100dp)
                                            val heightPx = 110.dp.toPx()
                                            if (draggingOffset > heightPx / 2 && index < currentSched.memberSchedules.size - 1) {
                                                viewModel.updateMemberOrder(index, index + 1)
                                                draggingOffset -= heightPx
                                            } else if (draggingOffset < -heightPx / 2 && index > 0) {
                                                viewModel.updateMemberOrder(index, index - 1)
                                                draggingOffset += heightPx
                                            }
                                        }
                                    )
                                },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 32.dp else 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                                 else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "⏰ ${sched.wakeUpTime} - ${sched.member.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp).alpha(0.6f)
                                    )
                                }
                                Text(text = stringResource(R.string.main_schedule_bathroom, sched.bathroomStartTime.toString(), sched.bathroomEndTime.toString()))
                                if (sched.member.leaveHomeTime != null) {
                                    Text(text = stringResource(R.string.main_schedule_leave, sched.member.leaveHomeTime.toString()))
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // 2. Liste der Familienmitglieder
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
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
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
                            !isAlarmEnabled -> stringResource(R.string.main_member_alarm_off)
                            member.isPaused -> stringResource(R.string.main_member_alarm_off)
                            else -> stringResource(R.string.main_member_alarm_on)
                        }
                        
                        val statusColor = if (!isAlarmEnabled || member.isPaused) 
                            MaterialTheme.colorScheme.error 
                        else 
                            textColor.copy(alpha = 0.7f)
                        
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
