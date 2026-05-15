import SwiftUI

// ─────────────────────────────────────────────────────────────────────────────
// FamWake Brand Palette – 1:1 mirror of Android Color.kt
//
// Primary   : Deep Night Blue  – Trust, Sleep
// Secondary : Soft Mint        – Confirmation, Fresh
// Tertiary  : Sunrise Orange   – Energy, Wake-up accent
// ─────────────────────────────────────────────────────────────────────────────

// MARK: - Night Blue
extension Color {
    static let nightBlue950    = Color(hex: "#080C11")
    static let nightBlue900    = Color(hex: "#0F151C")
    static let nightBlue800    = Color(hex: "#161F2A")
    static let nightBlue700    = Color(hex: "#1D2938")
    static let nightBlue600    = Color(hex: "#4E657C")
    static let nightBlue300    = Color(hex: "#8DAFC8")
    static let nightBlue150    = Color(hex: "#E3EDF7")
    static let nightBlue080    = Color(hex: "#E8F0F8")
    static let nightBlue050    = Color(hex: "#F3F7FB")
}

// MARK: - Sunrise Orange
extension Color {
    static let sunriseOrange600 = Color(hex: "#E07628")
    static let sunriseOrange500 = Color(hex: "#FF8C42")
    static let sunriseOrange300 = Color(hex: "#FFB37A")
    static let sunriseOrange100 = Color(hex: "#FFE8D2")
    static let sunriseOrange900 = Color(hex: "#3D1A00")
}

// MARK: - Soft Mint
extension Color {
    static let mint600          = Color(hex: "#2E7D52")
    static let mint400          = Color(hex: "#52B788")
    static let mint100          = Color(hex: "#CCEEDB")
    static let mint900          = Color(hex: "#0A2E1A")
}

// MARK: - Error
extension Color {
    static let errorRed700      = Color(hex: "#BA1A1A")
    static let errorRed300      = Color(hex: "#FF8A80")
    static let errorRedCont     = Color(hex: "#FFDAD6")
    static let errorRedContDark = Color(hex: "#93000A")
}

// MARK: - Ringing Screen
extension Color {
    static let ringingPurpleDark = Color(hex: "#2D1B69")
    static let ringingPurpleMed  = Color(hex: "#6B3FA0")
    static let ringingPeach      = Color(hex: "#FFB347")
}

// MARK: - Snooze & Status Cards
extension Color {
    static let snoozeAmberDark   = Color(hex: "#332000")
    static let snoozeAmberLight  = Color(hex: "#FFE0B2")
    static let snoozeTextDark    = Color(hex: "#FFCC80")
    static let snoozeTextLight   = Color(hex: "#3E2723")

    static let onlineGreenDark   = Color(hex: "#1B321B")
    static let onlineGreenLight  = Color(hex: "#E8F5E9")
    static let onlineIconDark    = Color(hex: "#81C784")
    static let onlineIconLight   = Color(hex: "#2E7D32")
}

// MARK: - Theme Colors (Light / Dark)
struct FamWakeTheme {
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let secondary: Color
    let onSecondary: Color
    let secondaryContainer: Color
    let onSecondaryContainer: Color
    let tertiary: Color
    let onTertiary: Color
    let tertiaryContainer: Color
    let onTertiaryContainer: Color
    let error: Color
    let errorContainer: Color
    let onError: Color
    let onErrorContainer: Color
    let background: Color
    let onBackground: Color
    let surface: Color
    let onSurface: Color
    let surfaceVariant: Color
    let onSurfaceVariant: Color
    let outline: Color

    // MARK: - Light Theme
    static let light = FamWakeTheme(
        primary:             .nightBlue800,
        onPrimary:           .white,
        primaryContainer:    .white,
        onPrimaryContainer:  .nightBlue950,
        secondary:           .mint600,
        onSecondary:         .white,
        secondaryContainer:  .mint100,
        onSecondaryContainer:.mint900,
        tertiary:            .sunriseOrange600,
        onTertiary:          .white,
        tertiaryContainer:   .sunriseOrange100,
        onTertiaryContainer: .sunriseOrange900,
        error:               .errorRed700,
        errorContainer:      Color(hex: "#FFDAD6"),
        onError:             .white,
        onErrorContainer:    .nightBlue950,
        background:          Color(hex: "#F0F4F8"),
        onBackground:        .nightBlue950,
        surface:             .white,
        onSurface:           .nightBlue950,
        surfaceVariant:      .nightBlue080,
        onSurfaceVariant:    .nightBlue950,
        outline:             .nightBlue600
    )

    // MARK: - Dark Theme
    static let dark = FamWakeTheme(
        primary:             .nightBlue150,
        onPrimary:           .nightBlue950,
        primaryContainer:    .nightBlue800,
        onPrimaryContainer:  .nightBlue150,
        secondary:           .mint400,
        onSecondary:         .mint900,
        secondaryContainer:  Color(hex: "#1B4A30"),
        onSecondaryContainer:.mint400,
        tertiary:            .sunriseOrange300,
        onTertiary:          .sunriseOrange900,
        tertiaryContainer:   Color(hex: "#4A2800"),
        onTertiaryContainer: .sunriseOrange300,
        error:               .errorRed300,
        errorContainer:      .errorRedContDark,
        onError:             Color(hex: "#690005"),
        onErrorContainer:    .errorRedCont,
        background:          .black,
        onBackground:        Color(hex: "#E8EDF2"),
        surface:             .black,
        onSurface:           Color(hex: "#E8EDF2"),
        surfaceVariant:      .nightBlue800,
        onSurfaceVariant:    .nightBlue150,
        outline:             .nightBlue600
    )

    /// Get theme for current color scheme
    static func current(for scheme: ColorScheme?) -> FamWakeTheme {
        switch scheme {
        case .dark: return .dark
        default: return .light
        }
    }
}

// MARK: - Hex Color Initializer
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6: // RGB
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
