import Foundation
import Combine
import FirebaseAuth
import GoogleSignIn
import GoogleSignInSwift

// MARK: - Auth State
enum AuthState: Equatable {
    case loading
    case authenticated
    case unauthenticated
    case awaitingEmailVerification(email: String)
    case error(String)
    case passwordResetSuccess

    static func == (lhs: AuthState, rhs: AuthState) -> Bool {
        switch (lhs, rhs) {
        case (.loading, .loading), (.authenticated, .authenticated),
             (.unauthenticated, .unauthenticated), (.passwordResetSuccess, .passwordResetSuccess):
            return true
        case (.awaitingEmailVerification(let a), .awaitingEmailVerification(let b)):
            return a == b
        case (.error(let a), .error(let b)):
            return a == b
        default:
            return false
        }
    }
}

// MARK: - AuthViewModel
@MainActor
class AuthViewModel: ObservableObject {
    @Published var authState: AuthState = .loading
    @Published var currentUserEmail: String? = nil

    private var authStateListener: AuthStateDidChangeListenerHandle?

    var isLoggedIn: Bool {
        if case .authenticated = authState { return true }
        return false
    }

    var isAnonymous: Bool {
        Auth.auth().currentUser?.isAnonymous ?? false
    }

    var currentUserId: String? {
        Auth.auth().currentUser?.uid
    }

    init() {
        listenToAuthState()
    }

    deinit {
        if let listener = authStateListener {
            Auth.auth().removeStateDidChangeListener(listener)
        }
    }

    private func listenToAuthState() {
        authStateListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self else { return }
            Task { @MainActor in
                if let user {
                    self.currentUserEmail = user.email
                    // Anonymous users and OAuth users (Google) are always "verified"
                    if user.isAnonymous || user.isEmailVerified || user.providerData.contains(where: { $0.providerID != "password" }) {
                        self.authState = .authenticated
                        MessagingService.shared.refreshAndSaveToken()
                    } else {
                        self.authState = .awaitingEmailVerification(email: user.email ?? "")
                    }
                } else {
                    self.currentUserEmail = nil
                    self.authState = .unauthenticated
                }
            }
        }
    }

    // MARK: - Anonymous Login (Lazy Registration)
    func signInAnonymously() {
        authState = .loading
        Task {
            do {
                try await Auth.auth().signInAnonymously()
                MessagingService.shared.refreshAndSaveToken()
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    // MARK: - Email/Password
    func login(email: String, password: String) {
        authState = .loading
        Task {
            do {
                try await Auth.auth().signIn(withEmail: email, password: password)
                MessagingService.shared.refreshAndSaveToken()
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func register(email: String, password: String) {
        authState = .loading
        Task {
            do {
                // Lazy Registration: link falls anonym, signIn als Fallback
                if let currentUser = Auth.auth().currentUser, currentUser.isAnonymous {
                    let credential = EmailAuthProvider.credential(withEmail: email, password: password)
                    do {
                        try await currentUser.link(with: credential)
                        try await currentUser.sendEmailVerification()
                    } catch {
                        let code = AuthErrorCode(rawValue: (error as NSError).code)
                        if code == .credentialAlreadyInUse || code == .emailAlreadyInUse {
                            // Account existiert schon → normaler Login
                            try await Auth.auth().signIn(withEmail: email, password: password)
                            MessagingService.shared.refreshAndSaveToken()
                        } else {
                            throw error
                        }
                    }
                } else {
                    let result = try await Auth.auth().createUser(withEmail: email, password: password)
                    try await result.user.sendEmailVerification()
                }
                authState = .awaitingEmailVerification(email: email)
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func logout() {
        MessagingService.shared.deleteTokenOnLogout()
        try? Auth.auth().signOut()
        authState = .unauthenticated
    }

    func resetPassword(email: String) {
        guard !email.isEmpty else { return }
        Task {
            do {
                try await Auth.auth().sendPasswordReset(withEmail: email)
                authState = .passwordResetSuccess
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func checkEmailVerified() {
        Task {
            do {
                try await Auth.auth().currentUser?.reload()
                if Auth.auth().currentUser?.isEmailVerified == true {
                    authState = .authenticated
                } else {
                    authState = .error(L.loginVerifyEmailNotVerified)
                }
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func resendVerificationEmail() {
        Task {
            do {
                try await Auth.auth().currentUser?.sendEmailVerification()
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func applyActionCode(_ oobCode: String) {
        authState = .loading
        Task {
            do {
                try await Auth.auth().applyActionCode(oobCode)
                checkEmailVerified()
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    // MARK: - Google Sign-In
    func signInWithGoogle() {
        authState = .loading
        Task {
            do {
                guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                      let rootViewController = windowScene.windows.first?.rootViewController else {
                    authState = .error(L.errorGeneric)
                    return
                }

                let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
                guard let idToken = result.user.idToken?.tokenString else {
                    authState = .error(L.errorGeneric)
                    return
                }

                let credential = GoogleAuthProvider.credential(
                    withIDToken: idToken,
                    accessToken: result.user.accessToken.tokenString
                )

                // Lazy Registration: link falls anonym, signIn als Fallback
                if let currentUser = Auth.auth().currentUser, currentUser.isAnonymous {
                    do {
                        try await currentUser.link(with: credential)
                    } catch {
                        // Credential bereits vergeben (z.B. auf Android registriert) → direkt anmelden
                        let code = AuthErrorCode(rawValue: (error as NSError).code)
                        if code == .credentialAlreadyInUse || code == .providerAlreadyLinked {
                            try await Auth.auth().signIn(with: credential)
                        } else {
                            throw error
                        }
                    }
                } else {
                    try await Auth.auth().signIn(with: credential)
                }
                MessagingService.shared.refreshAndSaveToken()
                // authState set via listener
            } catch {
                // User cancelled is not an error
                if (error as NSError).code == GIDSignInError.canceled.rawValue {
                    authState = .unauthenticated
                    return
                }
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    // MARK: - Error Mapping (mirrors Android AuthViewModel)
    private func mapFirebaseError(_ error: Error) -> String {
        let code = AuthErrorCode(rawValue: (error as NSError).code)
        switch code {
        case .wrongPassword, .invalidCredential: return L.errorWrongPassword
        case .userNotFound: return L.errorUserNotFound
        case .emailAlreadyInUse: return L.errorEmailInUse
        case .weakPassword: return L.errorWeakPassword
        case .invalidEmail: return L.errorInvalidEmail
        case .networkError: return L.errorNetwork
        default: return error.localizedDescription
        }
    }
}
