import SwiftUI
import Combine

/// Globaler App-Zustand – SharedPreferences-Äquivalent
@MainActor
class AppState: ObservableObject {
    @Published var route: AppRoute = .loading
    @Published var themePreference: String = UserDefaults.standard.string(forKey: "theme_preference") ?? "system"
    @Published var language: String = UserDefaults.standard.string(forKey: "language") ?? "system"
    @Published var onboardingCompleted: Bool = UserDefaults.standard.bool(forKey: "onboarding_completed")
    /// Wird bei Sprachwechsel inkrementiert → zwingt alle L.xxx-abhängigen Views zum Re-Render
    @Published var languageId: Int = 0

    var colorScheme: ColorScheme? {
        switch themePreference {
        case "dark": return .dark
        case "light": return .light
        default: return nil
        }
    }

    func markOnboardingDone() {
        onboardingCompleted = true
        UserDefaults.standard.set(true, forKey: "onboarding_completed")
    }

    func setTheme(_ theme: String) {
        themePreference = theme
        UserDefaults.standard.set(theme, forKey: "theme_preference")
    }

    func setLanguage(_ lang: String) {
        language = lang
        UserDefaults.standard.set(lang, forKey: "language")
        LanguageManager.shared.apply(lang)
        languageId &+= 1   // Overflow-sicherer Increment → triggert View-Rebuild
    }

    func load(authViewModel: AuthViewModel, familyViewModel: FamilyViewModel) async {
        // Kurze Pause damit Firebase Auth den Zustand laden kann
        try? await Task.sleep(nanoseconds: 800_000_000) // 0.8s
        if case .loading = route {
            if !onboardingCompleted {
                route = .onboarding
            } else if !authViewModel.isLoggedIn {
                route = .login
            } else if familyViewModel.hasFamilyId {
                route = .main
            } else {
                route = .familySetup
            }
        }
    }
}
