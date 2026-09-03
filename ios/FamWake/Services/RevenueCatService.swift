import Foundation
import RevenueCat

/// RevenueCat-Konfiguration für Spenden
enum RevenueCatService {
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
            print("[RevenueCat] Kein API-Key konfiguriert – RevenueCat deaktiviert.")
            return
        }
        Purchases.configure(withAPIKey: key)
        print("[RevenueCat] SDK erfolgreich konfiguriert")
    }
}
