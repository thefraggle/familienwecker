import SwiftUI

struct HelpFeedbackSettingsCard: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showTourSheet: Bool
    @Binding var showFeedbackSheet: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        SettingsCardContainer {
            SettingsSectionHeader(icon: "questionmark.circle.fill", title: L.settingsHelpTitle)

            // Tour Button
            Button(action: { showTourSheet = true }) {
                HStack(spacing: 8) {
                    Image(systemName: "map.fill")
                    Text(L.settingsTourButton)
                    Spacer()
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(theme.outline)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .padding(.horizontal, 16)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outline.opacity(0.4), lineWidth: 1))
            .accessibilityLabel(L.s("accessibility_tour_button"))

            // Feedback Button
            Button(action: { showFeedbackSheet = true }) {
                HStack(spacing: 8) {
                    Image(systemName: "envelope.fill")
                    Text(L.settingsFeedbackButton)
                    Spacer()
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(theme.outline)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .padding(.horizontal, 16)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outline.opacity(0.4), lineWidth: 1))
            .accessibilityLabel(L.s("accessibility_feedback_button"))
        }
    }
}
