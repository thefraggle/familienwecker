import SwiftUI

struct SettingsComponents: View {
    var body: some View {
        EmptyView()
    }
}

struct SettingsCardContainer<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    private var isDark: Bool { colorScheme == .dark }
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            content()
        }
        .padding(16)
        .background(
            .regularMaterial,
            in: RoundedRectangle(cornerRadius: 24, style: .continuous)
        )
        .shadow(color: .black.opacity(isDark ? 0.2 : 0.06), radius: 12, x: 0, y: 4)
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(theme.outline.opacity(0.15), lineWidth: 0.5)
        )
    }
}

struct SettingsSectionHeader: View {
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    let icon: String
    let title: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .foregroundStyle(theme.primary)
            Text(title)
                .font(.headline).fontWeight(.bold)
                .foregroundStyle(theme.onSurface)
        }
        .padding(.bottom, 4)
    }
}

struct SettingsSectionLabel: View {
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    let icon: String?
    let text: String

    var body: some View {
        HStack(spacing: 6) {
            if let icon {
                Image(systemName: icon)
                    .font(.caption2).foregroundStyle(theme.primary)
            }
            Text(text)
                .font(.caption).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
        }
    }
}
