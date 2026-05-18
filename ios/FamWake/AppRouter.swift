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
                OnboardingView(
                    startAtWelcome: false,
                    onFinished: { tooltipsEnabled in
                        familyViewModel.setTooltipsEnabled(tooltipsEnabled)
                        appState.markOnboardingDone()
                        if !authViewModel.isLoggedIn {
                            authViewModel.signInAnonymously()
                        } else if familyViewModel.hasFamilyId {
                            appState.route = .main
                        } else {
                            appState.route = .familySetup
                        }
                    },
                    onLoginRequested: {
                        appState.markOnboardingDone()
                        appState.route = .login
                    },
                    isLoggedIn: authViewModel.isLoggedIn
                )
            case .onboardingWelcome:
                OnboardingView(
                    startAtWelcome: true,
                    onFinished: { tooltipsEnabled in
                        familyViewModel.setTooltipsEnabled(tooltipsEnabled)
                        appState.markOnboardingDone()
                        if !authViewModel.isLoggedIn {
                            authViewModel.signInAnonymously()
                        } else if familyViewModel.hasFamilyId {
                            appState.route = .main
                        } else {
                            appState.route = .familySetup
                        }
                    },
                    onLoginRequested: {
                        appState.markOnboardingDone()
                        appState.route = .login
                    },
                    isLoggedIn: authViewModel.isLoggedIn
                )
            case .login:
                LoginView()
            case .familySetup:
                FamilySetupView()
            case .main:
                MainView()
            }
        }
        .onReceive(authViewModel.$authState) { state in
            Task { @MainActor in
                await handleAuthState(state)
            }
        }
        .task {
            await appState.load(authViewModel: authViewModel, familyViewModel: familyViewModel)
        }
        // Unsichtbare View die languageId beobachtet → zwingt SwiftUI zum Neurendern aller L.xxx
        .background(
            Text("").hidden().id(appState.languageId)
        )
    }

    private func handleAuthState(_ state: AuthState) async {
        switch state {
        case .authenticated:
            if appState.route == .login || appState.route == .loading || appState.route == .onboarding || appState.route == .onboardingWelcome {
                if !familyViewModel.hasFamilyId {
                    appState.route = .loading
                    await familyViewModel.restoreUserContextIfNeeded()
                }
                appState.route = familyViewModel.hasFamilyId ? .main : .familySetup
            }
        case .unauthenticated:
            // Nach Logout → zurück zum Onboarding-Welcome (wie Android)
            appState.route = .onboardingWelcome
        case .loading, .awaitingEmailVerification, .error, .passwordResetSuccess:
            break
        }
    }
}

// MARK: - Routes
enum AppRoute {
    case loading
    case onboarding
    case onboardingWelcome
    case login
    case familySetup
    case main
}
