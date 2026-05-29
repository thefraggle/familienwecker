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

                VStack(spacing: 0) {
                    // Tabs
                    Picker("", selection: $isCreateMode) {
                        Text(L.setupCreateTab).tag(true)
                        Text(L.setupJoinTab).tag(false)
                    }
                    .pickerStyle(.segmented)
                    .padding(.bottom, 24)

                    // Deep-Link Auto-Join
                    let _ = familyViewModel.pendingJoinCode.map { code in
                        if !isLoading {
                            isLoading = true
                            familyViewModel.joinFamily(code) { success in
                                isLoading = false
                                familyViewModel.clearPendingJoinCode()
                                if success { appState.route = .main }
                            }
                        }
                    }

                    if isCreateMode {
                        // Familie erstellen
                        VStack(spacing: 16) {
                            TextField(L.setupFamilyName, text: $familyName)
                                .textFieldStyle(.roundedBorder)

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
                        }
                    } else {
                        // Familie beitreten
                        VStack(spacing: 16) {
                            TextField(L.setupJoinCodeLabel, text: $joinCode)
                                .textFieldStyle(.roundedBorder)
                                .textCase(.uppercase)
                                .autocapitalization(.allCharacters)
                                .disableAutocorrection(true)
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
                .padding(.bottom, 32)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: isCreateMode)
        .onDisappear {
            familyViewModel.clearError()
        }
    }
}
