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
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormatter = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a") }
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
            // Avatar – Initialen-Kreis (wie iOS MemberCardView)
            de.familienwecker.famwake.ui.components.InitialsAvatar(
                name = member.name,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val allDaysInactive = member.dayProfiles?.values?.none { it.isActive } == true

                // Row 1: Name
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Status & Awake Emoji
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (member.claimedByUserId != null) {
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
                            member.id == myMemberId -> if (!isAlarmEnabled || member.isPaused || allDaysInactive) MaterialTheme.colorScheme.error else textColor.copy(alpha = 0.8f)
                            else -> if (member.deviceAlarmEnabled == false || member.isPaused || allDaysInactive) MaterialTheme.colorScheme.error else textColor.copy(alpha = 0.8f)
                        }

                        val cleanStatusText = statusText
                            .replace("(", "")
                            .replace(")", "")
                            .replace("（", "")
                            .replace("）", "")

                        Text(
                            text = cleanStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    } else if (member.isPaused) {
                        Text(
                            text = stringResource(R.string.member_status_paused),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    if (member.isAwakeToday) {
                        if (member.claimedByUserId != null || member.isPaused) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(text = "☀️", style = MaterialTheme.typography.bodyMedium)
                    }
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
