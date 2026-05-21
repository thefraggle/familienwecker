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

import androidx.compose.material.icons.filled.Lightbulb

import androidx.compose.ui.unit.sp

// Brand-Farben (nightBlue700, nightBlue800, nightBlue150)
private val TooltipBackground = Color(0xFF1D2938)
private val TooltipBackgroundDark = Color(0xFF161F2A)
private val TooltipText = Color(0xFFE3EDF7)
private val TooltipIconTint = Color(0xFFFFE082) // Weiches Pastellgelb für die Glühbirne

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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = TooltipIconTint,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = TooltipText,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp).padding(top = 2.dp)
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
