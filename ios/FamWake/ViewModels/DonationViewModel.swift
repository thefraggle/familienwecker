import Foundation
import Combine
import RevenueCat

/// ViewModel für In-App-Spenden via RevenueCat
@MainActor
class DonationViewModel: ObservableObject {
    @Published var offerings: Offerings? = nil
    @Published var purchaseState: PurchaseState = .idle

    func loadOfferings() {
        Purchases.shared.getOfferings { [weak self] offerings, error in
            guard let self = self else { return }
            if let error = error {
                print("[RevenueCat] Fehler beim Laden der Offerings: \(error.localizedDescription)")
            }
            self.offerings = offerings
        }
    }

    func purchase(package: Package) {
        purchaseState = .loading
        Purchases.shared.purchase(package: package) { [weak self] transaction, customerInfo, error, userCancelled in
            guard let self = self else { return }
            if userCancelled {
                self.purchaseState = .idle
                return
            }
            if let error = error {
                self.purchaseState = .error(error.localizedDescription)
                return
            }
            self.purchaseState = .success
        }
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
