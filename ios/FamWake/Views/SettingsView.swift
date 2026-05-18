import SwiftUI
import UserNotifications

struct SettingsView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var donationViewModel: DonationViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) var scenePhase

    @State private var showFeedback = false
    @State private var showProfilePicker = false
    @State private var showLeaveFamilyAlert = false
    @State private var showDeleteFamilyAlert = false
    @State private var showDeleteFamilyFinalAlert = false
    @State private var showShareSheet = false
    @State private var shareContent = ""
    @State private var showDonationSheet = false
    @State private var memberToSteal: FamilyMember? = nil
    @State private var showStealAlert = false
    @State private var showLoginSheet = false

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    private var isDark: Bool { colorScheme == .dark }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    
                    // Error Card
                    if let error = familyViewModel.errorMessage {
                        HStack(alignment: .top) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundStyle(theme.error)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(error)
                                    .font(.subheadline)
                                    .foregroundStyle(theme.error)
                                
                                if error == L.errorAlarmPermission {
                                    Button(action: {
                                        if let url = URL(string: UIApplication.openSettingsURLString) {
                                            UIApplication.shared.open(url)
                                        }
                                    }) {
                                        HStack(spacing: 4) {
                                            Text(L.settingsTitle)
                                                .font(.caption).fontWeight(.bold)
                                            Image(systemName: "chevron.right")
                                                .font(.caption2).fontWeight(.bold)
                                        }
                                        .foregroundStyle(theme.error)
                                    }
                                    .padding(.top, 4)
                                }
                            }
                            
                            Spacer()
                            Button(action: { familyViewModel.errorMessage = nil }) {
                                Image(systemName: "xmark")
                                    .foregroundStyle(theme.error)
                            }
                        }
                        .padding()
                        .background(theme.errorContainer.opacity(0.85))
                        .background(.regularMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }

                    // MARK: 1. Profil & Weckton (Combined Card)
                    profileAndAlarmCard

                    // MARK: 2. Familie & Konto
                    familyAndAccountCard

                    // MARK: 3. Darstellung (Sprache, Theme, Tooltips, Push)
                    displayCard

                    // MARK: 4. Unterstützen (Donations)
                    donationCard

                    // MARK: 5. Hilfe & Feedback
                    helpCard

                    // MARK: 6. Konto (Logout, Account löschen) – ganz unten wie Android
                    if !authViewModel.isAnonymous {
                        accountCard
                    }

                    // MARK: 7. Admin-Testmenü (nur für Admins)
                    if authViewModel.currentUserEmail == "daniel.notthoff@gmail.com" {
                        adminCard
                    }

                    // MARK: 8. Footer
                    footerSection
                }
                .padding(16)
            }
            .background(
                LinearGradient(
                    colors: isDark
                        ? [theme.surface, theme.background]
                        : [theme.primaryContainer.opacity(0.5), theme.background],
                    startPoint: .top, endPoint: .bottom
                ).ignoresSafeArea()
            )
            .navigationTitle(L.settingsTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L.backDesc) { dismiss() }
                }
            }
            .sheet(isPresented: $showFeedback) { FeedbackView() }
            .sheet(isPresented: $showShareSheet) { ActivityViewController(activityItems: [shareContent]) }
            .sheet(isPresented: $showProfilePicker) { profilePickerSheet }
            .sheet(isPresented: $showDonationSheet) { donationSheet }
            .alert(L.settingsLeaveFamily, isPresented: $showLeaveFamilyAlert) {
                Button(L.settingsLeaveFamily, role: .destructive) {
                    familyViewModel.leaveFamily()
                    appState.route = .familySetup
                    dismiss()
                }
                Button(L.cancelButton, role: .cancel) {}
            }
            .alert(L.settingsDeleteFamilyDialogTitle, isPresented: $showDeleteFamilyAlert) {
                Button(L.settingsDeleteFamilyDialogConfirm, role: .destructive) {
                    let hasOthers = familyViewModel.members.contains { $0.id != familyViewModel.myMemberId }
                    if hasOthers {
                        showDeleteFamilyFinalAlert = true
                    } else {
                        familyViewModel.deleteFamily { success in
                            if success { appState.route = .familySetup; dismiss() }
                        }
                    }
                }
                Button(L.settingsDeleteFamilyDialogCancel, role: .cancel) {}
            } message: { Text(L.settingsDeleteFamilyDialogText) }
            .alert(L.settingsDeleteFamilyWarningTitle, isPresented: $showDeleteFamilyFinalAlert) {
                Button(L.settingsDeleteFamilyWarningConfirm, role: .destructive) {
                    familyViewModel.deleteFamily { success in
                        if success { appState.route = .familySetup; dismiss() }
                    }
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: { Text(L.settingsDeleteFamilyWarningText) }
            .alert(L.s("settings_steal_title"), isPresented: $showStealAlert, presenting: memberToSteal) { member in
                Button(L.s("settings_steal_confirm"), role: .destructive) {
                    familyViewModel.setMyMemberId(member.id, force: true) { _ in }
                    showProfilePicker = false
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: { member in
                Text(String(format: L.s("settings_steal_text"), member.name))
            }
            .onChange(of: authViewModel.isAnonymous) { _, isAnon in
                // Nach erfolgreichem Account-Linking: Login-Sheet schließen, Daten neu laden
                if !isAnon {
                    showLoginSheet = false
                    familyViewModel.reloadForNewUser()
                    dismiss()
                }
            }
            .onChange(of: scenePhase) { _, newPhase in
                if newPhase == .active {
                    if familyViewModel.errorMessage == L.errorAlarmPermission {
                        UNUserNotificationCenter.current().getNotificationSettings { settings in
                            if settings.authorizationStatus == .authorized {
                                DispatchQueue.main.async {
                                    familyViewModel.clearErrorMessage()
                                    familyViewModel.recalculateSchedule()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: - 1. Profil & Weckton
    @ViewBuilder
    private var profileAndAlarmCard: some View {
        settingsCard {
            // Header
            settingsSectionHeader(icon: "person.fill", title: L.settingsProfileTitle)

            Text(L.settingsProfileDesc)
                .font(.subheadline).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                .padding(.bottom, 8)

            // Profile picker button
            let selectedMember = familyViewModel.members.first { $0.id == familyViewModel.myMemberId }
            let label = familyViewModel.members.isEmpty
                ? L.settingsNoMembers
                : (selectedMember?.name ?? L.settingsPleaseSelect)

            Button(action: {
                if familyViewModel.isOffline {
                    familyViewModel.errorMessage = L.errorProfileClaimOffline
                } else if !familyViewModel.members.isEmpty {
                    showProfilePicker = true
                }
            }) {
                HStack {
                    Text(label).font(.body)
                    Spacer()
                    Image(systemName: "person.fill").font(.caption)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 16).padding(.vertical, 12)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(theme.outline.opacity(0.4), lineWidth: 1))
            .disabled(familyViewModel.members.isEmpty)

            // Alarm sound picker is removed for iOS due to system limitations.
            // iOS users will use the default system notification sound or bundled default sound.
        }
    }

    // MARK: - 2. Familie & Konto
    @ViewBuilder
    private var familyAndAccountCard: some View {
        settingsCard {
            settingsSectionHeader(icon: "person.3.fill", title: L.settingsAccountTitle)

            // Anonymous → Link Account (zeigt Login-Sheet, kein direkter Google-Call wegen VC-Crash in Sheet)
            if authViewModel.isAnonymous {
                Button(action: { showLoginSheet = true }) {
                    HStack {
                        Image(systemName: "person.badge.plus")
                        Text(L.s("settings_anonymous_login_button"))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(theme.primary)
                    .foregroundStyle(theme.onPrimary)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .padding(.bottom, 12)
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

                // Tooltip E
                if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipInviteSeen {
                    TooltipBubble(text: L.tooltipInviteCode) {
                        familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyInvite)
                    }
                }

                // Share
                Button(action: {
                    if authViewModel.isAnonymous {
                        familyViewModel.errorMessage = L.s("settings_share_code_locked")
                    } else {
                        shareContent = L.settingsShareMessage(fName, code)
                        showShareSheet = true
                    }
                }) {
                    HStack {
                        Image(systemName: "person.3.fill").font(.caption)
                        Text(L.settingsShareCode)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(theme.primary)
                    .foregroundStyle(theme.onPrimary)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .padding(.bottom, 8)
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
                    .padding(.vertical, 10)
                }
                .foregroundStyle(theme.onSurface)
                .overlay(Capsule().stroke(theme.outline.opacity(0.4), lineWidth: 1))

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
                    .padding(.vertical, 10)
                }
                .foregroundStyle(familyViewModel.isAdmin ? theme.error : theme.outline)
                .overlay(Capsule().stroke((familyViewModel.isAdmin ? theme.error : theme.outline).opacity(0.6), lineWidth: 1))
            }
        }
    }

    // MARK: - 3. Darstellung
    @ViewBuilder
    private var displayCard: some View {
        settingsCard {
            settingsSectionHeader(icon: "slider.horizontal.3", title: L.settingsDisplayTitle)

            // Language
            settingsSectionLabel(icon: "globe", text: L.settingsLanguageTitle)
            NavigationLink(destination: LanguagePickerView()) {
                HStack {
                    Text(languageName(for: familyViewModel.language)).font(.body)
                    Spacer()
                    Image(systemName: "slider.horizontal.3").font(.caption)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 16).padding(.vertical, 12)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(theme.outline.opacity(0.4), lineWidth: 1))

            Spacer().frame(height: 16)

            // Theme – Segmented Control (matches Android SegmentedButtonRow)
            settingsSectionLabel(icon: nil, text: L.settingsAppearanceTitle)
            Picker("", selection: $appState.themePreference) {
                Image(systemName: "sun.max.fill").tag("light")
                Image(systemName: "circle.lefthalf.filled").tag("system")
                Image(systemName: "moon.fill").tag("dark")
            }
            .pickerStyle(.segmented)
            .onChange(of: appState.themePreference) { _, newValue in
                familyViewModel.setThemePreference(newValue)
            }

            Spacer().frame(height: 16)

            // Tooltips
            settingsSectionLabel(icon: "lightbulb.fill", text: L.settingsTooltipsTitle)
            Toggle(L.settingsTooltipsLabel, isOn: Binding(
                get: { familyViewModel.tooltipsEnabled },
                set: { familyViewModel.setTooltipsEnabled($0) }
            ))
            .tint(theme.secondary)

            if familyViewModel.tooltipsEnabled {
                Button(L.settingsTooltipsReset) {
                    familyViewModel.resetAllTooltips()
                }
                .font(.subheadline)
                .foregroundStyle(theme.primary)
                .frame(maxWidth: .infinity, alignment: .trailing)
            }

            Spacer().frame(height: 16)

            // Push Notifications (placeholder)
            settingsSectionLabel(icon: "bell.fill", text: L.s("settings_push_title"))
            Toggle(L.s("settings_push_label"), isOn: .constant(true))
                .tint(theme.secondary)
                .disabled(true) // Requires Apple Dev Account
        }
    }

    // MARK: - 4. Donations (non-functional placeholder)
    @ViewBuilder
    private var donationCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            settingsSectionHeader(icon: "heart.fill", title: L.settingsSupportTitle)

            Text(L.s("settings_support_desc"))
                .font(.subheadline).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))

            Button(action: { showDonationSheet = true }) {
                HStack {
                    Image(systemName: "heart.fill").font(.caption)
                    Text(L.s("settings_support_donate"))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(theme.primary)
                .foregroundStyle(theme.onPrimary)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
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

    // MARK: - 5. Hilfe & Feedback
    @ViewBuilder
    private var helpCard: some View {
        settingsCard {
            settingsSectionHeader(icon: "doc.text.fill", title: L.s("settings_help_feedback_title"))

            // Hilfe & Feedback Buttons
            VStack(spacing: 8) {
                // Restart Tour
                Button(action: {
                    appState.onboardingCompleted = false
                    UserDefaults.standard.set(false, forKey: "onboarding_completed")
                    appState.route = .onboarding
                    dismiss()
                }) {
                    Text(L.s("settings_start_onboarding"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                }
                .foregroundStyle(theme.onSurface)
                .overlay(Capsule().stroke(theme.outline.opacity(0.4), lineWidth: 1))

                // Feedback
                Button(action: { showFeedback = true }) {
                    HStack(spacing: 8) {
                        Image(systemName: "text.bubble")
                        Text(L.settingsFeedbackButton)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .foregroundStyle(theme.onSurface)
                .overlay(Capsule().stroke(theme.outline.opacity(0.4), lineWidth: 1))

                // E-Mail Support
                Button(action: {
                    if let url = URL(string: "mailto:hello@familienwecker.de?subject=FamWake%20Feedback") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "envelope")
                        Text(L.s("settings_support_button"))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .foregroundStyle(theme.onSurface)
                .overlay(Capsule().stroke(theme.outline.opacity(0.4), lineWidth: 1))
            }
        }
    }

    // MARK: - 6. Konto (ganz unten wie Android)
    @ViewBuilder
    private var accountCard: some View {
        settingsCard {
            settingsSectionHeader(icon: "person.circle", title: L.s("settings_account_section"))

            // Logout
            Button(action: {
                familyViewModel.reloadForNewUser()
                authViewModel.logout()
                dismiss()
            }) {
                HStack(spacing: 8) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                    Text(L.settingsLogout)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
            }
            .foregroundStyle(theme.error)
            .overlay(Capsule().stroke(theme.error.opacity(0.6), lineWidth: 1))

            // Delete Account Info
            if let deleteUrl = URL(string: L.settingsDeleteAccountUrl) {
                Link(destination: deleteUrl) {
                    HStack(spacing: 8) {
                        Image(systemName: "arrow.up.forward.square")
                        Text(L.settingsDeleteAccount)
                    }
                    .font(.caption)
                    .frame(maxWidth: .infinity, alignment: .center)
                }
                .foregroundStyle(theme.onSurface.opacity(0.8))
                .padding(.top, 12)
            }
        }
    }

    // MARK: - 7. Admin-Testmenü
    @ViewBuilder
    private var adminCard: some View {
        settingsCard {
            settingsSectionHeader(icon: "wrench.and.screwdriver.fill", title: "Admin")

            // Test-Alarm: alle Member löschen, neuen anlegen mit Weckzeit in 2 Min
            Button(action: { setupTestAlarm() }) {
                HStack {
                    Image(systemName: "alarm.fill")
                    Text("⏰ Test-Wecker (2 Min)")
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
            .tint(theme.tertiary)
            .clipShape(Capsule())
        }
    }

    private func setupTestAlarm() {
        familyViewModel.setupTestAlarmAndMembers { status in
            print("Admin Test Wecker: \(status)")
        }
    }

    // MARK: - 8. Footer
    @ViewBuilder
    private var footerSection: some View {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        VStack(spacing: 6) {
            Text("\(L.appNameShort) v\(version)")
            Text(L.s("settings_footer_copyright"))
            Text(L.s("settings_footer_rights"))

            Spacer().frame(height: 4)

            ViewThatFits {
                HStack(spacing: 16) {
                    footerLinks
                }
                VStack(spacing: 6) {
                    footerLinks
                }
            }
        }
        .font(.caption)
        .foregroundStyle(theme.outline)
        .frame(maxWidth: .infinity)
        .multilineTextAlignment(.center)
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private var footerLinks: some View {
        Button(L.s("settings_terms_of_use")) {
            if let url = URL(string: L.settingsTermsOfUseUrl) { UIApplication.shared.open(url) }
        }.buttonStyle(.borderless)
        Button(L.s("settings_privacy_policy")) {
            if let url = URL(string: L.settingsPrivacyPolicyUrl) { UIApplication.shared.open(url) }
        }.buttonStyle(.borderless)
        Button(L.s("settings_imprint")) {
            if let url = URL(string: L.s("settings_imprint_url")) { UIApplication.shared.open(url) }
        }.buttonStyle(.borderless)
    }

    // MARK: - Profile Picker Sheet (iOS-native BottomSheet)
    @ViewBuilder
    private var profilePickerSheet: some View {
        NavigationStack {
            let currentUid = authViewModel.currentUserId
            List {
                // Unclaim-Option: "Kein Profil" (Android bietet das ebenfalls an)
                if familyViewModel.myMemberId != nil {
                    Button(action: {
                        familyViewModel.myMemberId = nil
                        UserDefaults.standard.removeObject(forKey: "my_member_id")
                        familyViewModel.isAlarmEnabled = false
                        UserDefaults.standard.set(false, forKey: "alarm_enabled")
                        familyViewModel.recalculateSchedule()
                        showProfilePicker = false
                    }) {
                        HStack {
                            ZStack {
                                Circle()
                                    .fill(theme.surfaceVariant)
                                    .frame(width: 36, height: 36)
                                Image(systemName: "person.slash")
                                    .font(.caption)
                                    .foregroundStyle(theme.onSurfaceVariant)
                            }
                            Text(L.s("settings_no_profile"))
                                .font(.body).fontWeight(.medium)
                            Spacer()
                        }
                    }
                    .foregroundStyle(theme.onSurface)
                }
                
                ForEach(familyViewModel.members) { member in
                    let isClaimedByOther = member.claimedByUserId != nil && member.claimedByUserId != currentUid
                    let isSelected = member.id == familyViewModel.myMemberId

                    Button(action: {
                        if isClaimedByOther {
                            memberToSteal = member
                            showStealAlert = true
                        } else {
                            familyViewModel.setMyMemberId(member.id) { _ in }
                            showProfilePicker = false
                        }
                    }) {
                        HStack {
                            // Avatar
                            ZStack {
                                Circle()
                                    .fill(isSelected ? theme.tertiary : theme.surfaceVariant)
                                    .frame(width: 36, height: 36)
                                Text(member.name.prefix(1).uppercased())
                                    .font(.subheadline).fontWeight(.bold)
                                    .foregroundStyle(isSelected ? theme.onTertiary : theme.onSurfaceVariant)
                            }

                            VStack(alignment: .leading, spacing: 2) {
                                Text(member.name).font(.body).fontWeight(.medium)
                                if isClaimedByOther {
                                    Text(L.settingsAlreadyClaimed)
                                        .font(.caption).foregroundStyle(theme.error)
                                }
                            }

                            Spacer()

                            if isSelected {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(theme.secondary)
                            }
                        }
                    }
                    .foregroundStyle(theme.onSurface)
                }
            }
            .navigationTitle(L.settingsProfileTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L.cancelButton) { showProfilePicker = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    // MARK: - Donation Sheet (Placeholder)
    @ViewBuilder
    private var donationSheet: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                Image(systemName: "heart.fill")
                    .font(.system(size: 60))
                    .foregroundStyle(theme.tertiary)

                Text(L.settingsSupportTitle)
                    .font(.title2).fontWeight(.bold)
                    .foregroundStyle(theme.onSurface)

                Text(L.s("settings_support_desc"))
                    .font(.body)
                    .foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                // Placeholder packages
                VStack(spacing: 12) {
                    donationOption(emoji: "☕", title: L.s("settings_donate_coffee"), price: "1,99 €")
                    donationOption(emoji: "🍕", title: L.s("settings_donate_pizza"), price: "4,99 €")
                    donationOption(emoji: "🎉", title: L.s("settings_donate_party"), price: "9,99 €")
                }
                .padding(.horizontal)

                Text(L.s("settings_donate_coming_soon"))
                    .font(.caption).foregroundStyle(theme.outline)

                Spacer()
            }
            .navigationTitle(L.settingsSupportTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L.cancelButton) { showDonationSheet = false }
                }
            }
        }
    }

    // MARK: - Helpers

    @ViewBuilder
    private func settingsCard(@ViewBuilder content: () -> some View) -> some View {
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

    @ViewBuilder
    private func settingsSectionHeader(icon: String, title: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .foregroundStyle(theme.primary)
            Text(title)
                .font(.headline).fontWeight(.bold)
                .foregroundStyle(theme.onSurface)
        }
        .padding(.bottom, 4)
    }

    @ViewBuilder
    private func settingsSectionLabel(icon: String?, text: String) -> some View {
        HStack(spacing: 6) {
            if let icon {
                Image(systemName: icon)
                    .font(.caption2).foregroundStyle(theme.primary)
            }
            Text(text)
                .font(.caption).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
        }
    }

    @ViewBuilder
    private func donationOption(emoji: String, title: String, price: String) -> some View {
        HStack {
            Text(emoji).font(.title2)
            Text(title).font(.body).fontWeight(.medium)
            Spacer()
            Text(price).font(.subheadline).foregroundStyle(theme.outline)
        }
        .padding(16)
        .background(
            isDark ? theme.primaryContainer : theme.surfaceVariant.opacity(0.3)
        )
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(theme.outline.opacity(0.15), lineWidth: 0.5)
        )
    }

    private func languageName(for code: String) -> String {
        switch code {
        case "de":  return "Deutsch"
        case "en":  return "English"
        case "da":  return "Dansk"
        case "es":  return "Español"
        case "fr":  return "Français"
        case "it":  return "Italiano"
        case "ja":  return "日本語"
        case "ko":  return "한국어"
        case "nl":  return "Nederlands"
        case "no":  return "Norsk"
        case "pl":  return "Polski"
        case "pt":  return "Português"
        case "ru":  return "Русский"
        case "sv":  return "Svenska"
        case "tr":  return "Türkçe"
        case "uk":  return "Українська"
        case "zh":  return "中文"
        case "id":  return "Bahasa Indonesia"
        case "vi":  return "Tiếng Việt"
        case "bn":  return "বাংলা"
        case "mr":  return "मराठी"
        case "hi":  return "हिन्दी"
        case "gsw": return "Schwiizerdütsch"
        case "swg": return "Schwäbsch"
        case "ksh": return "Ruhrpott"
        default:    return L.settingsLanguageSystem
        }
    }
}

// MARK: - Language Picker View (NavigationLink-Destination)
private struct LanguagePickerView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    private let dialectLanguages: [(String, String)] = [
        ("ksh", "Ruhrpott"), ("swg", "Schwäbsch"), ("gsw", "Schwiizerdütsch")
    ]

    var body: some View {
        List {
            Section {
                langRow(code: "system", label: L.settingsLanguageSystem)
            }
            Section {
                langRow(code: "de", label: "Deutsch")
                langRow(code: "en", label: "English")
                langRow(code: "da", label: "Dansk")
                langRow(code: "es", label: "Español")
                langRow(code: "fr", label: "Français")
                langRow(code: "it", label: "Italiano")
                langRow(code: "ja", label: "日本語")
                langRow(code: "ko", label: "한국어")
                langRow(code: "nl", label: "Nederlands")
                langRow(code: "no", label: "Norsk")
                langRow(code: "pl", label: "Polski")
                langRow(code: "pt", label: "Português")
                langRow(code: "ru", label: "Русский")
                langRow(code: "sv", label: "Svenska")
                langRow(code: "tr", label: "Türkçe")
                langRow(code: "uk", label: "Українська")
                langRow(code: "zh", label: "中文")
                langRow(code: "id", label: "Bahasa Indonesia")
                langRow(code: "vi", label: "Tiếng Việt")
                langRow(code: "bn", label: "বাংলা")
                langRow(code: "mr", label: "मराठी")
                langRow(code: "hi", label: "हिन्दी")
            }
            Section("Dialekte 🎙️") {
                ForEach(dialectLanguages, id: \.0) { item in
                    langRow(code: item.0, label: item.1)
                }
            }
        }
        .navigationTitle(L.settingsLanguageTitle)
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func langRow(code: String, label: String) -> some View {
        Button(action: {
            familyViewModel.setLanguage(code)
            appState.setLanguage(code)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { dismiss() }
        }) {
            HStack {
                Text(label)
                Spacer()
                if familyViewModel.language == code {
                    Image(systemName: "checkmark")
                        .foregroundStyle(theme.tertiary)
                        .fontWeight(.semibold)
                }
            }
        }
        .foregroundStyle(theme.onSurface)
    }
}

// MARK: - UIKit Share Sheet Wrapper
struct ActivityViewController: UIViewControllerRepresentable {
    var activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
