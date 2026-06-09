import SwiftUI
import TelemetryClient

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
                            TelemetryManager.send("onboarding.completed_anonymously")
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
                            TelemetryManager.send("onboarding.completed_anonymously")
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
        .onReceive(NotificationCenter.default.publisher(for: .showRingingView)) { notif in
            if let info = notif.userInfo,
               let memberId = info["memberId"] as? String,
               let memberName = info["memberName"] as? String {
                appState.startRinging(memberId: memberId, memberName: memberName)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .showGreetingView)) { notif in
            if let info = notif.userInfo,
               let memberId = info["memberId"] as? String,
               let memberName = info["memberName"] as? String {
                appState.startGreeting(memberId: memberId, memberName: memberName)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .stopAlarmFromNotification)) { notif in
            appState.stopRinging()
            if let info = notif.userInfo, let memberId = info["memberId"] as? String {
                familyViewModel.cancelSnooze(memberId)
                familyViewModel.recalculateSchedule()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .snoozeAlarmFromNotification)) { notif in
            appState.stopRinging()
            if let info = notif.userInfo,
               let memberId = info["memberId"] as? String,
               let memberName = info["memberName"] as? String {
                familyViewModel.snooze(memberId: memberId, memberName: memberName)
            }
        }
        .fullScreenCover(isPresented: $appState.isRinging) {
            RingingView(
                memberId: appState.ringingMemberId,
                memberName: appState.ringingMemberName,
                isGreetingOnly: appState.isGreeting,
                onStop: {
                    appState.stopRinging()
                    familyViewModel.cancelSnooze(appState.ringingMemberId)
                    familyViewModel.recalculateSchedule()
                },
                onSnooze: {
                    appState.stopRinging()
                    familyViewModel.snooze(memberId: appState.ringingMemberId, memberName: appState.ringingMemberName)
                }
            )
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
            let currentUid = authViewModel.currentUserId
            let lastUid = UserDefaults.standard.string(forKey: "last_logged_in_uid")
            
            // Wenn sich der Benutzer geändert hat (z.B. nach Logout/Login oder Fallback-Login bei Google-Auth)
            if let last = lastUid, let curr = currentUid, last != curr {
                familyViewModel.reloadForNewUser()
            }
            if let curr = currentUid {
                UserDefaults.standard.set(curr, forKey: "last_logged_in_uid")
            }
            
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
