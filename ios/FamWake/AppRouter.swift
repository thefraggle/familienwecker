import SwiftUI

/// Zentrales Navigation-Management – analog zu MainActivity/Routes.kt
struct AppRouter: View {
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var familyViewModel: FamilyViewModel

    var body: some View {
        Group {
            switch appState.route {
            case .loading:
                LoadingView()
            case .onboarding:
                OnboardingView(onFinished: {
                    appState.markOnboardingDone()
                    if !authViewModel.isLoggedIn {
                        authViewModel.signInAnonymously()
                    } else if familyViewModel.hasFamilyId {
                        appState.route = .main
                    } else {
                        appState.route = .familySetup
                    }
                })
            case .login:
                LoginView()
            case .familySetup:
                FamilySetupView()
            case .main:
                MainView()
            }
        }
        .onReceive(authViewModel.$authState) { state in
            handleAuthState(state)
        }
        .task {
            await appState.load(authViewModel: authViewModel, familyViewModel: familyViewModel)
        }
        // Unsichtbare View die languageId beobachtet → zwingt SwiftUI zum Neurendern aller L.xxx
        .background(
            Text("").hidden().id(appState.languageId)
        )
    }

    private func handleAuthState(_ state: AuthState) {
        switch state {
        case .authenticated:
            if appState.route == .login || appState.route == .loading || appState.route == .onboarding {
                appState.route = familyViewModel.hasFamilyId ? .main : .familySetup
            }
        case .unauthenticated:
            appState.route = .login
        case .loading, .awaitingEmailVerification, .error, .passwordResetSuccess:
            break
        }
    }
}

// MARK: - Routes
enum AppRoute {
    case loading
    case onboarding
    case login
    case familySetup
    case main
}
