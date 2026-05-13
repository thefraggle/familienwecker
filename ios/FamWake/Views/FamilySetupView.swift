import SwiftUI

struct FamilySetupView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var appState: AppState

    @State private var isCreateMode = true
    @State private var familyName = ""
    @State private var joinCode = ""
    @State private var isLoading = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: appState.colorScheme == .dark
                    ? [Color(.systemBackground), Color(.secondarySystemBackground)]
                    : [Color.accentColor.opacity(0.12), Color(.systemBackground)],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            VStack(spacing: 0) {
                // TopBar
                HStack {
                    Text("FamWake ").font(.headline).bold() +
                    Text(L.appNameShort).font(.headline).fontWeight(.regular)
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

                            Button(L.setupCreateButton) {
                                isLoading = true
                                familyViewModel.createFamily(familyName) { success in
                                    isLoading = false
                                    if success { appState.route = .main }
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .disabled(familyName.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)
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

                            Button(L.setupJoinButton) {
                                isLoading = true
                                familyViewModel.joinFamily(joinCode) { success in
                                    isLoading = false
                                    if success { appState.route = .main }
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .disabled(joinCode.count != 6 || isLoading)
                            .buttonStyle(BounceButtonStyle())
                        }
                    }

                    if isLoading {
                        ProgressView()
                            .padding()
                    }

                    if let error = familyViewModel.errorMessage {
                        Text(error)
                            .foregroundStyle(.red)
                            .font(.footnote)
                            .multilineTextAlignment(.center)
                            .padding(.top, 8)
                    }
                }
                .padding(20)
                .famWakeCard(cornerRadius: 32, isDark: appState.colorScheme == .dark)
                .padding(.horizontal, 24)

                Spacer().frame(height: 32)

                // Logout
                Button(L.settingsLogout) {
                    authViewModel.logout()
                }
                .foregroundStyle(.red)
                .padding(.bottom, 32)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: isCreateMode)
        .onDisappear {
            familyViewModel.clearError()
        }
    }
}
