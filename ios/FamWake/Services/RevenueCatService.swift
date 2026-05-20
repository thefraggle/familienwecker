import Foundation
import RevenueCat

/// RevenueCat-Konfiguration für Spenden
enum RevenueCatService {
    private static let apiKey = "REDACTED_REVENUECAT_KEY"

    static func configure() {
        Purchases.configure(withAPIKey: apiKey)
        print("[RevenueCat] SDK erfolgreich konfiguriert")
    }
}
