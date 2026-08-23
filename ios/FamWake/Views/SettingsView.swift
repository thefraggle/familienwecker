import SwiftUI
import AVFoundation

struct SettingsView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var donationViewModel: DonationViewModel
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    private var isDark: Bool { colorScheme == .dark }

    @State private var showLeaveFamilyAlert = false
    @State private var showDeleteFamilyAlert = false
    @State private var showDeleteAccountAlert = false
    @State private var showLogoutAlert = false
    @State private var showProfileConfirmAlert = false
    @State private var pendingClaimMemberId: String?
    @State private var showAdminPanel = false
    @State private var showResetScheduleAlert = false
    @State private var showResetTipsAlert = false
    @State private var showTourSheet = false
    @State private var showFeedbackSheet = false
    @State private var showLoginSheet = false
    @State private var showSoundPicker = false
    @State private var showProfilePicker = false
    @State private var showDonationSheet = false
    @State private var previewPlayer: AVAudioPlayer?

    var body: some View {
        ZStack {
            LinearGradient(
                colors: isDark
                    ? [theme.surface, theme.background]
                    : [theme.primaryContainer.opacity(0.5), theme.background],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            ScrollView {
                VStack(spacing: 16) {
                    // Offline banner
                    if familyViewModel.isOffline {
                        HStack(spacing: 8) {
                            Image(systemName: "icloud.slash").foregroundStyle(theme.outline)
                            Text(L.s("offline_banner_desc")).font(.subheadline).foregroundStyle(theme.outline)
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(theme.surfaceVariant.opacity(0.5))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }

                    // Error banner
                    if let err = familyViewModel.errorMessage {
                        Text("⚠️ \(err)")
                            .foregroundStyle(theme.error)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(theme.errorContainer.opacity(0.3))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .onTapGesture { familyViewModel.clearError() }
                    }

                    // Card 1: Profile & Alarm Sound
                    ProfileAlarmSettingsCard(
                        showProfilePicker: $showProfilePicker,
                        showSoundPicker: $showSoundPicker
                    )

                    // Card 2: Family & Account
                    FamilyAccountSettingsCard(
                        showLoginSheet: $showLoginSheet,
                        showLeaveFamilyAlert: $showLeaveFamilyAlert,
                        showDeleteFamilyAlert: $showDeleteFamilyAlert
                    )

                    // Card 3: Display, Theme, Tooltips, Push
                    DisplaySettingsCard(
                        showResetTipsAlert: $showResetTipsAlert
                    )

                    // Card 4: Help & Feedback
                    HelpFeedbackSettingsCard(
                        showTourSheet: $showTourSheet,
                        showFeedbackSheet: $showFeedbackSheet
                    )

                    // Card 5: Account Actions (Logout, Delete Account)
                    AccountActionsSettingsCard(
                        showLogoutAlert: $showLogoutAlert,
                        showDeleteAccountAlert: $showDeleteAccountAlert
                    )

                    // Card 6: Admin Panel (if admin)
                    if familyViewModel.isAdmin {
                        AdminSettingsCard(
                            showAdminPanel: $showAdminPanel,
                            showResetScheduleAlert: $showResetScheduleAlert
                        )
                    }

                    // Footer
                    SettingsFooterSection(
                        showDonationSheet: $showDonationSheet
                    )
                }
                .padding(16)
            }
        }
        .navigationTitle(L.settingsTitle)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.backward")
                        .font(.body.weight(.semibold))
                }
                .buttonStyle(.borderless)
                .foregroundStyle(theme.primary)
                .accessibilityLabel(L.s("accessibility_back_button"))
            }
        }
        .settingsSheets(
            showProfilePicker: $showProfilePicker,
            showSoundPicker: $showSoundPicker,
            showDonationSheet: $showDonationSheet,
            showTourSheet: $showTourSheet,
            showFeedbackSheet: $showFeedbackSheet,
            showProfileConfirmAlert: $showProfileConfirmAlert,
            pendingClaimMemberId: $pendingClaimMemberId,
            previewPlayer: $previewPlayer
        )
        .settingsFamilyAlerts(
            showLeaveFamilyAlert: $showLeaveFamilyAlert,
            showDeleteFamilyAlert: $showDeleteFamilyAlert,
            dismiss: dismiss
        )
        .settingsAccountAlerts(
            showLogoutAlert: $showLogoutAlert,
            showDeleteAccountAlert: $showDeleteAccountAlert,
            dismiss: dismiss
        )
        .settingsOtherAlerts(
            showProfileConfirmAlert: $showProfileConfirmAlert,
            showResetScheduleAlert: $showResetScheduleAlert,
            showResetTipsAlert: $showResetTipsAlert,
            showProfilePicker: $showProfilePicker,
            pendingClaimMemberId: $pendingClaimMemberId
        )
    }
}

