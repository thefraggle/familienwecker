package de.familienwecker.famwake.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.ui.components.EmptyState
import de.familienwecker.famwake.ui.components.TooltipBubble
import de.familienwecker.famwake.ui.components.bounceClick
import de.familienwecker.famwake.ui.theme.SnoozeAmberDark
import de.familienwecker.famwake.ui.theme.SnoozeAmberLight
import de.familienwecker.famwake.ui.theme.SnoozeTextDark
import de.familienwecker.famwake.ui.theme.SnoozeTextLight
import de.familienwecker.famwake.ui.viewmodel.FamilyViewModel
import de.familienwecker.famwake.ui.viewmodel.scheduleMessageToUiText
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

fun LazyListScope.scheduleSection(
    schedule: FamilySchedule?,
    viewModel: FamilyViewModel,
    context: Context,
    isDarkTheme: Boolean,
    isAlarmEnabled: Boolean,
    myMemberId: String?,
    tooltipsEnabled: Boolean,
    tooltipDragSeen: Boolean,
    selectedDayOfWeek: Int?,
    draggedItemId: String?,
    setDraggedItemId: (String?) -> Unit,
    draggingOffset: Float,
    setDraggingOffset: (Float) -> Unit,
    setPendingReorder: (Pair<Int, Int>?) -> Unit,
    itemHeightPx: Float,
    timeFormatter: DateTimeFormatter
) {
    item {
        Text(stringResource(R.string.main_current_schedule), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }

    item {
        val appLocale = context.resources.configuration.locales[0]
        val daysOfWeek = remember(appLocale) {
            (1..7).map { dow ->
                java.time.DayOfWeek.of(dow)
                    .getDisplayName(java.time.format.TextStyle.SHORT, appLocale)
                    .replaceFirstChar { it.uppercase() }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEachIndexed { index, dayName ->
                val dayValue = index + 1
                val isSelected = selectedDayOfWeek == dayValue
                
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    label = "chipBg_$dayValue"
                )
                
                val chipContentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "chipContentColor_$dayValue"
                )
                
                val interactionSource = remember { MutableInteractionSource() }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .aspectRatio(1f)
                        .background(chipBg, shape = androidx.compose.foundation.shape.CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .bounceClick(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (isSelected) {
                                viewModel.selectDayOfWeek(null)
                            } else {
                                viewModel.selectDayOfWeek(dayValue)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName.take(2),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = chipContentColor
                    )
                }
            }
        }
    }

    item {
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

        if (schedule == null || schedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.NoActiveSchedule) {
            EmptyState(
                lottieRes = R.raw.mond,
                title = stringResource(R.string.empty_schedule_title),
                description = stringResource(R.string.empty_schedule_description)
            )
        } else if (!schedule.isValid) {
            val cardColor = if (isDarkTheme) SnoozeAmberDark else SnoozeAmberLight
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
                            text = viewModel.scheduleMessageToUiText(schedule.scheduleMessage).asString(),
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    
                    val descKey = when (schedule.scheduleMessage) {
                        is de.familienwecker.famwake.model.ScheduleMessage.MemberConflict -> R.string.schedule_message_member_conflict_desc
                        else -> R.string.schedule_message_no_valid_desc
                    }
                    Text(
                        text = stringResource(descKey),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp, start = 28.dp)
                    )

                    val myMember = schedule.memberSchedules.find { it.member.id == myMemberId }
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
                    val scheduleTargetDate = schedule.targetDate
                    val todayJava = java.time.LocalDate.now()
                    val targetJava = scheduleTargetDate?.let { java.time.LocalDate.of(it.year, it.monthNumber, it.dayOfMonth) }
                    if (targetJava != null && targetJava != todayJava) {
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
                    val msgText = viewModel.scheduleMessageToUiText(schedule.scheduleMessage).asString()
                    val isFlexibleAdjustment = schedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.TimeAdjusted ||
                        schedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BreakfastReduced ||
                        schedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BreakfastAndTimeAdjusted ||
                        schedule.scheduleMessage is de.familienwecker.famwake.model.ScheduleMessage.BufferReduced
                    if (isFlexibleAdjustment) {
                        Text(
                            text = "⚠️ $msgText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    schedule.breakfastTime?.let {
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

    if (schedule != null && schedule.isValid && schedule.memberSchedules.isNotEmpty()) {
        val totalItems = schedule.memberSchedules.size

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
            items = schedule.memberSchedules,
            key = { _, s -> "sched_${s.member.id}" },
            contentType = { _, _ -> "schedule_item" }
        ) { index, sched ->
            val isDragging = draggedItemId == sched.member.id
            
            val otherItemTranslationY by animateFloatAsState(
                targetValue = if (draggedItemId != null && !isDragging) {
                    val draggedIdx = schedule.memberSchedules.indexOfFirst { it.member.id == draggedItemId }
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
                    .pointerInput(schedule.memberSchedules) {
                        // Lokaler Akkumulator – vermeidet Stale-Closure-Bug,
                        // da draggingOffset als Float-Parameter im pointerInput eingefroren wäre.
                        var accumulatedOffset = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                accumulatedOffset = 0f
                                setDraggedItemId(sched.member.id)
                            },
                            onDragEnd = {
                                val offsetItems = (accumulatedOffset / itemHeightPx).roundToInt()
                                val targetIdx = (index + offsetItems).coerceIn(0, totalItems - 1)
                                if (targetIdx != index) {
                                    setPendingReorder(Pair(index, targetIdx))
                                }
                                setDraggedItemId(null)
                                setDraggingOffset(0f)
                            },
                            onDragCancel = {
                                setDraggedItemId(null)
                                setDraggingOffset(0f)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedOffset += dragAmount.y
                                setDraggingOffset(accumulatedOffset)
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

                            // Snooze-Indikator: zeige 💤 wenn Member aktiv snoozed
                            val isSnoozed = sched.member.snoozeUntil?.let { snoozeUntil ->
                                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                snoozeUntil > now
                            } ?: false

                            Text(
                                text = "${sched.wakeUpTime.toJavaLocalTime().format(timeFormatter)} - ${sched.member.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isSnoozed) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = "Snooze",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isDarkTheme) SnoozeAmberLight else SnoozeAmberDark
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
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

            if (index < totalItems - 1 && sched.bufferAfter > 0 && !isDragging) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 0.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.buffer_between_display, sched.bufferAfter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}
