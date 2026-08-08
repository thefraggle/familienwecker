import SwiftUI

struct FamilySetupView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme

    @State private var isCreateMode = true
    @State private var familyName = ""
    @State private var joinCode = ""
    @State private var isLoading = false

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: colorScheme == .dark
                    ? [theme.surface, theme.background]
                    : [theme.primaryContainer.opacity(0.5), theme.background],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            VStack(spacing: 0) {
                // TopBar
                HStack {
                    famWakeTitle(L.appNameShort)
                        .foregroundStyle(theme.onSurface)
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                Spacer()

                if authViewModel.isAnonymous {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(L.s("anonymous_warning_title"))
                            .font(.subheadline).fontWeight(.bold)
                            .foregroundStyle(theme.onErrorContainer)
                        Text(L.s("anonymous_warning_desc"))
                            .font(.caption)
                            .foregroundStyle(theme.onErrorContainer.opacity(0.9))
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.errorContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(theme.error.opacity(0.3), lineWidth: 1)
                    )
                    .onTapGesture {
                        appState.route = .login
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 16)
                }

                VStack(spacing: 0) {
                    // Tabs
                    Picker("", selection: $isCreateMode) {
                        Text(L.setupCreateTab).tag(true)
                        Text(L.setupJoinTab).tag(false)
                    }
                    .pickerStyle(.segmented)
                    .accessibilityLabel(L.s("accessibility_create_join_picker"))
                    .padding(.bottom, 24)



                    if isCreateMode {
                        // Familie erstellen
                        VStack(spacing: 16) {
                            TextField(L.setupFamilyName, text: $familyName)
                                .textFieldStyle(.roundedBorder)
                                .accessibilityLabel(L.s("accessibility_family_name_field"))

                            Button(action: {
                                isLoading = true
                                familyViewModel.createFamily(familyName) { success in
                                    isLoading = false
                                    if success { appState.route = .main }
                                }
                            }) {
                                Text(L.setupCreateButton)
                                    .font(.headline)
                                    .foregroundStyle(theme.onPrimary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 56)
                                    .background(theme.primary)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(BounceButtonStyle())
                            .disabled(familyName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                            .opacity((familyName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading) ? 0.5 : 1.0)
                            .accessibilityLabel(L.s("accessibility_create_family"))

                            if familyViewModel.isOffline {
                                Text(L.s("offline_family_created_hint"))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .padding(.top, 4)
                            }
                        }
                    } else {
                        // Familie beitreten
                        VStack(spacing: 16) {
                            TextField(L.setupJoinCodeLabel, text: $joinCode)
                                .textFieldStyle(.roundedBorder)
                                .accessibilityLabel(L.s("accessibility_join_code_field"))
                                .textCase(.uppercase)
                                .textInputAutocapitalization(.characters)
                                .autocorrectionDisabled()
                                .onChange(of: joinCode) { _, new in
                                    let filtered = new.filter { $0.isLetter || $0.isNumber }.uppercased()
                                    joinCode = String(filtered.prefix(6))
                                }

                            Button(action: {
                                isLoading = true
                                familyViewModel.joinFamily(joinCode) { success in
                                    isLoading = false
                                    if success { appState.route = .main }
                                }
                            }) {
                                Text(L.setupJoinButton)
                                    .font(.headline)
                                    .foregroundStyle(theme.onPrimary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 56)
                                    .background(theme.primary)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(BounceButtonStyle())
                            .disabled(joinCode.count != 6 || isLoading)
                            .opacity((joinCode.count != 6 || isLoading) ? 0.5 : 1.0)
                            .accessibilityLabel(L.s("accessibility_join_family"))
                        }
                    }

                    if isLoading {
                        ProgressView()
                            .padding()
                    }

                    if let error = familyViewModel.errorMessage {
                        Text(error)
                            .foregroundStyle(theme.error)
                            .font(.footnote)
                            .multilineTextAlignment(.center)
                            .padding(.top, 8)
                    }
                }
                .padding(20)
                .famWakeCard(cornerRadius: 32, isDark: colorScheme == .dark)
                .padding(.horizontal, 24)

                Spacer().frame(height: 32)

                // Logout
                Button(L.settingsLogout) {
                    authViewModel.logout()
                }
                .foregroundStyle(theme.error)
                .accessibilityLabel(L.s("accessibility_setup_logout"))
                .padding(.bottom, 32)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: isCreateMode)
        .onChange(of: familyViewModel.pendingJoinCode) { _, newCode in
            // Deep-Link Auto-Join – nur ausführen wenn sich pendingJoinCode ändert
            guard let code = newCode, !isLoading else { return }
            isLoading = true
            familyViewModel.joinFamily(code) { success in
                isLoading = false
                familyViewModel.clearPendingJoinCode()
                if success { appState.route = .main }
            }
        }
        .onDisappear {
            familyViewModel.clearError()
        }
    }
}
