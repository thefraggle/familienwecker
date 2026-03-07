package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.util.BatteryUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.familienwecker.famwake.R
import kotlinx.coroutines.launch
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.components.EmptyState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FamilyViewModel,
    onNavigateToAddMember: () -> Unit,
    onNavigateToEditMember: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLeaveFamily: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val whatsNewContent by viewModel.whatsNewContent.collectAsStateWithLifecycle()
    var showDeleteMemberDialog by remember { mutableStateOf<FamilyMember?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    if (showDeleteMemberDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteMemberDialog = null },
            title = { Text(stringResource(R.string.delete_member_title)) },
            text = { Text(stringResource(R.string.delete_member_text, showDeleteMemberDialog!!.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val memberToDelete = showDeleteMemberDialog!!
                        if (memberToDelete.id == myMemberId) {
                            viewModel.setMyMemberId(null) { }
                        }
                        viewModel.removeMember(memberToDelete.id)
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

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()

    // Deep Link Join Conflict Dialog
    val pendingJoinCode by viewModel.pendingJoinCode.collectAsStateWithLifecycle()
    val currentFamilyName by viewModel.familyName.collectAsStateWithLifecycle()

    if (pendingJoinCode != null && familyId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingJoinCode() },
            title = { Text(stringResource(R.string.join_conflict_title)) },
            text = { Text(stringResource(R.string.join_conflict_text, currentFamilyName ?: "---", pendingJoinCode!!)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveAndJoinPendingCode { success ->
                            if (success) {
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.main_sync_success))
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.join_conflict_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPendingJoinCode() }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
    
    // What's New Popup
    whatsNewContent?.let { content ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissWhatsNew() },
            title = { Text(content.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text(content.text, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissWhatsNew() },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(content.buttonText)
                }
            }
        )
    }
    
    val isBatteryOptimized = remember { mutableStateOf(!BatteryUtils.isBatteryOptimizationIgnored(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.triggerMemberReset()
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isSyncing, familyId) {
        if (familyId == null) {
            onLeaveFamily()
        }
    }

    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val isDarkTheme = when (themePreference) {
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

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            buildAnnotatedString {
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append("FamWake")
                                }
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Normal)) {
                                    append(" " + stringResource(R.string.app_name_short))
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
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.main_settings_desc))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 0. Fehlermeldung (falls vorhanden)
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

            // 0. Akku-Optimierung Warnung
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

            // 0b. Wecker Ein/Aus Schalter
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

            // Fallback: Warnung wenn kein Profil ausgewählt ist (nur wenn Mitglieder vorhanden sind)
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

            // 1. Errechneter Wecker-Plan
            Text(stringResource(R.string.main_current_schedule), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
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
                    @Suppress("DEPRECATION")
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isAlarmEnabled) "✅ " + stringResource(R.string.main_optimal_plan) else "⏸️ " + stringResource(R.string.main_plan_paused), 
                            fontWeight = FontWeight.Bold
                        )
                        // If there is a flexible adjustment message, show it explicitly
                        if (currentSchedule.message.contains("flexibel")) {
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

                currentSchedule.memberSchedules.sortedBy { it.wakeUpTime }.forEach { sched ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                             else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "⏰ ${sched.wakeUpTime} - ${sched.member.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.main_schedule_bathroom, sched.bathroomStartTime.toString(), sched.bathroomEndTime.toString()))
                            if (sched.member.leaveHomeTime != null) {
                                Text(text = stringResource(R.string.main_schedule_leave, sched.member.leaveHomeTime.toString()))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 2. Liste der Familienmitglieder
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
            if (members.size >= 6) {
                Text(
                    text = stringResource(R.string.main_member_limit_reached),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (members.isEmpty()) {
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
            
            // Verwende animateItem für geschmeidige Umsortierungen/Hinzufügen
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                members.forEach { member ->
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
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
    }
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
    // Aktive Karten: primaryContainer (helles Night-Blue-Grau) – brand-konform, kein Grün, kein Lila
    // Pausierte Karten: surfaceVariant mit reduzierter Deckkraft (gedimmt)
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
