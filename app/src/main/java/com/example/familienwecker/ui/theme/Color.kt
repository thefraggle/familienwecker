package com.example.familienwecker.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// FamWake Brand Palette
//
// Primary   : Deep Night Blue  – Ruhe, Vertrauen, Schlaf
// Secondary : Soft Mint        – Bestätigung, Frische
// Tertiary  : Sunrise Orange   – Energie, Weckruf, Akzent
//
// Kontrast-Ziel: WCAG AA (≥ 4.5:1 Text, ≥ 3:1 große UI-Elemente)
// ─────────────────────────────────────────────────────────────────────────────

// ── Night Blue ───────────────────────────────────────────────────────────────
val NightBlue950    = Color(0xFF0F1923) // tiefster Hintergrund Night
val NightBlue900    = Color(0xFF1A2532) // dunkler Hintergrund
val NightBlue800    = Color(0xFF2C3E50) // Primärfarbe (Brand-Dunkel)
val NightBlue700    = Color(0xFF3D5166) // Surface-Variante Dark
val NightBlue600    = Color(0xFF4E657C) // Outline Dark
val NightBlue300    = Color(0xFF8DAFC8) // Primärfarbe Light im Dark-Mode
val NightBlue150    = Color(0xFFCCDEED) // heller Container-Text im Light-Mode
val NightBlue080    = Color(0xFFE8F0F8) // sehr helle Card in Light-Mode
val NightBlue050    = Color(0xFFF3F7FB) // Page-Background Light

// ── Sunrise Orange ───────────────────────────────────────────────────────────
val SunriseOrange600 = Color(0xFFE07628) // gesättigter für Light-Mode (kontrastreich auf Weiß)
val SunriseOrange500 = Color(0xFFFF8C42) // Hauptakzent (Brand)
val SunriseOrange300 = Color(0xFFFFB37A) // etwas heller für Dark-Mode Akzent
val SunriseOrange100 = Color(0xFFFFE8D2) // Light-Container
val SunriseOrange900 = Color(0xFF3D1A00) // onTertiaryContainer Dark

// ── Soft Mint ────────────────────────────────────────────────────────────────
val Mint600          = Color(0xFF2E7D52) // gesättigter für Light-Mode
val Mint400          = Color(0xFF52B788) // heller für Dark-Mode
val Mint100          = Color(0xFFCCEEDB) // Light-Container
val Mint900          = Color(0xFF0A2E1A) // onSecondaryContainer Dark

// ── Error ────────────────────────────────────────────────────────────────────
val ErrorRed700      = Color(0xFFBA1A1A) // Light error
val ErrorRed300      = Color(0xFFFF8A80) // Dark error – warm, gut sichtbar auf dunkel
val ErrorRedCont     = Color(0xFFFFDAD6) // Light errorContainer – klar lesbar
val ErrorRedContDark = Color(0xFF93000A) // Dark errorContainer

// ─────────────────────────────────────────────────────────────────────────────
// Light Color Scheme
// Hintergrund: Fast-Weiß mit leichtem Blau-Stich (NightBlue050)
// Cards: NightBlue080 – sichtbar abgehoben, edler als reines Weiß
// Primary: NightBlue800 – kräftiger Kontrast auf hellem BG (7:1+)
// Akzent: Sunrise Orange in TopAppBar + interaktiven Elementen
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_light_primary             = NightBlue800        // #2C3E50 – 8.5:1 auf Weiß ✓
val md_theme_light_onPrimary           = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer    = NightBlue080        // #E8F0F8 – helle, klar abgesetzte Cards
val md_theme_light_onPrimaryContainer  = NightBlue900        // #1A2532 – 10:1 auf Container ✓

val md_theme_light_secondary           = Mint600             // #2E7D52 – 4.7:1 auf Weiß ✓
val md_theme_light_onSecondary         = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer  = Mint100             // #CCEEDD
val md_theme_light_onSecondaryContainer= Mint900             // #0A2E1A – hoher Kontrast ✓

val md_theme_light_tertiary            = SunriseOrange600    // #E07628 – 3.6:1 auf Weiß ✓ (groß)
val md_theme_light_onTertiary          = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer   = SunriseOrange100    // #FFE8D2
val md_theme_light_onTertiaryContainer = SunriseOrange900    // #3D1A00 ✓

val md_theme_light_error               = ErrorRed700
val md_theme_light_errorContainer      = ErrorRedCont        // #FFDAD6 – klar als Fehler erkennbar
val md_theme_light_onError             = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer    = Color(0xFF410002)

val md_theme_light_background          = NightBlue050        // #F3F7FB – hochwertiger als reines Weiß
val md_theme_light_onBackground        = NightBlue900
val md_theme_light_surface             = NightBlue050
val md_theme_light_onSurface           = NightBlue900
val md_theme_light_surfaceVariant      = NightBlue150        // #CCDEEC – dezenter Kontrast
val md_theme_light_onSurfaceVariant    = NightBlue700
val md_theme_light_outline             = NightBlue600

// ─────────────────────────────────────────────────────────────────────────────
// Dark Color Scheme
// Hintergrund: NightBlue950 – tiefstes Blau-Schwarz (echter Night-Feel)
// Cards: NightBlue800 – satte Tiefe, klar vom BG abgehoben (~3:1)
// Primary (interaktiv): NightBlue300 – helles Blau, Brand-nah, 5.5:1 ✓
// Akzent: SunriseOrange300 – wärmer, gut auf dunklem Grund (4.5:1+)
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_dark_primary              = NightBlue300        // #8DAFC8 – 5.5:1 auf #0F1923 ✓
val md_theme_dark_onPrimary            = NightBlue950        // sehr dunkel für Button-Text
val md_theme_dark_primaryContainer     = NightBlue800        // #2C3E50 – Card-BG im Dark ✓
val md_theme_dark_onPrimaryContainer   = NightBlue150        // #CCDEEC – 8:1 auf Container ✓

val md_theme_dark_secondary            = Mint400             // #52B788 – 4.6:1 auf #0F1923 ✓
val md_theme_dark_onSecondary          = Mint900
val md_theme_dark_secondaryContainer   = Color(0xFF1B4A30)   // tiefdunkles Grün
val md_theme_dark_onSecondaryContainer = Mint400

val md_theme_dark_tertiary             = SunriseOrange300    // #FFB37A – 5.2:1 auf #0F1923 ✓
val md_theme_dark_onTertiary           = SunriseOrange900
val md_theme_dark_tertiaryContainer    = Color(0xFF4A2800)   // tiefes Braun-Orange
val md_theme_dark_onTertiaryContainer  = SunriseOrange300

val md_theme_dark_error                = ErrorRed300         // #FF8A80 – warm, lesbar auf Dunkel
val md_theme_dark_errorContainer       = ErrorRedContDark    // #93000A
val md_theme_dark_onError              = Color(0xFF690005)
val md_theme_dark_onErrorContainer     = ErrorRedCont        // #FFDAD6 – hoch kontrastreich ✓

val md_theme_dark_background           = NightBlue950        // #0F1923 – tiefes Blau-Schwarz
val md_theme_dark_onBackground         = Color(0xFFE8EDF2)
val md_theme_dark_surface              = NightBlue950
val md_theme_dark_onSurface            = Color(0xFFE8EDF2)
val md_theme_dark_surfaceVariant       = NightBlue800        // #2C3E50 – für Card-ähnliche Elemente
val md_theme_dark_onSurfaceVariant     = NightBlue300        // #8DAFC8 – lesbar auf dunklen Cards
val md_theme_dark_outline              = NightBlue600        // #4E657C