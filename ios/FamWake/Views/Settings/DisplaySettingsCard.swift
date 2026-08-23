import SwiftUI

struct DisplaySettingsCard: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showResetTipsAlert: Bool
    @Binding var showTimePickerSheet: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        SettingsCardContainer {
            SettingsSectionHeader(icon: "paintbrush.fill", title: L.settingsDisplayTitle)

            // Theme Picker
            VStack(alignment: .leading, spacing: 4) {
                SettingsSectionLabel(icon: "circle.lefthalf.filled", text: L.s("settings_theme_title"))
                Picker(L.s("settings_theme_title"), selection: Binding(
                    get: { familyViewModel.themeMode },
                    set: {
                        familyViewModel.setThemeMode($0)
                        appState.themeMode = $0
                    }
                )) {
                    Text(L.s("settings_theme_system")).tag("system")
                    Text(L.settingsThemeLight).tag("light")
                    Text(L.settingsThemeDark).tag("dark")
                }
                .pickerStyle(.segmented)
                .accessibilityLabel(L.s("accessibility_theme_picker"))
            }

            Divider().background(theme.outline.opacity(0.15))

            // Time Format
            VStack(alignment: .leading, spacing: 4) {
                SettingsSectionLabel(icon: "clock.fill", text: L.settingsTimeFormatTitle)
                Button(action: { showTimePickerSheet = true }) {
                    HStack {
                        Text(timeFormatLabel(familyViewModel.timeFormat)).font(.body)
                        Spacer()
                        Image(systemName: "chevron.right").font(.caption).foregroundStyle(theme.outline)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .padding(.horizontal, 16)
                }
                .foregroundStyle(theme.onSurface)
                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(theme.outline.opacity(0.4), lineWidth: 1))
                .accessibilityLabel(L.s("accessibility_time_format_button"))
            }

            Divider().background(theme.outline.opacity(0.15))

            // Language Navigation Link
            NavigationLink(destination: LanguagePickerView()) {
                HStack {
                    Image(systemName: "globe")
                        .foregroundStyle(theme.primary)
                    Text(L.settingsLanguageTitle)
                        .font(.body)
                    Spacer()
                    Text(languageName(for: familyViewModel.language))
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurfaceVariant)
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(theme.outline)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 48)
            }
            .foregroundStyle(theme.onSurface)
            .accessibilityLabel(L.s("accessibility_language_picker"))

            Divider().background(theme.outline.opacity(0.15))

            // Tooltip Toggle
            Toggle(isOn: Binding(
                get: { familyViewModel.tooltipsEnabled },
                set: { familyViewModel.setTooltipsEnabled($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L.settingsTooltipsTitle).font(.body)
                    Text(L.settingsTooltipsDesc).font(.caption).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                }
            }
            .tint(theme.secondary)
            .accessibilityLabel(L.s("accessibility_tooltips_toggle"))

            // Reset Tooltips Button
            Button(action: { showResetTipsAlert = true }) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.counterclockwise")
                    Text(L.settingsResetTips)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outline.opacity(0.4), lineWidth: 1))
            .accessibilityLabel(L.s("accessibility_reset_tooltips"))

            Divider().background(theme.outline.opacity(0.15))

            // Push Notification Toggle
            Toggle(isOn: Binding(
                get: { familyViewModel.pushEnabled },
                set: { familyViewModel.setPushEnabled($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L.settingsPushTitle).font(.body)
                    Text(L.settingsPushDesc).font(.caption).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                }
            }
            .tint(theme.secondary)
            .accessibilityLabel(L.s("accessibility_push_toggle"))
        }
    }

    private func timeFormatLabel(_ format: String) -> String {
        switch format {
        case "12h": return L.s("settings_time_format_12")
        case "24h": return L.s("settings_time_format_24")
        default:    return L.s("settings_time_format_auto")
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
}
