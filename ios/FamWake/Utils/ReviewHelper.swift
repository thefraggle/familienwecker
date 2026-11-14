import Foundation
import StoreKit
import UIKit
import Aptabase

/// Steuert den intelligenten In-App-Bewertungsdialog analog zu Android `ReviewHelper`.
enum ReviewHelper {
    static let installTimeKey = "review_install_time"
    static let lastPromptTimeKey = "review_last_prompt_time"

    private static let threeDaysSeconds: TimeInterval = 3 * 24 * 60 * 60
    private static let fiveDaysSeconds: TimeInterval = 5 * 24 * 60 * 60
    private static let nineDaysSeconds: TimeInterval = 9 * 24 * 60 * 60

    /// Prüft ob die Bedingungen für den automatischen Review-Prompt erfüllt sind:
    /// - Mindestens 3 Tage seit Erstinstallation für den ersten Prompt.
    /// - Mindestens 9 Tage seit Erstinstallation für den zweiten Prompt (falls der erste vor Tag 9 lag und mindestens 5 Tage vergangen sind).
    /// - Keine Morgensperre zwischen 6:00 und 8:59 Uhr.
    static func shouldShowReview(
        defaults: UserDefaults = .standard,
        now: Date = Date(),
        hour: Int? = nil
    ) -> Bool {
        let nowTimestamp = now.timeIntervalSince1970
        let installTime = defaults.double(forKey: installTimeKey)

        if installTime == 0 {
            defaults.set(nowTimestamp, forKey: installTimeKey)
            return false
        }

        let currentHour = hour ?? Calendar.current.component(.hour, from: now)
        if currentHour >= 6 && currentHour < 9 {
            return false
        }

        let lastPromptTime = defaults.double(forKey: lastPromptTimeKey)
        let timeSinceInstall = nowTimestamp - installTime

        if lastPromptTime == 0 {
            return timeSinceInstall >= threeDaysSeconds
        }

        let firstPromptSinceInstall = lastPromptTime - installTime
        let timeSinceLastPrompt = nowTimestamp - lastPromptTime
        return timeSinceInstall >= nineDaysSeconds &&
            firstPromptSinceInstall < nineDaysSeconds &&
            timeSinceLastPrompt >= fiveDaysSeconds
    }

    /// Zeigt bei erfüllten Bedingungen den systemeigenen StoreKit-Bewertungsdialog an.
    @MainActor
    static func launchReview(defaults: UserDefaults = .standard) {
        guard shouldShowReview(defaults: defaults) else { return }

        guard let scene = UIApplication.shared.connectedScenes
            .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene else {
            return
        }

        Aptabase.shared.trackEvent("in_app_review_prompted")
        SKStoreReviewController.requestReview(in: scene)
        defaults.set(Date().timeIntervalSince1970, forKey: lastPromptTimeKey)
    }
}
