import Foundation
import Combine
import FirebaseAuth
import AuthenticationServices
import CryptoKit

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
    private var currentNonce: String?

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
                    if user.isEmailVerified || user.providerData.contains(where: { $0.providerID != "password" }) {
                        self.authState = .authenticated
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

    // MARK: - Email/Password & Anonymous Login
    func signInAnonymously() {
        authState = .loading
        Task {
            do {
                try await Auth.auth().signInAnonymously()
                // authState wird via Listener gesetzt
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func login(email: String, password: String) {
        authState = .loading
        Task {
            do {
                try await Auth.auth().signIn(withEmail: email, password: password)
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func register(email: String, password: String) {
        authState = .loading
        Task {
            do {
                let result = try await Auth.auth().createUser(withEmail: email, password: password)
                try await result.user.sendEmailVerification()
                authState = .awaitingEmailVerification(email: email)
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func logout() {
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

    // MARK: - Apple Sign-In
    func signInWithApple(credential: ASAuthorizationAppleIDCredential) {
        authState = .loading
        guard let nonce = currentNonce,
              let tokenData = credential.identityToken,
              let tokenString = String(data: tokenData, encoding: .utf8) else {
            authState = .error(L.errorGeneric)
            return
        }
        let firebaseCredential = OAuthProvider.credential(
            providerID: AuthProviderID.apple,
            idToken: tokenString,
            rawNonce: nonce
        )
        Task {
            do {
                try await Auth.auth().signIn(with: firebaseCredential)
                // authState via Listener
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func prepareAppleSignIn() -> String {
        let nonce = randomNonceString()
        currentNonce = nonce
        return sha256(nonce)
    }

    // MARK: - Helpers
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

    private func randomNonceString(length: Int = 32) -> String {
        var randomBytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        return randomBytes.map { String(format: "%02x", $0) }.joined()
    }

    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashed = SHA256.hash(data: inputData)
        return hashed.compactMap { String(format: "%02x", $0) }.joined()
    }
}
