package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.ui.theme.LocalDarkTheme
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter

/**
 * Kachel zur Darstellung eines Familienmitglieds in der Mitgliederliste.
 * Zeigt Name, Weckzeitfenster, Badezimmer-Info sowie Aktions-Buttons (Pause, Edit, Delete).
 */
@Composable
fun MemberCard(
    member: FamilyMember,
    myMemberId: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleAwake: () -> Unit,
    isAlarmEnabled: Boolean,
    isPauseLoading: Boolean = false
) {
    val isDarkTheme = LocalDarkTheme.current
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val backgroundColor = if (member.isPaused) {
        if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = if (member.isPaused)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    val isOtherUserClaim = member.claimedByUserId != null && member.id != myMemberId

    Card(
        onClick = { if (!isOtherUserClaim) onEdit() },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        // Tonal statt Shadow-Elevation: Pixel/Material-You-Stil
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp),
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
                val allDaysInactive = member.dayProfiles?.values?.none { it.isActive } == true

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
                                !isAlarmEnabled -> stringResource(R.string.main_member_alarm_off)
                                member.isPaused || allDaysInactive -> stringResource(R.string.main_member_alarm_off)
                                else -> stringResource(R.string.main_member_alarm_on)
                            }
                            else -> when {
                                member.deviceAlarmEnabled == false -> stringResource(R.string.main_member_alarm_off)
                                member.isPaused || allDaysInactive -> stringResource(R.string.main_member_alarm_off)
                                else -> stringResource(R.string.main_member_alarm_on)
                            }
                        }

                        val statusColor = when {
                            member.id == myMemberId -> if (!isAlarmEnabled || member.isPaused || allDaysInactive) MaterialTheme.colorScheme.error else textColor.copy(alpha = 0.7f)
                            else -> if (member.deviceAlarmEnabled == false || member.isPaused || allDaysInactive) MaterialTheme.colorScheme.error else textColor.copy(alpha = 0.7f)
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
                        Text(text = "☀️", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (!allDaysInactive) {
                    val today = java.time.LocalDate.now()
                    val nextActiveDayResult = (0..6).mapNotNull { offset ->
                        val date = today.plusDays(offset.toLong())
                        val dow = date.dayOfWeek.value
                        val profile = member.dayProfiles?.get(dow)
                        if (profile != null && profile.isActive) date to profile else null
                    }.firstOrNull()

                    val showDayLabel = nextActiveDayResult != null && nextActiveDayResult.first != today

                    val displayEarliest = nextActiveDayResult?.second?.earliestWakeUp?.toJavaLocalTime() ?: member.earliestWakeUp.toJavaLocalTime()
                    val displayLatest   = nextActiveDayResult?.second?.latestWakeUp?.toJavaLocalTime()   ?: member.latestWakeUp.toJavaLocalTime()

                    if (showDayLabel) {
                        nextActiveDayResult?.let { (date, _) ->
                            val dayName = date.dayOfWeek
                                .getDisplayName(java.time.format.TextStyle.FULL, context.resources.configuration.locales[0])
                                .replaceFirstChar { it.uppercase() }
                            Text(
                                text = dayName,
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.main_wake_time, displayEarliest.format(timeFormatter), displayLatest.format(timeFormatter)), 
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.main_bathroom_info, member.bathroomDurationMinutes.toString(), if (member.wantsBreakfast) stringResource(R.string.yes) else stringResource(R.string.no)), 
                        color = textColor.copy(alpha = 0.9f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pausieren nur für unclaimed Member die NICHT das eigene Profil sind.
                // Doppelte Absicherung: auch myMemberId ausschließen (Auto-Claim könnte kurz verzögert sein).
                if (member.claimedByUserId == null && member.id != myMemberId) {
                    IconButton(
                        onClick = { if (!isPauseLoading) onTogglePause() },
                        modifier = Modifier.size(32.dp),
                        enabled = !isPauseLoading
                    ) {
                        if (isPauseLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        } else {
                            val icon = if (member.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(R.string.pause_today_desc),
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
