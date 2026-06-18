package de.familienwecker.famwake.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Initialen-Kreis – wiederverwendbar für Member-Listen.
 * Farbe wird deterministisch aus dem Namens-Hash berechnet (wie iOS).
 */
@Composable
fun InitialsAvatar(
    name: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(name) {
        val words = name.trim().split("\\s+".toRegex())
        when {
            words.isEmpty() || words[0].isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words[0].first()}${words[1].first()}".uppercase()
        }
    }

    // Palette harmonischer Pastellfarben (synchron mit iOS InitialsAvatar.swift)
    val palette = listOf(
        Color(0xFFD9616B), // Rosé
        Color(0xFFE68A5C), // Pfirsich
        Color(0xFFD9B34A), // Gold
        Color(0xFF70A870), // Salbei
        Color(0xFF6BA8A8), // Teal
        Color(0xFF6B9FBF), // Sky
        Color(0xFF6B82BF), // Blau
        Color(0xFF8F6BBF), // Lavendel
        Color(0xFFB06BAE), // Mauve
        Color(0xFFD96B8F), // Pink
    )

    val avatarColor = remember(name) {
        palette[abs(name.hashCode()) % palette.size]
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor)
            .clearAndSetSemantics { } // Dekorativ – Name steht daneben
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
