package de.familienwecker.famwake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.familienwecker.famwake.R

// ─────────────────────────────────────────────────────────────────────────────
// Hardcoded Dark-Mode-Farben für die Mockups (immer dark, unabhängig vom System-Theme)
// ─────────────────────────────────────────────────────────────────────────────
private val MockBg         = Color(0xFF000000)
private val MockCard       = Color(0xFF161F2A)
private val MockCardAlt    = Color(0xFF1D2938)
private val MockText       = Color(0xFFE8EDF2)
private val MockSubText    = Color(0xFF8DAFC8)
private val MockOutline    = Color(0xFF4E657C)
private val MockPrimary    = Color(0xFFE3EDF7)
private val MockAccent     = Color(0xFFFFB37A)  // SunriseOrange300
private val MockGreen      = Color(0xFF52B788)  // Mint400
private val MockSelected   = Color(0xFF8DAFC8)  // für aktiven Day-Chip

/**
 * Wrapper der den Mockup-Inhalt wie einen geclippten Phone-Screenshot aussehen lässt.
 * [offsetY] verschiebt den Inhalt nach oben um einen bestimmten Ausschnitt zu zeigen.
 */
@Composable
fun OnboardingMockupCard(
    modifier: Modifier = Modifier,
    height: Dp = 275.dp,
    offsetY: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(20.dp))
            .background(MockBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
        // sanfter Fade am unteren Rand für Screenshot-Effekt
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, MockBg.copy(alpha = 0.85f))))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide 1 – "Alle wissen Bescheid / Everyone's in the loop"
// Zeigt: aktueller Weck-Plan mit Familienmitglied-Karten
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Slide1ScheduleMockup() {
    OnboardingMockupCard(offsetY = 0f) {
        // Abschnitt-Header
        Text(
            text = stringResource(R.string.main_current_schedule),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MockText
        )

        // Shared-Breakfast-Info-Karte
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MockCardAlt)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("✅  ${stringResource(R.string.main_optimal_plan)}",
                    color = MockPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(stringResource(R.string.main_shared_breakfast, "07:20"),
                    color = MockSubText, fontSize = 12.sp)
            }
        }

        // Member-Karte 1
        MockMemberScheduleCard(
            emoji = "🔔",
            time = "06:20",
            name = stringResource(R.string.onboarding_mock_name1),
            bathroomTime = stringResource(R.string.main_schedule_bathroom, "06:20", "06:40")
        )

        // Member-Karte 2
        MockMemberScheduleCard(
            emoji = "🔔",
            time = "06:40",
            name = stringResource(R.string.onboarding_mock_name2),
            bathroomTime = stringResource(R.string.main_schedule_bathroom, "06:40", "07:00")
        )
    }
}

