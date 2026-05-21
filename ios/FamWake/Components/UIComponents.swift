import SwiftUI
import Lottie

// MARK: - FamWake Title Helper
/// Renders "FamWake" bold + rest of the localized app name regular.
/// Input: "FamWake Familienwecker" → **FamWake** Familienwecker
func famWakeTitle(_ fullName: String) -> Text {
    if fullName.hasPrefix("FamWake") {
        let suffix = String(fullName.dropFirst("FamWake".count))
        return Text("FamWake").font(.headline).bold() +
               Text(suffix).font(.headline).fontWeight(.regular)
    }
    return Text(fullName).font(.headline)
}

// MARK: - BounceButton Modifier (analog bounceClick)
struct BounceButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.96 : 1.0)
            .animation(.spring(response: 0.2, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

extension Button {
    func bounceStyle() -> some View {
        self.buttonStyle(BounceButtonStyle())
    }
}

// MARK: - FamWake Card Modifier
struct FamWakeCard: ViewModifier {
    var cornerRadius: CGFloat = 24
    var isDark: Bool = false

    func body(content: Content) -> some View {
        let theme = isDark ? FamWakeTheme.dark : FamWakeTheme.light
        content
            .background(
                .regularMaterial,
                in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            )
            .shadow(color: .black.opacity(isDark ? 0.2 : 0.06), radius: 12, x: 0, y: 4)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(theme.outline.opacity(0.15), lineWidth: 0.5)
            )
    }
}

extension View {
    func famWakeCard(cornerRadius: CGFloat = 24, isDark: Bool = false) -> some View {
        self.modifier(FamWakeCard(cornerRadius: cornerRadius, isDark: isDark))
    }
}

// MARK: - Background Gradient (analog Android-Gradient)
struct FamWakeBackground: ViewModifier {
    @Environment(\.colorScheme) var colorScheme

    func body(content: Content) -> some View {
        let theme = FamWakeTheme.current(for: colorScheme)
        ZStack {
            LinearGradient(
                colors: colorScheme == .dark
                    ? [theme.surface, theme.background]
                    : [theme.primaryContainer.opacity(0.5), theme.background],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            content
        }
    }
}

extension View {
    func famWakeBackground() -> some View {
        self.modifier(FamWakeBackground())
    }
}

// MARK: - Tooltip Bubble (analog TooltipBubble.kt)
struct TooltipBubble: View {
    var text: String
    var onDismiss: () -> Void
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "lightbulb.fill")
                .foregroundColor(Color.tooltipYellow)
                .font(.caption)
                .padding(.top, 2)
            Text(text)
                .font(.caption).italic()
                .foregroundStyle(Color.nightBlue150)
                .lineSpacing(3)
            Spacer()
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .foregroundStyle(Color.nightBlue150.opacity(0.8))
                    .font(.caption)
                    .padding(.top, 2)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(colorScheme == .dark ? Color.nightBlue800 : Color.nightBlue700)
                .shadow(color: .black.opacity(0.15), radius: 6, x: 0, y: 3)
        )
    }
}

// MARK: - Sync Rotation Animation
struct RotatingIcon: View {
    let systemName: String
    var color: Color = .accentColor

    @State private var rotation: Double = 0

    var body: some View {
        Image(systemName: systemName)
            .foregroundStyle(color)
            .rotationEffect(.degrees(rotation))
            .onAppear {
                withAnimation(.linear(duration: 1).repeatForever(autoreverses: false)) {
                    rotation = 360
                }
            }
    }
}

// MARK: - Empty State (Lottie-Style SwiftUI Animation)
struct EmptyStateView: View {
    var title: String
    var description: String
    var lottieName: String

    var body: some View {
        VStack(spacing: 20) {
            Text(title)
                .font(.title3).fontWeight(.bold)
                .multilineTextAlignment(.center)
                .padding(.top, 4)

            Text(description)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)

            LottieView(animation: .named(lottieName))
                .playing(loopMode: .loop)
                .animationSpeed(0.7)
                .frame(width: 240, height: 240)
        }
        .padding(32)
    }
}

