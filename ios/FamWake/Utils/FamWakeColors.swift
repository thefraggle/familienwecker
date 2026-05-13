import SwiftUI

// MARK: - FamWake Farben (analog Color.kt / Theme.kt)
extension Color {
    // Primary Palette (Night Blue / Purple)
    static let famPrimary = Color("FamPrimary")
    static let famPrimaryDark = Color("FamPrimaryDark")
    static let famSecondary = Color("FamSecondary")

    // Onboarding Gradients
    static let onboardingWarm1 = Color(hex: "#FF8C00")
    static let onboardingWarm2 = Color(hex: "#FFB347")
    static let onboardingBlue1 = Color(hex: "#1A237E")
    static let onboardingBlue2 = Color(hex: "#283593")
    static let onboardingGreen1 = Color(hex: "#1B5E20")
    static let onboardingGreen2 = Color(hex: "#388E3C")
    static let onboardingPurple1 = Color(hex: "#4A148C")
    static let onboardingPurple2 = Color(hex: "#6A1B9A")
    static let onboardingPink1 = Color(hex: "#880E4F")
    static let onboardingPink2 = Color(hex: "#AD1457")

    // Ringing Screen
    static let ringingPurpleDark = Color(hex: "#1A0035")
    static let ringingPurpleMed = Color(hex: "#3D0054")
    static let ringingPeach = Color(hex: "#FF8A65")

    // Status Colors
    static let onlineGreenLight = Color(hex: "#E8F5E9")
    static let onlineGreenDark = Color(hex: "#1B5E20").opacity(0.3)
    static let onlineIconLight = Color(hex: "#2E7D32")
    static let onlineIconDark = Color(hex: "#A5D6A7")
    static let snoozeAmberLight = Color(hex: "#FFF8E1")
    static let snoozeAmberDark = Color(hex: "#F57F17").opacity(0.25)
    static let snoozeTextLight = Color(hex: "#E65100")
    static let snoozeTextDark = Color(hex: "#FFCC02")

    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB,
                  red: Double(r) / 255,
                  green: Double(g) / 255,
                  blue: Double(b) / 255,
                  opacity: Double(a) / 255)
    }
}
