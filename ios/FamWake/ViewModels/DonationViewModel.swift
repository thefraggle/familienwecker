import Foundation
import Combine
import RevenueCat
import Aptabase

/// ViewModel für In-App-Spenden via RevenueCat
@MainActor
class DonationViewModel: ObservableObject {
    @Published var offerings: Offerings? = nil
    @Published var purchaseState: PurchaseState = .idle

    func loadOfferings() {
        Aptabase.shared.trackEvent("donation_sheet_opened")
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
        Aptabase.shared.trackEvent("donation_package_selected", with: ["package_id": package.identifier])
        Purchases.shared.purchase(package: package) { [weak self] transaction, customerInfo, error, userCancelled in
            guard let self = self else { return }
            if userCancelled {
                Aptabase.shared.trackEvent("donation_cancelled")
                self.purchaseState = .idle
                return
            }
            if let error = error {
                Aptabase.shared.trackEvent("donation_failed", with: ["error": error.localizedDescription])
                self.purchaseState = .error(error.localizedDescription)
                return
            }
            Aptabase.shared.trackEvent("donation_completed", with: ["package_id": package.identifier])
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
