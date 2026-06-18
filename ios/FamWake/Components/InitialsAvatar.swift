import SwiftUI

/// Initialen-Kreis – wiederverwendbar für Member-Listen & Schedule-Cards.
/// Farbe wird deterministisch aus dem Namens-Hash berechnet (wie Android).
struct InitialsAvatar: View {
    let name: String
    let size: CGFloat

    /// Palette harmonischer Pastellfarben (Android MaterialYou-inspired)
    private static let palette: [Color] = [
        Color(hue: 0.00, saturation: 0.50, brightness: 0.85), // Rosé
        Color(hue: 0.08, saturation: 0.55, brightness: 0.90), // Pfirsich
        Color(hue: 0.15, saturation: 0.50, brightness: 0.85), // Gold
        Color(hue: 0.30, saturation: 0.45, brightness: 0.80), // Salbei
        Color(hue: 0.45, saturation: 0.40, brightness: 0.80), // Teal
        Color(hue: 0.55, saturation: 0.45, brightness: 0.82), // Sky
        Color(hue: 0.65, saturation: 0.45, brightness: 0.82), // Blau
        Color(hue: 0.75, saturation: 0.40, brightness: 0.82), // Lavendel
        Color(hue: 0.85, saturation: 0.40, brightness: 0.82), // Mauve
        Color(hue: 0.95, saturation: 0.45, brightness: 0.85), // Pink
    ]

    /// Erste(n) Buchstaben der ersten zwei Wörter – z.B. "Max Müller" → "MM"
    private var initials: String {
        let words = name
            .trimmingCharacters(in: .whitespaces)
            .split(separator: " ")
        switch words.count {
        case 0:
            return "?"
        case 1:
            return String(words[0].prefix(2)).uppercased()
        default:
            let first = words[0].prefix(1)
            let second = words[1].prefix(1)
            return "\(first)\(second)".uppercased()
        }
    }

    /// Deterministische Farbwahl basierend auf dem String-Hash
    private var avatarColor: Color {
        let hash = abs(name.hashValue)
        return Self.palette[hash % Self.palette.count]
    }

    var body: some View {
        Text(initials)
            .font(.system(size: size * 0.38, weight: .bold, design: .rounded))
            .foregroundStyle(.white)
            .frame(minWidth: size, maxWidth: size)
            .frame(minHeight: size, maxHeight: size)
            .background(avatarColor)
            .clipShape(Circle())
            .accessibilityHidden(true)
    }
}
