package de.familienwecker.famwake.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.ui.components.TooltipBubble
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.ui.viewmodel.setAlarmEnabled
import de.familienwecker.famwake.ui.viewmodel.checkAndShowReview
import de.familienwecker.famwake.ui.viewmodel.toggleAwakeMember
import de.familienwecker.famwake.util.findActivity

@Composable
fun AlarmToggleSection(
    viewModel: FamilyViewModel,
    context: Context,
    isDarkTheme: Boolean,
    isAlarmEnabled: Boolean,
    tooltipsEnabled: Boolean,
    tooltipSwitchSeen: Boolean,
    tooltipAwakeSeen: Boolean,
    myMemberId: String?,
    members: List<FamilyMember>,
    deviceSchedule: FamilySchedule?,
    isAwakeTodayLocal: Boolean
) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                        context.findActivity()?.let { activity ->
                            viewModel.checkAndShowReview(activity)
                        }
                    },
                    enabled = myMemberId != null
                )
            }

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
            val isAwakeButtonVisible = remember(myMember, isAlarmEnabled, deviceSchedule, isAwakeTodayLocal) {
                if (myMember == null || !isAlarmEnabled) return@remember false

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
                    val myScheduledTime = deviceSchedule?.memberSchedules
                        ?.find { it.member.id == myMemberId }
                        ?.wakeUpTime?.toJavaLocalTime()
                    val alarmTime = myScheduledTime ?: profile.earliestWakeUp.toJavaLocalTime()
                    val targetDt = java.time.LocalDateTime.of(targetDate, alarmTime)

                    if (isAwakeTodayLocal) {
                        targetDate == todayDate && nowDt < targetDt
                    } else {
                        val isToday = targetDate == todayDate
                        val windowStart = targetDt.minusHours(4)
                        (isToday || nowDt >= windowStart) && nowDt < targetDt
                    }
                } else {
                    false
                }
            }

            AnimatedVisibility(
                visible = isAwakeButtonVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
