import Foundation
import Combine

/// Platzhalter für DonationViewModel – RevenueCat noch nicht aktiv
@MainActor
class DonationViewModel: ObservableObject {
    @Published var offerings: [String: Any] = [:]  // Wird zu Purchases.Offerings wenn SDK aktiv
    @Published var purchaseState: PurchaseState = .idle

    func loadOfferings() {
        // TODO: Purchases.shared.getOfferings { ... }
        print("[RevenueCat] Offerings laden – SDK noch nicht aktiv")
    }

    func purchase(packageId _: String) {
        // TODO: Purchases.shared.purchase(package:) { ... }
        print("[RevenueCat] Purchase – SDK noch nicht aktiv")
    }

    func resetState() {
        purchaseState = .idle
    }
}

enum PurchaseState {
    case idle
    case loading
    case success
    case error(String)
}