@Composable
private fun MockMemberScheduleCard(emoji: String, time: String, name: String, bathroomTime: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MockCard)
            .border(1.dp, MockOutline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("$emoji  $time – $name",
                color = MockText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(bathroomTime, color = MockSubText, fontSize = 12.sp)
        }
        // Drag-Handle Punkte
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(2) {
                        Box(Modifier.size(3.dp).clip(CircleShape).background(MockOutline))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide 2 – "Deine Familie, ein Plan / Your family, one plan"
// Zeigt: Einladungscode-Karte aus den Einstellungen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Slide2InviteMockup() {
    OnboardingMockupCard(offsetY = 0f, height = 190.dp) {
        // Familie & Account Karte
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MockCard)
                .border(1.dp, MockOutline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Group, contentDescription = null,
                        tint = MockPrimary, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.settings_account_title),
                        color = MockText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Einladungscode Label
                Text(stringResource(R.string.settings_join_code, stringResource(R.string.onboarding_mock_family_name)),
                    color = MockSubText, fontSize = 12.sp)

                // Code-Anzeige
                Text(
                    text = "3KY342",
                    color = MockPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Share-Button
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MockSelected.copy(alpha = 0.18f),
                        contentColor = MockPrimary
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_share_code),
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide 3 – "Jeder Tag perfekt geplant / Every day, perfectly planned"
// Zeigt: Wochentag-Chips (lokalisiert!) + Tageseinstellungen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Slide3DaySettingsMockup() {
    // Kurzbezeichnungen aus strings.xml → automatisch in allen 18 Sprachen korrekt
    val dayShortIds = listOf(
        R.string.weekday_short_1, R.string.weekday_short_2, R.string.weekday_short_3,
        R.string.weekday_short_4, R.string.weekday_short_5,
        R.string.weekday_short_6, R.string.weekday_short_7
    )
    val mondayFullName = stringResource(R.string.weekday_1)

    OnboardingMockupCard(offsetY = 0f, height = 260.dp) {
        // Wochentag-Chip-Leiste
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            dayShortIds.forEachIndexed { index, resId ->
                val isSelected = index == 0   // Montag aktiv
                val active     = index < 5    // Mo–Fr haben Alarm, Sa/So ausgegraut
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isSelected -> MockSelected
                                active     -> MockCardAlt
                                else       -> MockCardAlt.copy(alpha = 0.5f)
                            }
                        )
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(resId),
                        color = when {
                            isSelected -> MockBg
                            active     -> MockText
                            else       -> MockSubText.copy(alpha = 0.5f)
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Tages-Einstellungs-Karte
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MockCard)
                .border(1.dp, MockOutline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Wochentag-Name + Alarm-Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mondayFullName, color = MockText,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.add_member_day_active),
                            color = MockSubText, fontSize = 11.sp)
                        // Toggle-Darstellung (immer ON)
                        Box(
                            modifier = Modifier
                                .width(36.dp).height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MockSelected),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 3.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(MockBg)
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MockOutline.copy(alpha = 0.3f))

                // Früheste Weckzeit
                MockSettingsRow(
                    label = stringResource(R.string.add_member_earliest_wake),
                    value = "06:00"
                )
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MockOutline.copy(alpha = 0.15f))

                // Späteste Weckzeit
                MockSettingsRow(
                    label = stringResource(R.string.add_member_latest_wake),
                    value = "07:30"
                )
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MockOutline.copy(alpha = 0.15f))

                // Bad-Dauer mit Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.add_member_bathroom_duration),
                        color = MockText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("−", color = MockPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("20 min", color = MockAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("+", color = MockPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MockSettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MockText, fontSize = 13.sp)
        Text(value, color = MockPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide 4 – "Stressfreie Morgen / Stress-free mornings"
// Zeigt: Weck-Plan der reibungslos läuft (positiver als leere State)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Slide4ReliableMockup() {
    OnboardingMockupCard(offsetY = 0f, height = 295.dp) {
        Text(
            text = stringResource(R.string.main_current_schedule),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MockText
        )

        // Globaler Wecker-Switch – nachgebildet nach MainScreen Card (extraLarge, dunkelblau, Switch rechts)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))  // entspricht extraLarge shape
                .background(MockCard.copy(alpha = 0.7f))
                .border(1.dp, MockOutline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.main_alarm_enabled),
                        color = MockText,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = stringResource(R.string.main_alarm_enabled_desc),
                        color = MockSubText,
                        fontSize = 12.sp
                    )
                }
                // Switch-Darstellung: längliches Pill mit Thumb
                Box(
                    modifier = Modifier
                        .width(52.dp).height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MockSelected),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MockBg)
                    )
                }
            }
        }

        MockMemberScheduleCard(
            emoji = "🔔", time = "06:20",
            name = stringResource(R.string.onboarding_mock_name1),
            bathroomTime = stringResource(R.string.main_schedule_bathroom, "06:20", "06:40")
        )
        MockMemberScheduleCard(
            emoji = "🔔", time = "06:40",
            name = stringResource(R.string.onboarding_mock_name2),
            bathroomTime = stringResource(R.string.main_schedule_bathroom, "06:40", "07:00")
        )
    }
}
