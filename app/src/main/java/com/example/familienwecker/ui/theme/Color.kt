package com.example.familienwecker.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Brand Colors
// Primärfarbe : Deep Night Blue   – Ruhe & Schlaf
// Akzentfarbe : Sunrise Orange    – Energie & Weckruf
// Sekundärfarbe: Soft Mint        – Bestätigung & Harmonie
// ─────────────────────────────────────────────────────────────────────────────

// Deep Night Blue Palette
val NightBlue900    = Color(0xFF1A2532) // tiefster Hintergrund (Dark)
val NightBlue800    = Color(0xFF2C3E50) // Primärfarbe (Brand)
val NightBlue700    = Color(0xFF3D5166) // etwas heller
val NightBlue600    = Color(0xFF4E657C) // Container/Surface
val NightBlue200    = Color(0xFFB0C4D8) // onPrimary Container Text (Light) / Dark Primary
val NightBlue100    = Color(0xFFDCE9F3) // sehr hell – Card-Hintergründe Light-Mode

// Sunrise Orange Palette  (Akzent / Tertiary)
val SunriseOrange   = Color(0xFFFF8C42) // Hauptakzent
val SunriseDark     = Color(0xFFCC6E28) // dunklere Variante für Light-Mode
val SunriseLight    = Color(0xFFFFD0A8) // Container (Light)
val SunrisePale     = Color(0xFF3D2007) // onTertiary Container Text

// Soft Mint Palette (Secondary)
val SoftMint        = Color(0xFFA2D5AB) // Hauptfarbe
val MintDark        = Color(0xFF3A7A44) // etwas satter/dunkler
val MintContainer   = Color(0xFFBFE8C5) // satteres Mint statt blass
val MintDeep        = Color(0xFF0B2E12) // onSecondary Container Text

// Neutral / Surface
val SurfaceLight    = Color(0xFFF5F7FA) // heller Hintergrund
val SurfaceDark     = Color(0xFF1A2532) // dunkler Hintergrund
val OnSurfaceLight  = Color(0xFF1A2532) // Text auf heller Fläche
val OnSurfaceDark   = Color(0xFFE8EDF2) // Text auf dunkler Fläche

// Neutrale Card-Hintergründe – abgeleitet aus Night Blue, kein M3-Lila
val CardSurfaceLight  = Color(0xFFE8EFF5) // sehr helles Blau-Grau (Light-Mode Cards)
val CardSurfaceDark   = Color(0xFF243244) // etwas heller als Hintergrund (Dark-Mode Cards)

// Error – Container bewusst neutralgrau statt rosa/lachs
val ErrorRed           = Color(0xFFBA1A1A)
val ErrorRedDark       = Color(0xFFFFB4AB)
val ErrorContainer     = Color(0xFFEDE5DF) // warmgrau statt Lachs-Rosa
val ErrorContainerDk   = Color(0xFF7A1212)

// ─────────────────────────────────────────────────────────────────────────────
// Light Theme
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_light_primary             = NightBlue800
val md_theme_light_onPrimary           = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer    = NightBlue100    // sehr hell → guter Kontrast für Text
val md_theme_light_onPrimaryContainer  = NightBlue900

val md_theme_light_secondary           = MintDark
val md_theme_light_onSecondary         = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer  = MintContainer
val md_theme_light_onSecondaryContainer= MintDeep

val md_theme_light_tertiary            = SunriseDark
val md_theme_light_onTertiary          = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer   = SunriseLight
val md_theme_light_onTertiaryContainer = SunrisePale

val md_theme_light_error               = ErrorRed
val md_theme_light_errorContainer      = ErrorContainer
val md_theme_light_onError             = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer    = Color(0xFF410002)

val md_theme_light_background          = SurfaceLight
val md_theme_light_onBackground        = OnSurfaceLight
val md_theme_light_surface             = SurfaceLight
val md_theme_light_onSurface           = OnSurfaceLight
val md_theme_light_surfaceVariant      = Color(0xFFDCE6EE)
val md_theme_light_onSurfaceVariant    = NightBlue700
val md_theme_light_outline             = NightBlue600

// ─────────────────────────────────────────────────────────────────────────────
// Dark Theme
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_dark_primary              = Color(0xFF90CAF9) // Helleres, klares Blau für Icons/Buttons
val md_theme_dark_onPrimary            = Color(0xFF003354)
val md_theme_dark_primaryContainer     = Color(0xFF2C3E50) // NightBlue800 als Container – satterer Kontrast
val md_theme_dark_onPrimaryContainer   = Color(0xFFDCE9F3) // NightBlue100 als Text auf dunkler Karte

val md_theme_dark_secondary            = SoftMint
val md_theme_dark_onSecondary          = MintDeep
val md_theme_dark_secondaryContainer   = Color(0xFF1E4D2B) // Dunkles Waldgrün für Kontrast
val md_theme_dark_onSecondaryContainer = SoftMint

val md_theme_dark_tertiary             = SunriseOrange
val md_theme_dark_onTertiary           = SunrisePale
val md_theme_dark_tertiaryContainer    = Color(0xFF4A2A0F) // Dunkles Orange/Braun
val md_theme_dark_onTertiaryContainer  = SunriseLight

val md_theme_dark_error                = ErrorRedDark
val md_theme_dark_errorContainer       = ErrorContainerDk
val md_theme_dark_onError              = Color(0xFF690005)
val md_theme_dark_onErrorContainer     = ErrorContainer

val md_theme_dark_background           = Color(0xFF101820) // Noch tieferes Blau-Schwarz
val md_theme_dark_onBackground         = Color(0xFFE8EDF2)
val md_theme_dark_surface              = Color(0xFF101820)
val md_theme_dark_onSurface            = Color(0xFFE8EDF2)
val md_theme_dark_surfaceVariant       = Color(0xFF2C3E50)
val md_theme_dark_onSurfaceVariant     = Color(0xFFB0C4D8)
val md_theme_dark_outline              = Color(0xFF4E657C)