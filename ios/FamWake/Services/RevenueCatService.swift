import Foundation
import RevenueCat

/// RevenueCat-Konfiguration für Spenden
enum RevenueCatService {
    private(set) static var isConfigured: Bool = false

    private static var apiKey: String? {
        if let key = Bundle.main.infoDictionary?["RevenueCatAPIKey"] as? String,
           !key.isEmpty,
           !key.hasPrefix("$(") {
            return key
        }
        return nil
    }

    static func configure() {
        guard let key = apiKey, !key.isEmpty else {
            isConfigured = false
            print("[RevenueCat] Kein API-Key konfiguriert – RevenueCat deaktiviert.")
            return
        }
        Purchases.configure(withAPIKey: key)
        isConfigured = true
        print("[RevenueCat] SDK erfolgreich konfiguriert")
    }
}
