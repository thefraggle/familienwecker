import Foundation
import RevenueCat

/// RevenueCat-Konfiguration für Spenden
enum RevenueCatService {
    private static let apiKey = "appl_cHVPunINzmngYBhsXCPcwleBtfs"

    static func configure() {
        Purchases.configure(withAPIKey: apiKey)
        print("[RevenueCat] SDK erfolgreich konfiguriert")
    }
}
