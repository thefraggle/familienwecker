import SwiftUI
import GoogleSignIn
import GoogleSignInSwift

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

    private var theme: FamWakeTheme { FamWakeTheme.current(for: appState.colorScheme) }

    var body: some View {
        ZStack {
            // Background gradient matching Android LoginScreen
            LinearGradient(
                colors: appState.colorScheme == .dark
                    ? [theme.surface, theme.background]
                    : [theme.primaryContainer.opacity(0.5), theme.background],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // TopBar – "FamWake Familienwecker" Header
                HStack {
                    (Text("FamWake ").font(.headline).bold() +
                     Text(L.appNameShort).font(.headline).fontWeight(.regular))
                        .foregroundStyle(theme.onSurface)
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                Spacer()

                // Login Card
                VStack(spacing: 16) {
                    // E-Mail Field
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
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(theme.outline.opacity(0.2), lineWidth: 1)
                        )

                    // Password Field
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
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(theme.outline.opacity(0.2), lineWidth: 1)
                    )

                    // Auth State Content
                    authStateContent
                }
                .padding(20)
                .background(
                    RoundedRectangle(cornerRadius: 32)
                        .fill(appState.colorScheme == .dark
                            ? theme.primaryContainer
                            : theme.surface)
                        .shadow(color: .black.opacity(appState.colorScheme == .dark ? 0 : 0.08), radius: 12, x: 0, y: 4)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 32)
                        .stroke(theme.outline.opacity(0.15), lineWidth: 1)
                )
                .padding(.horizontal, 24)

                Spacer()
            }
        }
        .onTapGesture { focusedField = nil }
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
        // Login / Register Button (matches Android M3 Button)
        Button(action: handleMainAction) {
            Text(isRegistering ? L.registerButton : L.loginButton)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
        }
        .buttonStyle(.borderedProminent)
        .tint(theme.primary)
        .clipShape(Capsule())
        .disabled(email.isEmpty || password.isEmpty)

        // Registration disclaimer with legal links
        if isRegistering {
            registrationDisclaimer
        }

        // Forgot password (only when logging in)
        if !isRegistering {
            Button(L.loginForgotPassword) {
                authViewModel.resetPassword(email: email)
            }
            .font(.footnote)
            .foregroundStyle(theme.primary)
        }

        // Toggle Login / Register
        Button(action: { withAnimation { isRegistering.toggle() } }) {
            Text(isRegistering ? L.alreadyHaveAccount : L.noAccount)
                .font(.subheadline)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
        }
        .buttonStyle(.bordered)
        .tint(theme.primary)
        .clipShape(Capsule())

        Spacer().frame(height: 8)

        // Google Sign-In (matches Android LoginScreen Google button)
        Button(action: { authViewModel.signInWithGoogle() }) {
            HStack(spacing: 8) {
                Image("ic_google")
                    .resizable()
                    .frame(width: 18, height: 18)
                Text(L.loginWithGoogle)
                    .font(.subheadline)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
        }
        .buttonStyle(.bordered)
        .tint(theme.onSurface)
        .clipShape(Capsule())

        // Error display
        if case .error(let msg) = authViewModel.authState {
            Text(msg)
                .foregroundStyle(theme.error)
                .font(.footnote)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
        }

        // Password Reset Success
        if case .passwordResetSuccess = authViewModel.authState {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill").foregroundStyle(theme.secondary)
                Text(L.loginPasswordResetSent).font(.footnote)
            }
            .padding(10)
            .background(theme.secondaryContainer.opacity(0.3))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    @ViewBuilder
    private func emailVerificationView(email: String) -> some View {
        VStack(spacing: 12) {
            Text(L.loginVerifyEmailTitle)
                .font(.headline)
                .foregroundStyle(theme.primary)

            Text(L.loginVerifyEmailText.replacingOccurrences(of: "%s", with: email))
                .font(.subheadline)
                .multilineTextAlignment(.center)

            Button(L.loginVerifyEmailConfirm) {
                authViewModel.checkEmailVerified()
            }
            .buttonStyle(.borderedProminent)
            .tint(theme.primary)
            .frame(maxWidth: .infinity)
            .clipShape(Capsule())

            Button(L.loginVerifyEmailResend) {
                authViewModel.resendVerificationEmail()
            }
            .font(.footnote)
            .foregroundStyle(theme.primary)

            Button(L.cancelButton) {
                authViewModel.logout()
                isRegistering = true
            }
            .font(.footnote)
            .foregroundStyle(theme.error)
        }
    }

    @ViewBuilder
    private var registrationDisclaimer: some View {
        let terms = L.registrationTermsOfUse
        let privacy = L.registrationPrivacyPolicy
        let termsUrl = L.settingsTermsOfUseUrl
        let privacyUrl = L.settingsPrivacyPolicyUrl

        VStack(spacing: 2) {
            Text(L.registrationDisclaimerPrefix)
                .font(.caption)
                .foregroundStyle(.secondary)
            HStack(spacing: 4) {
                Link(terms, destination: URL(string: termsUrl) ?? URL(string: "https://")!)
                    .font(.caption).fontWeight(.bold).foregroundStyle(theme.primary)
                Text("&")
                    .font(.caption).foregroundStyle(.secondary)
                Link(privacy, destination: URL(string: privacyUrl) ?? URL(string: "https://")!)
                    .font(.caption).fontWeight(.bold).foregroundStyle(theme.primary)
            }
        }
        .multilineTextAlignment(.center)
    }

    private func handleMainAction() {
        if isRegistering {
            authViewModel.register(email: email, password: password)
        } else {
            authViewModel.login(email: email, password: password)
        }
    }
}
