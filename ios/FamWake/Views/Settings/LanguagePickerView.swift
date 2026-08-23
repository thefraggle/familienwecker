import SwiftUI

struct LanguagePickerView: View {
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
