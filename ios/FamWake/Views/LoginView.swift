import SwiftUI
import AuthenticationServices
import CryptoKit

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var appState: AppState

    @State private var email = ""
    @State private var password = ""
    @State private var passwordVisible = false
    @State private var isRegistering = false
    @FocusState private var focusedField: Field?

    enum Field { case email, password }

    var body: some View {
        ZStack {
            famWakeLinearGradient
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // TopBar analog
                HStack {
                    Text("FamWake ").font(.headline).bold() +
                    Text(L.appNameShort).font(.headline).fontWeight(.regular)
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                Spacer()

                // Login Card
                VStack(spacing: 16) {
                    // E-Mail Feld
                    TextField(L.emailLabel, text: $email)
                        .keyboardType(.emailAddress)
                        .textContentType(.emailAddress)
                        .autocapitalization(.none)
                        .focused($focusedField, equals: .email)
                        .submitLabel(.next)
                        .onSubmit { focusedField = .password }
                        .padding(14)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.accentColor.opacity(0.3), lineWidth: 1))

                    // Passwort Feld
                    HStack {
                        Group {
                            if passwordVisible {
                                TextField(L.passwordLabel, text: $password)
                            } else {
                                SecureField(L.passwordLabel, text: $password)
                            }
                        }
                        .textContentType(isRegistering ? .newPassword : .password)
                        .focused($focusedField, equals: .password)
                        .submitLabel(.done)
                        .onSubmit { handleMainAction() }

                        Button(action: { passwordVisible.toggle() }) {
                            Image(systemName: passwordVisible ? "eye.slash.fill" : "eye.fill")
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(14)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.accentColor.opacity(0.3), lineWidth: 1))

                    // Auth State abhängige Inhalte
                    authStateContent

                }
                .padding(20)
                .famWakeCard(cornerRadius: 32, isDark: appState.colorScheme == .dark)
                .padding(.horizontal, 24)

                Spacer()
            }
        }
    }

    // MARK: - Auth State Content
    @ViewBuilder
    private var authStateContent: some View {
        switch authViewModel.authState {
        case .loading:
            ProgressView()
                .padding()

        case .awaitingEmailVerification(let userEmail):
            emailVerificationView(email: userEmail)

        default:
            mainAuthButtons
        }
    }

    @ViewBuilder
    private var mainAuthButtons: some View {
        // Login / Register Button
        Button(action: handleMainAction) {
            Text(isRegistering ? L.registerButton : L.loginButton)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
        }
        .buttonStyle(.borderedProminent)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .disabled(email.isEmpty || password.isEmpty)
        .buttonStyle(BounceButtonStyle())

        // AGB bei Registrierung
        if isRegistering {
            registrationDisclaimer
        }

        // Passwort vergessen
        if !isRegistering {
            Button(L.loginForgotPassword) {
                authViewModel.resetPassword(email: email)
            }
            .font(.footnote)
        }

        Button(isRegistering ? L.alreadyHaveAccount : L.noAccount) {
            withAnimation { isRegistering.toggle() }
        }
        .font(.footnote)

        Divider()

        // Apple Sign-In
        AppleSignInButton()
            .environmentObject(authViewModel)

        // Fehleranzeige
        if case .error(let msg) = authViewModel.authState {
            Text(msg)
                .foregroundStyle(.red)
                .font(.footnote)
                .multilineTextAlignment(.center)
        }

        // Passwort Reset Erfolg
        if case .passwordResetSuccess = authViewModel.authState {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                Text(L.loginPasswordResetSent).font(.footnote)
            }
            .padding(10)
            .background(Color.green.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    @ViewBuilder
    private func emailVerificationView(email: String) -> some View {
        VStack(spacing: 12) {
            Text(L.loginVerifyEmailTitle)
                .font(.headline)
                .foregroundStyle(Color.accentColor)

            Text(NSLocalizedString("login_verify_email_text", comment: "").replacingOccurrences(of: "%s", with: email))
                .font(.subheadline)
                .multilineTextAlignment(.center)

            Button(L.loginVerifyEmailConfirm) {
                authViewModel.checkEmailVerified()
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 16))

            Button(L.loginVerifyEmailResend) {
                authViewModel.resendVerificationEmail()
            }
            .font(.footnote)

            Button(L.cancelButton) {
                authViewModel.logout()
                isRegistering = true
            }
            .font(.footnote)
            .foregroundStyle(.red)
        }
    }

    @ViewBuilder
    private var registrationDisclaimer: some View {
        let terms = L.registrationTermsOfUse
        let privacy = L.registrationPrivacyPolicy
        let termsUrl = L.settingsTermsOfUseUrl
        let privacyUrl = L.settingsPrivacyPolicyUrl

        HStack(spacing: 0) {
            Text(NSLocalizedString("registration_disclaimer_prefix", comment: ""))
                .font(.caption)
                .foregroundStyle(.secondary)
            Link(terms, destination: URL(string: termsUrl) ?? URL(string: "https://")!)
                .font(.caption).fontWeight(.bold)
            Text(" & ")
                .font(.caption).foregroundStyle(.secondary)
            Link(privacy, destination: URL(string: privacyUrl) ?? URL(string: "https://")!)
                .font(.caption).fontWeight(.bold)
        }
        .multilineTextAlignment(.center)
    }

    // MARK: - Gradient
    private var famWakeLinearGradient: some View {
        LinearGradient(
            colors: appState.colorScheme == .dark
                ? [Color(.systemBackground), Color(.secondarySystemBackground)]
                : [Color.accentColor.opacity(0.12), Color(.systemBackground)],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private func handleMainAction() {
        if isRegistering {
            authViewModel.register(email: email, password: password)
        } else {
            authViewModel.login(email: email, password: password)
        }
    }
}

// MARK: - Apple Sign-In Button
struct AppleSignInButton: View {
    @EnvironmentObject var authViewModel: AuthViewModel

    var body: some View {
        SignInWithAppleButton(.signIn) { request in
            let nonce = authViewModel.prepareAppleSignIn()
            request.requestedScopes = [.fullName, .email]
            request.nonce = nonce
        } onCompletion: { result in
            switch result {
            case .success(let auth):
                if let credential = auth.credential as? ASAuthorizationAppleIDCredential {
                    authViewModel.signInWithApple(credential: credential)
                }
            case .failure(let error):
                print("[Apple Sign-In] Fehler: \(error.localizedDescription)")
            }
        }
        .signInWithAppleButtonStyle(.black)
        .frame(height: 50)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
