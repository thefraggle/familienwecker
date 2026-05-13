import Foundation
// import RevenueCat  // Wird aktiviert sobald RevenueCat SPM-Package eingebunden ist

/// RevenueCat-Konfiguration – vorbereitet, noch keine Produkte
enum RevenueCatService {
    // TODO: API-Key aus Firebase Remote Config oder Secrets laden
    private static let apiKey = "REVENUECAT_IOS_API_KEY_PLACEHOLDER"

    static func configure() {
        // Purchases.configure(withAPIKey: apiKey)
        // ↑ Aktivieren sobald RevenueCat via SPM eingebunden und API-Key gesetzt ist
        print("[RevenueCat] SDK vorbereitet – noch nicht aktiv (kein API-Key)")
    }
}
