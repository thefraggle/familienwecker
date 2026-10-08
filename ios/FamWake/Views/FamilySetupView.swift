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
    @State private var detectedClipboardCode: String? = nil

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
                    Button(action: { appState.route = .login }) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(L.s("anonymous_warning_title"))
                                .font(.subheadline).fontWeight(.bold)
                                .foregroundStyle(theme.onErrorContainer)
                            Text(L.s("anonymous_warning_desc"))
                                .font(.caption)
                                .foregroundStyle(theme.onErrorContainer.opacity(0.85))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .background(
                            .regularMaterial,
                            in: RoundedRectangle(cornerRadius: 24, style: .continuous)
                        )
                        .background(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .fill(colorScheme == .dark ? theme.errorContainer.opacity(0.4) : theme.errorContainer.opacity(0.8))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(theme.outline.opacity(0.2), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
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
                                Text(L.offlineFamilyCreated)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .padding(.top, 4)
                            }
                        }
                    } else {
                        // Familie beitreten
                        VStack(spacing: 14) {
                            HStack(spacing: 8) {
                                TextField(L.setupJoinCodeLabel, text: $joinCode)
                                    .textFieldStyle(.roundedBorder)
                                    .accessibilityLabel(L.s("accessibility_join_code_field"))
                                    .textCase(.uppercase)
                                    .textInputAutocapitalization(.characters)
                                    .autocorrectionDisabled()
                                    .onChange(of: joinCode) { _, new in
                                        let filtered = new.filter { $0.isLetter || $0.isNumber }.uppercased()
                                        joinCode = String(filtered.prefix(6))
                                        if joinCode != detectedClipboardCode {
                                            detectedClipboardCode = nil
                                        }
                                    }

                                if #available(iOS 16.0, *) {
                                    PasteButton(payloadType: String.self) { strings in
                                        guard let first = strings.first else { return }
                                        let sanitized = String(first.filter { $0.isLetter || $0.isNumber }.uppercased().prefix(6))
                                        if sanitized.count == 6 {
                                            joinCode = sanitized
                                            detectedClipboardCode = sanitized
                                        }
                                    }
                                    .buttonBorderShape(.roundedRectangle(radius: 8))
                                    .labelStyle(.iconOnly)
                                    .tint(theme.primary)
                                }
                            }

                            if let detected = detectedClipboardCode, joinCode == detected, joinCode.count == 6 {
                                HStack(spacing: 6) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(theme.primary)
                                        .font(.caption)
                                    Text(L.setupClipboardCodeDetected(joinCode))
                                        .font(.caption)
                                        .foregroundColor(theme.primary)
                                        .fontWeight(.semibold)
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(theme.primary.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
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
        .onAppear {
            if UIPasteboard.general.hasStrings {
                if let clip = UIPasteboard.general.string {
                    let sanitized = String(clip.filter { $0.isLetter || $0.isNumber }.uppercased().prefix(6))
                    if sanitized.count == 6 && sanitized.range(of: "^[A-Z0-9]{6}$", options: .regularExpression) != nil {
                        if joinCode.isEmpty {
                            joinCode = sanitized
                            isCreateMode = false
                            detectedClipboardCode = sanitized
                        }
                    }
                }
            }
        }
        .onDisappear {
            familyViewModel.clearError()
        }
    }
}
