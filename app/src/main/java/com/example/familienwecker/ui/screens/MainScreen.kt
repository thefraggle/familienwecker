package com.example.familienwecker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.familienwecker.model.FamilyMember
import com.example.familienwecker.ui.viewmodel.FamilyViewModel
import androidx.compose.ui.res.stringResource
import com.example.familienwecker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FamilyViewModel,
    onNavigateToAddMember: () -> Unit,
    onNavigateToEditMember: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
    val myMemberId by viewModel.myMemberId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_title)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.main_settings_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddMember) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.main_add_member_desc))
            }
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
            
            // 0. Wecker Ein/Aus Schalter
            val toggleCardColor by animateColorAsState(
                targetValue = if (isAlarmEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(durationMillis = 300),
                label = "toggleCardColor"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = toggleCardColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isAlarmEnabled) stringResource(R.string.main_alarm_enabled) else stringResource(R.string.main_alarm_disabled),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAlarmEnabled) stringResource(R.string.main_alarm_enabled_desc) else stringResource(R.string.main_alarm_disabled_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { viewModel.setAlarmEnabled(it) }
                    )
                }
            }

            // Fallback: Warnung wenn kein Profil ausgewählt ist (nur wenn Mitglieder vorhanden sind)
            if (myMemberId == null && members.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSettings() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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

            // 1. Liste der Familienmitglieder
            Text(stringResource(R.string.main_family_members), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            AnimatedVisibility(visible = members.isEmpty()) {
                Text(stringResource(R.string.main_no_members))
            }
            members.forEach { member ->
                MemberCard(
                    member = member, 
                    onEdit = { onNavigateToEditMember(member.id) },
                    onDelete = { viewModel.removeMember(member.id) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 2. Errechneter Wecker-Plan
            Text(stringResource(R.string.main_current_schedule), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val currentSchedule = schedule
            
            val planCardColor by animateColorAsState(
                targetValue = if (isAlarmEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(durationMillis = 300),
                label = "planCardColor"
            )

            if (currentSchedule == null) {
                Text(stringResource(R.string.main_add_members_prompt))
            } else if (!currentSchedule.isValid) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "❌ " + stringResource(R.string.main_error, currentSchedule.message), 
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = planCardColor)
                ) {
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
                    Card(modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}

@Composable
fun MemberCard(member: FamilyMember, onEdit: () -> Unit, onDelete: () -> Unit) {
    val backgroundColor = if (member.isPaused) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    val textColor = if (member.isPaused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                    if (member.isPaused) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.main_member_paused), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                Text(stringResource(R.string.main_wake_time, member.earliestWakeUp.toString(), member.latestWakeUp.toString()), color = textColor)
                Text(stringResource(R.string.main_bathroom_info, member.bathroomDurationMinutes.toString(), if(member.wantsBreakfast) stringResource(R.string.yes) else stringResource(R.string.no)), color = textColor)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_desc), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
