import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct FamWakeApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var familyViewModel = FamilyViewModel()
    @StateObject private var donationViewModel = DonationViewModel()

    init() {
        FirebaseApp.configure()
        if let clientID = FirebaseApp.app()?.options.clientID {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        }
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
                // Google Sign-In URL handler for OAuth callback
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
