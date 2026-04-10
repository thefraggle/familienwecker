package de.familienwecker.famwake.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Zentrale Material-3-Shapes für FamWake.
 * Pill-orientiert (Pixel-Style): extraLarge ist fast kreisförmig.
 * Wird in FamilienweckerTheme als MaterialTheme.shapes eingebunden.
 */
val FamWakeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // Chips, kleine Tags
    small       = RoundedCornerShape(12.dp),  // Kleine Cards, TextField
    medium      = RoundedCornerShape(20.dp),  // Standard-Cards, Dialoge
    large       = RoundedCornerShape(28.dp),  // BottomSheets, große Panels
    extraLarge  = RoundedCornerShape(32.dp),  // Login-Card, Full-Screen-Sheets
)
