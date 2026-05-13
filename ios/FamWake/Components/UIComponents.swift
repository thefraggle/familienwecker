import SwiftUI

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
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(isDark ? Color(.systemGray6).opacity(0.4) : Color(.systemBackground))
                    .shadow(color: .black.opacity(0.08), radius: 8, x: 0, y: 4)
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .stroke(Color(.separator).opacity(0.2), lineWidth: 1)
                    )
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
        ZStack {
            LinearGradient(
                colors: colorScheme == .dark
                    ? [Color(.systemBackground), Color(.secondarySystemBackground)]
                    : [Color.accentColor.opacity(0.08), Color(.systemBackground)],
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

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "lightbulb.fill")
                .foregroundColor(.yellow)
                .font(.caption)
            Text(text)
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer()
            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(.secondary)
                    .font(.caption)
            }
        }
        .padding(10)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.accentColor.opacity(0.1))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.accentColor.opacity(0.2), lineWidth: 1)
                )
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
    var systemImage: String = "moon.stars.fill"

    @State private var pulse = false
    @State private var floatOffset: CGFloat = 0
    @State private var starOpacity: [Double] = [1, 0.5, 0.8]
    @State private var starScale: [CGFloat] = [1, 0.7, 1.1]

    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                // Glowing circle behind icon
                Circle()
                    .fill(Color.accentColor.opacity(0.12))
                    .frame(width: 110, height: 110)
                    .scaleEffect(pulse ? 1.18 : 1.0)
                    .animation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true), value: pulse)

                Circle()
                    .fill(Color.accentColor.opacity(0.06))
                    .frame(width: 140, height: 140)
                    .scaleEffect(pulse ? 1.0 : 1.12)
                    .animation(.easeInOut(duration: 2.8).repeatForever(autoreverses: true), value: pulse)

                // Floating stars
                ForEach(0..<3) { i in
                    Image(systemName: "sparkle")
                        .font(.system(size: [14, 10, 12][i]))
                        .foregroundStyle(Color.accentColor.opacity(starOpacity[i]))
                        .scaleEffect(starScale[i])
                        .offset(
                            x: [CGFloat(-38), CGFloat(42), CGFloat(-22)][i],
                            y: [CGFloat(-30), CGFloat(-18), CGFloat(36)][i] + (i == 1 ? floatOffset * 0.6 : -floatOffset * 0.4)
                        )
                        .animation(.easeInOut(duration: Double([1.8, 2.4, 2.0][i])).repeatForever(autoreverses: true).delay(Double(i) * 0.4), value: floatOffset)
                }

                // Main icon – floating
                Image(systemName: systemImage)
                    .font(.system(size: 60))
                    .foregroundStyle(Color.accentColor.opacity(0.75))
                    .offset(y: floatOffset)
                    .animation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true), value: floatOffset)
                    .shadow(color: Color.accentColor.opacity(0.3), radius: 16, x: 0, y: 8)
            }
            .frame(width: 140, height: 140)
            .onAppear {
                pulse = true
                floatOffset = -10
                starOpacity = [0.3, 1.0, 0.4]
                starScale = [1.3, 0.6, 1.2]
            }

            Text(title)
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding(.top, 4)

            Text(description)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
        }
        .padding(32)
    }
}

