import Foundation
import Combine
import FirebaseAuth
import GoogleSignIn
import GoogleSignInSwift
import TelemetryClient
import FirebaseFunctions
import AuthenticationServices
import CryptoKit
import Security

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
                    
                    let hasPasswordProvider = user.providerData.contains(where: { $0.providerID == "password" })
                    
                    if hasPasswordProvider {
                        if user.isEmailVerified {
                            self.authState = .authenticated
                            MessagingService.shared.refreshAndSaveToken()
                        } else {
                            self.authState = .awaitingEmailVerification(email: user.email ?? "")
                        }
                    } else if user.isAnonymous || user.providerData.contains(where: { $0.providerID != "password" }) {
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
                TelemetryManager.send("auth.loginSuccess", with: ["method": "anonymous"])
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
                TelemetryManager.send("auth.loginSuccess", with: ["method": "email"])
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
                        try await sendVerificationEmailViaFunction(email: email)
                    } catch {
                        let code = AuthErrorCode(rawValue: (error as NSError).code)
                        if code == .credentialAlreadyInUse || code == .emailAlreadyInUse {
                            // Account existiert schon → normaler Login
                            try await Auth.auth().signIn(withEmail: email, password: password)
                            TelemetryManager.send("auth.loginSuccess", with: ["method": "email"])
                            MessagingService.shared.refreshAndSaveToken()
                        } else {
                            throw error
                        }
                    }
                } else {
                    let result = try await Auth.auth().createUser(withEmail: email, password: password)
                    TelemetryManager.send("auth.registerSuccess")
                    try await sendVerificationEmailViaFunction(email: email)
                }
                authState = .awaitingEmailVerification(email: email)
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    func logout() {
        TelemetryManager.send("auth.logout")
        MessagingService.shared.deleteTokenOnLogout()
        try? Auth.auth().signOut()
        authState = .unauthenticated
    }

    func resetPassword(email: String) {
        guard !email.isEmpty else { return }
        Task {
            do {
                let language = UserDefaults.standard.string(forKey: "language") ?? "de"
                let data: [String: Any] = ["email": email.trimmingCharacters(in: .whitespacesAndNewlines), "language": language]
                _ = try await Functions.functions(region: "europe-west3").httpsCallable("sendBrandedResetEmail").call(data)
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
        guard let email = Auth.auth().currentUser?.email else { return }
        Task {
            do {
                try await sendVerificationEmailViaFunction(email: email)
            } catch {
                authState = .error(mapFirebaseError(error))
            }
        }
    }

    private func sendVerificationEmailViaFunction(email: String) async throws {
        let language = UserDefaults.standard.string(forKey: "language") ?? "de"
        let data: [String: Any] = ["email": email.trimmingCharacters(in: .whitespacesAndNewlines), "language": language]
        do {
            _ = try await Functions.functions(region: "europe-west3").httpsCallable("sendVerificationEmail").call(data)
        } catch {
            // Fallback
            try await Auth.auth().currentUser?.sendEmailVerification()
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
                TelemetryManager.send("auth.loginSuccess", with: ["method": "google"])
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

    // MARK: - Apple Sign-In
    private var currentNonce: String?

    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        let errorCode = SecRandomCopyBytes(nil, randomBytes.count, &randomBytes)
        if errorCode != errSecSuccess {
            fatalError("Unable to generate input bytes: \(errorCode)")
        }

        let charset: [Character] =
            Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")

        let nonce = randomBytes.map { byte in
            charset[Int(byte) % charset.count]
        }

        return String(nonce)
    }

    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        let hashString = hashedData.compactMap {
            String(format: "%02x", $0)
        }.joined()
        return hashString
    }

    func prepareAppleSignInRequest(_ request: ASAuthorizationAppleIDRequest) {
        let nonce = randomNonceString()
        currentNonce = nonce
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
    }

    func handleAppleSignInCompletion(_ result: Result<ASAuthorization, Error>) {
        switch result {
        case .success(let authorization):
            if let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential {
                guard let nonce = currentNonce else {
                    self.authState = .error(L.errorGeneric)
                    return
                }
                guard let appleIDToken = appleIDCredential.identityToken else {
                    self.authState = .error(L.errorGeneric)
                    return
                }
                guard let idTokenString = String(data: appleIDToken, encoding: .utf8) else {
                    self.authState = .error(L.errorGeneric)
                    return
                }

                let credential = OAuthProvider.credential(
                    providerID: .apple,
                    idToken: idTokenString,
                    rawNonce: nonce
                )

                authState = .loading
                Task {
                    do {
                        // Lazy Registration: link falls anonym, signIn als Fallback
                        if let currentUser = Auth.auth().currentUser, currentUser.isAnonymous {
                            do {
                                try await currentUser.link(with: credential)
                            } catch {
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
                        TelemetryManager.send("auth.loginSuccess", with: ["method": "apple"])
                        MessagingService.shared.refreshAndSaveToken()
                    } catch {
                        authState = .error(mapFirebaseError(error))
                    }
                }
            } else {
                authState = .error(L.errorGeneric)
            }
        case .failure(let error):
            // User cancelled is not an error
            if (error as NSError).code == ASAuthorizationError.canceled.rawValue {
                authState = .unauthenticated
                return
            }
            authState = .error(error.localizedDescription)
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
