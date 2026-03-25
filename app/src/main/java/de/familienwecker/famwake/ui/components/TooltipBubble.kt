package de.familienwecker.famwake.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import de.familienwecker.famwake.R

// Indigo-Lila – passend zu den Onboarding-Farben
private val TooltipBackground = Color(0xFF4A148C)
private val TooltipBackgroundDark = Color(0xFF6A1FB0)
private val TooltipText = Color(0xFFF3E5F5)

/**
 * Dezente Sprechblase für Erstnutzer-Hinweise.
 *
 * @param visible  Steuert AnimatedVisibility
 * @param text     Inhalt des Tooltips
 * @param onDismiss Callback beim Schließen (✕-Button)
 * @param isDark   Für leicht helleres Lila im Dark-Mode
 */
@Composable
fun TooltipBubble(
    visible: Boolean,
    text: String,
    onDismiss: () -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) TooltipBackgroundDark else TooltipBackground,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 $text",
                    color = TooltipText,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_desc),
                        tint = TooltipText.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
