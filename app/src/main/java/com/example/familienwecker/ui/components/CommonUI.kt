package com.example.familienwecker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A modifier that adds a bounce effect (scaling down) when the element is pressed.
 *
 * @param interactionSource The interaction source to monitor for press states.
 */
@Composable
fun Modifier.bounceClick(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "bounceScale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
