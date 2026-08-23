import SwiftUI

struct AdminSettingsCard: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showAdminPanel: Bool
    @Binding var showResetScheduleAlert: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        SettingsCardContainer {
            SettingsSectionHeader(icon: "shield.fill", title: L.settingsAdminTitle)

            // Toggle admin panel
            Button(action: { showAdminPanel.toggle() }) {
                HStack {
                    Text(L.settingsAdminPanel).font(.body)
                    Spacer()
                    Image(systemName: showAdminPanel ? "chevron.up" : "chevron.down").font(.caption)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 48)
            }
            .foregroundStyle(theme.onSurface)

            if showAdminPanel {
                Divider().background(theme.outline.opacity(0.15))

                // Reset Schedule Button
                Button(action: { showResetScheduleAlert = true }) {
                    HStack(spacing: 8) {
                        Image(systemName: "arrow.counterclockwise")
                        Text(L.settingsAdminResetSchedule)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                }
                .foregroundStyle(theme.error)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.error.opacity(0.4), lineWidth: 1))
            }
        }
    }
}