private extension View {
    func settingsSheets(
        showProfilePicker: Binding<Bool>,
        showSoundPicker: Binding<Bool>,
        showDonationSheet: Binding<Bool>,
        showTourSheet: Binding<Bool>,
        showFeedbackSheet: Binding<Bool>,
        showProfileConfirmAlert: Binding<Bool>,
        pendingClaimMemberId: Binding<String?>,
        previewPlayer: Binding<AVAudioPlayer?>
    ) -> some View {
        self
            .sheet(isPresented: showProfilePicker) {
                ProfilePickerSheetView(
                    showProfileConfirmAlert: showProfileConfirmAlert,
                    pendingClaimMemberId: pendingClaimMemberId
                )
            }
            .sheet(isPresented: showSoundPicker) {
                SoundPickerSheetView(previewPlayer: previewPlayer)
            }
            .sheet(isPresented: showDonationSheet) {
                DonationSheetView(showDonationSheet: showDonationSheet)
            }
            .sheet(isPresented: showTourSheet) {
                OnboardingView(isTour: true)
            }
            .sheet(isPresented: showFeedbackSheet) {
                FeedbackView()
            }
    }

    func settingsFamilyAlerts(
        showLeaveFamilyAlert: Binding<Bool>,
        showDeleteFamilyAlert: Binding<Bool>,
        dismiss: DismissAction
    ) -> some View {
        self.modifier(SettingsFamilyAlertsModifier(
            showLeaveFamilyAlert: showLeaveFamilyAlert,
            showDeleteFamilyAlert: showDeleteFamilyAlert,
            dismiss: dismiss
        ))
    }

    func settingsAccountAlerts(
        showLogoutAlert: Binding<Bool>,
        showDeleteAccountAlert: Binding<Bool>,
        dismiss: DismissAction
    ) -> some View {
        self.modifier(SettingsAccountAlertsModifier(
            showLogoutAlert: showLogoutAlert,
            showDeleteAccountAlert: showDeleteAccountAlert,
            dismiss: dismiss
        ))
    }

    func settingsOtherAlerts(
        showProfileConfirmAlert: Binding<Bool>,
        showResetScheduleAlert: Binding<Bool>,
        showResetTipsAlert: Binding<Bool>,
        showProfilePicker: Binding<Bool>,
        pendingClaimMemberId: Binding<String?>
    ) -> some View {
        self.modifier(SettingsOtherAlertsModifier(
            showProfileConfirmAlert: showProfileConfirmAlert,
            showResetScheduleAlert: showResetScheduleAlert,
            showResetTipsAlert: showResetTipsAlert,
            showProfilePicker: showProfilePicker,
            pendingClaimMemberId: pendingClaimMemberId
        ))
    }
}

private struct SettingsFamilyAlertsModifier: ViewModifier {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Binding var showLeaveFamilyAlert: Bool
    @Binding var showDeleteFamilyAlert: Bool
    let dismiss: DismissAction

    func body(content: Content) -> some View {
        content
            .alert(L.settingsLeaveFamily, isPresented: $showLeaveFamilyAlert) {
                Button(L.settingsLeaveConfirm, role: .destructive) {
                    familyViewModel.leaveFamily { success in
                        if success {
                            dismiss()
                        }
                    }
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsLeaveFamilyConfirm)
            }
            .alert(L.settingsDeleteFamily, isPresented: $showDeleteFamilyAlert) {
                Button(L.settingsDeleteConfirm, role: .destructive) {
                    familyViewModel.deleteFamily { success in
                        if success {
                            dismiss()
                        }
                    }
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsDeleteFamilyConfirm)
            }
    }
}

private struct SettingsAccountAlertsModifier: ViewModifier {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Binding var showLogoutAlert: Bool
    @Binding var showDeleteAccountAlert: Bool
    let dismiss: DismissAction

    func body(content: Content) -> some View {
        content
            .alert(L.settingsLogout, isPresented: $showLogoutAlert) {
                Button(L.settingsLogoutConfirm, role: .destructive) {
                    authViewModel.logout()
                    familyViewModel.logout()
                    dismiss()
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsLogoutMessage)
            }
            .alert(L.settingsDeleteAccount, isPresented: $showDeleteAccountAlert) {
                Button(L.settingsDeleteAccountConfirm, role: .destructive) {
                    authViewModel.deleteAccount { success, err in
                        if success {
                            familyViewModel.logout()
                            dismiss()
                        } else if let err = err {
                            familyViewModel.errorMessage = err
                        }
                    }
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsDeleteAccountMessage)
            }
    }
}

private struct SettingsOtherAlertsModifier: ViewModifier {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Binding var showProfileConfirmAlert: Bool
    @Binding var showResetScheduleAlert: Bool
    @Binding var showResetTipsAlert: Bool
    @Binding var showProfilePicker: Bool
    @Binding var pendingClaimMemberId: String?

    func body(content: Content) -> some View {
        content
            .alert(L.settingsProfileClaimTitle, isPresented: $showProfileConfirmAlert) {
                Button(L.confirmButton) {
                    if let id = pendingClaimMemberId {
                        familyViewModel.claimProfile(memberId: id)
                        showProfilePicker = false
                    }
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsProfileClaimMessage)
            }
            .alert(L.settingsAdminResetSchedule, isPresented: $showResetScheduleAlert) {
                Button(L.confirmButton, role: .destructive) {
                    familyViewModel.resetSchedule()
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsAdminResetScheduleConfirm)
            }
            .alert(L.settingsResetTips, isPresented: $showResetTipsAlert) {
                Button(L.confirmButton) {
                    familyViewModel.resetAllTooltips()
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsResetTipsConfirm)
            }
    }
}
