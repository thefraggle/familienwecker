import SwiftUI

struct AccountActionsSettingsCard: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showLogoutAlert: Bool
    @Binding var showDeleteAccountAlert: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        SettingsCardContainer {
            SettingsSectionHeader(icon: "person.crop.circle.badge.xmark", title: L.settingsAccountActionsTitle)

            // Logout Button
            Button(action: { showLogoutAlert = true }) {
                HStack(spacing: 8) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                    Text(L.settingsLogout)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outline.opacity(0.4), lineWidth: 1))
            .accessibilityLabel(L.s("accessibility_logout"))

            // Delete Account Button
            Button(action: { showDeleteAccountAlert = true }) {
                HStack(spacing: 8) {
                    Image(systemName: "person.crop.circle.badge.minus")
                    Text(L.settingsDeleteAccount)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
            }
            .foregroundStyle(theme.error)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.error.opacity(0.6), lineWidth: 1))
            .accessibilityLabel(L.s("accessibility_delete_account"))
        }
    }
}
