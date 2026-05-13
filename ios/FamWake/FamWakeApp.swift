import SwiftUI
import FirebaseCore

@main
struct FamWakeApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var familyViewModel = FamilyViewModel()
    @StateObject private var donationViewModel = DonationViewModel()

    init() {
        FirebaseApp.configure()
        RevenueCatService.configure()
    }

    var body: some Scene {
        WindowGroup {
            AppRouter()
                .environmentObject(appState)
                .environmentObject(authViewModel)
                .environmentObject(familyViewModel)
                .environmentObject(donationViewModel)
                .preferredColorScheme(appState.colorScheme)
        }
    }
}
