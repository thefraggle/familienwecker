import SwiftUI
import Aptabase

struct FamilyAccountSettingsCard: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showLoginSheet: Bool
    @Binding var showLeaveFamilyAlert: Bool
    @Binding var showDeleteFamilyAlert: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        SettingsCardContainer {
            SettingsSectionHeader(icon: "person.3.fill", title: L.settingsAccountTitle)

            // Anonymous → Link Account
            if authViewModel.isAnonymous {
                Button(action: { showLoginSheet = true }) {
                    HStack {
                        Image(systemName: "person.badge.plus")
                        Text(L.s("settings_anonymous_login_button"))
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(theme.primary)
                    .foregroundStyle(theme.onPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .padding(.bottom, 12)
                .accessibilityLabel(L.s("accessibility_link_account"))
                .accessibilityHint(L.s("accessibility_link_account_hint"))
                .sheet(isPresented: $showLoginSheet) {
                    LoginView()
                }
            }

            // Join Code
            if let code = familyViewModel.joinCode {
                let fName = familyViewModel.familyName ?? ""
                Text(L.settingsJoinCodeName(fName))
                    .font(.subheadline)

                Text(authViewModel.isAnonymous ? "******" : code)
                    .font(.title2).fontWeight(.black)
                    .foregroundStyle(theme.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)

                // Share
                if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipInviteSeen {
                    TooltipBubble(text: L.tooltipInviteCode) {
                        familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyInvite)
                    }
                }

                Button(action: {
                    if authViewModel.isAnonymous {
                        familyViewModel.errorMessage = L.s("settings_share_code_locked")
                    } else {
                        let text = L.settingsShareMessage(fName, code)
                        presentShareSheet(with: text)
                    }
                }) {
                    HStack {
                        Image(systemName: "person.3.fill").font(.caption)
                        Text(L.settingsShareCode)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(theme.primary)
                    .foregroundStyle(theme.onPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .padding(.bottom, 8)
                .accessibilityLabel(L.s("accessibility_share_code"))
            }

            // Leave and Delete Family Buttons
            VStack(spacing: 8) {
                // Leave Family
                Button(action: { showLeaveFamilyAlert = true }) {
                    HStack(spacing: 8) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                        Text(L.settingsLeaveFamily)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                }
                .foregroundStyle(theme.onSurface)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outline.opacity(0.4), lineWidth: 1))
                .accessibilityLabel(L.s("accessibility_leave_family"))

                // Delete Family
                Button(action: {
                    if familyViewModel.isAdmin {
                        showDeleteFamilyAlert = true
                    } else {
                        familyViewModel.errorMessage = L.errorDeleteNotAdmin
                    }
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "trash.fill")
                        Text(L.settingsDeleteFamily)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                }
                .foregroundStyle(familyViewModel.isAdmin ? theme.error : theme.outline)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke((familyViewModel.isAdmin ? theme.error : theme.outline).opacity(0.6), lineWidth: 1))
                .accessibilityLabel(L.s("accessibility_delete_family"))
            }
        }
    }

    private func presentShareSheet(with text: String) {
        Aptabase.shared.trackEvent("family_invite_shared")
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            return
        }
        let activityVC = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let popoverController = activityVC.popoverPresentationController {
            popoverController.sourceView = rootViewController.view
            popoverController.sourceRect = CGRect(x: rootViewController.view.bounds.midX, y: rootViewController.view.bounds.midY, width: 0, height: 0)
            popoverController.permittedArrowDirections = []
        }
        rootViewController.present(activityVC, animated: true, completion: nil)
    }
}
