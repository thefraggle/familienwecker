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
    @Published var pushNotificationsEnabled: Bool = UserDefaults.standard.object(forKey: "push_notifications_enabled") as? Bool ?? true

    @Published var isRinging: Bool = false
    @Published var ringingMemberId: String = ""
    @Published var ringingMemberName: String = ""

    var colorScheme: ColorScheme? {
        switch themePreference {
        case "dark": return .dark
        case "light": return .light
        default: return nil
        }
    }

    func startRinging(memberId: String, memberName: String) {
        self.ringingMemberId = memberId
        self.ringingMemberName = memberName
        self.isRinging = true
        AlarmService.shared.playAlarm(soundUri: nil)
    }
    
    func stopRinging() {
        self.isRinging = false
        AlarmService.shared.stopAlarm()
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

    func setPushNotificationsEnabled(_ enabled: Bool) {
        pushNotificationsEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "push_notifications_enabled")
        if enabled {
            MessagingService.shared.refreshAndSaveToken()
        } else {
            // Wenn Push deaktiviert wird, löschen wir das Token aus Firestore
            MessagingService.shared.deleteTokenOnLogout()
        }
    }

    func load(authViewModel: AuthViewModel, familyViewModel: FamilyViewModel) async {
        if case .loading = route {
            // Warten bis Firebase Auth initialisiert ist (maximal 5 Sekunden)
            var attempts = 0
            while authViewModel.authState == .loading && attempts < 50 {
                try? await Task.sleep(nanoseconds: 100_000_000) // 0.1s
                attempts += 1
            }
            
            // Auto-recover onboarding state if user is already logged in (e.g. after reinstall)
            if authViewModel.isLoggedIn && !onboardingCompleted {
                markOnboardingDone()
            }
            
            // Wait for familyId restore from Firestore before routing, if we are logged in but missing local familyId
            if authViewModel.isLoggedIn && !familyViewModel.hasFamilyId {
                await familyViewModel.restoreUserContextIfNeeded()
            }

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

extension Notification.Name {
    static let showRingingView = Notification.Name("showRingingView")
    static let stopAlarmFromNotification = Notification.Name("stopAlarmFromNotification")
    static let snoozeAlarmFromNotification = Notification.Name("snoozeAlarmFromNotification")
}
