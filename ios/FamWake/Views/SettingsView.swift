import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var donationViewModel: DonationViewModel
    @Environment(\.dismiss) var dismiss

    @State private var showFeedback = false
    @State private var showLeaveFamilyAlert = false
    @State private var showDeleteFamilyAlert = false
    @State private var showDeleteFamilyFinalAlert = false
    @State private var showDeleteAccountAlert = false
    @State private var showShareSheet = false
    @State private var shareContent = ""

    var body: some View {
        NavigationStack {
            List {

                // MARK: 1. Profil
                Section(L.settingsProfileTitle) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(L.settingsProfileDesc).font(.caption).foregroundStyle(.secondary)
                        profilePicker
                    }
                }

                // MARK: 2. Weckton
                Section(L.settingsAlarmTitle) {
                    alarmSoundPicker
                }

                // MARK: 3. Familie & Konto
                Section(L.settingsAccountTitle) {
                    if let code = familyViewModel.joinCode {
                        VStack(alignment: .leading, spacing: 4) {
                            let fName = familyViewModel.familyName ?? L.s("settings_fallback_username")
                            HStack {
                                Text(L.settingsJoinCodeName(fName)).font(.subheadline)
                                Spacer()
                                Text(code)
                                    .font(.title3).fontWeight(.black)
                                    .foregroundStyle(Color.sunriseOrange500)
                            }
                            if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipInviteSeen {
                                TooltipBubble(text: L.tooltipInviteCode) {
                                    familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyInvite)
                                }
                            }
                        }

                        Button(action: {
                            let familyName = familyViewModel.familyName ?? ""
                            shareContent = L.settingsShareMessage(familyName, code)
                            showShareSheet = true
                        }) {
                            Label(L.settingsShareCode, systemImage: "square.and.arrow.up")
                        }
                    }

                    Button(L.settingsLeaveFamily, role: .destructive) {
                        showLeaveFamilyAlert = true
                    }

                    Button(L.settingsDeleteFamily, role: .destructive) {
                        showDeleteFamilyAlert = true
                    }

                    if authViewModel.isAnonymous {
                        Button(L.loginWithGoogle) {
                            authViewModel.signInWithGoogle()
                        }
                    } else {
                        Button(L.settingsLogout, role: .destructive) {
                            authViewModel.logout()
                            dismiss()
                        }
                    }

                    // Konto löschen (Info) – Textlink, normal (kein Rot)
                    if let deleteUrl = URL(string: L.settingsDeleteAccountUrl) {
                        Link(destination: deleteUrl) {
                            HStack {
                                Text(L.settingsDeleteAccount)
                                Spacer()
                                Image(systemName: "info.circle").foregroundStyle(.secondary)
                            }
                        }
                        .foregroundStyle(.primary)
                    }
                }

                // MARK: 4. Einstellungen (Sprache, Theme, Tipps)
                Section(L.settingsDisplayTitle) {
                    languagePicker
                    themePicker

                    Toggle(L.settingsTooltipsLabel, isOn: Binding(
                        get: { familyViewModel.tooltipsEnabled },
                        set: { familyViewModel.setTooltipsEnabled($0) }
                    ))

                    if familyViewModel.tooltipsEnabled {
                        Button(L.settingsTooltipsReset) {
                            familyViewModel.resetAllTooltips()
                        }
                        .font(.subheadline)
                    }
                }

                // MARK: 5. Hilfe & Feedback
                Section(L.s("settings_help_feedback_title")) {
                    Button(action: {
                        appState.onboardingCompleted = false
                        UserDefaults.standard.set(false, forKey: "onboarding_completed")
                        appState.route = .onboarding
                        dismiss()
                    }) {
                        Label(L.s("settings_start_onboarding"), systemImage: "map")
                    }

                    Button(action: { showFeedback = true }) {
                        Label(L.s("settings_feedback_button"), systemImage: "text.bubble")
                    }

                    Button(action: {
                        if let url = URL(string: "mailto:hello@familienwecker.de?subject=FamWake%20Feedback") {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        Label(L.s("settings_support_button"), systemImage: "envelope")
                    }
                }

                // MARK: 6. Footer – Version + Copyright + Rechtliches als Textlinks
                Section {
                    let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
                    let termsUrl  = URL(string: L.settingsTermsOfUseUrl)
                    let privacyUrl = URL(string: L.settingsPrivacyPolicyUrl)
                    let imprintUrl = URL(string: L.s("settings_imprint_url"))
                    VStack(spacing: 6) {
                        Text(String(format: L.s("settings_footer_version"), version))
                        Text(L.s("settings_footer_copyright"))
                        Text(L.s("settings_footer_rights"))

                        Spacer().frame(height: 4)

                        Button(L.s("settings_terms_of_use")) {
                            if let url = URL(string: L.settingsTermsOfUseUrl) {
                                UIApplication.shared.open(url)
                            }
                        }.buttonStyle(.borderless)
                        Button(L.s("settings_privacy_policy")) {
                            if let url = URL(string: L.settingsPrivacyPolicyUrl) {
                                UIApplication.shared.open(url)
                            }
                        }.buttonStyle(.borderless)
                        Button(L.s("settings_imprint")) {
                            if let url = URL(string: L.s("settings_imprint_url")) {
                                UIApplication.shared.open(url)
                            }
                        }.buttonStyle(.borderless)
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                    .padding(.vertical, 4)
                    .listRowBackground(Color.clear)
                }

            }
            .navigationTitle(L.settingsTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L.backDesc) { dismiss() }
                }
            }
            .sheet(isPresented: $showFeedback) {
                FeedbackView()
            }
            .sheet(isPresented: $showShareSheet) {
                ActivityViewController(activityItems: [shareContent])
            }
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
                    showDeleteFamilyFinalAlert = true
                }
                Button(L.settingsDeleteFamilyDialogCancel, role: .cancel) {}
            } message: {
                Text(L.settingsDeleteFamilyDialogText)
            }
            .alert(L.settingsDeleteFamilyWarningTitle, isPresented: $showDeleteFamilyFinalAlert) {
                Button(L.settingsDeleteFamilyWarningConfirm, role: .destructive) {
                    familyViewModel.deleteFamily { success in
                        if success {
                            appState.route = .familySetup
                            dismiss()
                        }
                    }
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: {
                Text(L.settingsDeleteFamilyWarningText)
            }
        }
    }

    // MARK: - Profile Picker
    @ViewBuilder
    private var profilePicker: some View {
        if familyViewModel.members.isEmpty {
            Text(L.settingsNoMembers).font(.subheadline).foregroundStyle(.secondary)
        } else {
            Menu {
                Button(action: {}) {
                    Label(L.settingsPleaseSelect, systemImage: "questionmark.circle")
                }
                Divider()
                ForEach(familyViewModel.members) { member in
                    let isMine = member.id == familyViewModel.myMemberId
                    let isClaimed = !isMine && member.claimedByUserId != nil
                    Button(action: {
                        if !isClaimed {
                            familyViewModel.setMyMemberId(member.id) { _ in }
                        }
                    }) {
                        HStack {
                            Text(member.name)
                            if isMine { Text("✓") }
                            if isClaimed { Text(" (\(L.settingsAlreadyClaimed))") }
                        }
                    }
                    .disabled(isClaimed)
                }
            } label: {
                HStack {
                    let myName = familyViewModel.members.first(where: { $0.id == familyViewModel.myMemberId })?.name
                    Text(myName ?? L.settingsPleaseSelect)
                    Spacer()
                    Image(systemName: "chevron.up.chevron.down").foregroundStyle(.secondary)
                }
            }
        }
    }

    // MARK: - Language Picker → NavigationLink
    @ViewBuilder
    private var languagePicker: some View {
        NavigationLink(destination: LanguagePickerView()) {
            HStack {
                Text(L.settingsLanguageTitle)
                Spacer()
                Text(languageName(for: familyViewModel.language)).foregroundStyle(.secondary)
            }
        }
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

    // MARK: - Theme Picker
    @ViewBuilder
    private var themePicker: some View {
        let themes: [(String, String)] = [
            ("system", L.s("settings_theme_system")),
            ("dark",   L.settingsThemeDark),
            ("light",  L.settingsThemeLight)
        ]
        Picker(L.settingsAppearanceTitle, selection: Binding(
            get: { familyViewModel.themePreference },
            set: { familyViewModel.setThemePreference($0); appState.setTheme($0) }
        )) {
            ForEach(themes, id: \.0) { theme in
                Text(theme.1).tag(theme.0)
            }
        }
    }

    // MARK: - Alarm Sound Picker
    @ViewBuilder
    private var alarmSoundPicker: some View {
        let sounds: [(String?, String)] = [
            (nil,                  L.settingsAlarmDefault),
            ("alarm_classic.caf", "Classic Alarm"),
            ("alarm_gentle.caf",  "Gentle Rise"),
            ("alarm_digital.caf", "Digital Beep")
        ]
        Picker(L.settingsAlarmTitle, selection: Binding(
            get: { familyViewModel.alarmSoundUri },
            set: { familyViewModel.setAlarmSoundUri($0) }
        )) {
            ForEach(sounds, id: \.0) { sound in
                Text(sound.1).tag(sound.0)
            }
        }
    }
}

// MARK: - Language Picker View (NavigationLink-Destination)
private struct LanguagePickerView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) var dismiss

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
                        .foregroundStyle(Color.accentColor)
                        .fontWeight(.semibold)
                }
            }
        }
        .foregroundStyle(.primary)
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
