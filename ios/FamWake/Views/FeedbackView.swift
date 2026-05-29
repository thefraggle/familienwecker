import SwiftUI

struct FeedbackView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    @State private var selectedCategory = 0
    @State private var message = ""
    @State private var email = ""

    let categories = [
        L.feedbackCategoryBug,
        L.feedbackCategoryFeature,
        L.feedbackCategoryPraise,
        L.feedbackCategoryOther
    ]

    var isEmailValid: Bool {
        email.isEmpty || email.contains("@") && email.contains(".")
    }

    var canSend: Bool {
        !message.trimmingCharacters(in: .whitespaces).isEmpty && isEmailValid && !familyViewModel.isSendingFeedback
    }

    var deviceModel: String {
        UIDevice.current.model + " \(ProcessInfo.processInfo.operatingSystemVersionString)"
    }

    var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if familyViewModel.feedbackSubmitted {
                        // Erfolg
                        VStack(spacing: 12) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 56))
                                .foregroundStyle(.green)
                            Text(L.feedbackSuccessTitle).font(.headline)
                            Text(L.feedbackSuccessMessage)
                                .font(.subheadline).foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .task {
                            try? await Task.sleep(nanoseconds: 2_500_000_000)
                            familyViewModel.resetFeedbackState()
                            dismiss()
                        }
                    } else {
                        // Fehler
                        if let err = familyViewModel.feedbackError {
                            Text(err)
                                .foregroundStyle(.red)
                                .font(.footnote)
                                .padding()
                                .background(Color.red.opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }

                        Text(L.feedbackIntro)
                            .font(.subheadline).foregroundStyle(.secondary)

                        // Kategorie
                        VStack(alignment: .leading, spacing: 4) {
                            Text(L.feedbackCategoryLabel).font(.caption).foregroundStyle(.secondary)
                            Picker("", selection: $selectedCategory) {
                                ForEach(categories.indices, id: \.self) { i in
                                    Text(categories[i]).tag(i)
                                }
                            }
                            .pickerStyle(.menu)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }

                        // Nachricht
                        VStack(alignment: .leading, spacing: 4) {
                            Text(L.feedbackMessageLabel).font(.caption).foregroundStyle(.secondary)
                            TextEditor(text: $message)
                                .frame(minHeight: 120)
                                .padding(8)
                                .background(Color(.secondarySystemBackground))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    Group {
                                        if message.isEmpty {
                                            Text(L.feedbackMessagePlaceholder)
                                                .foregroundStyle(.placeholder)
                                                .padding(16)
                                                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                                                .allowsHitTesting(false)
                                        }
                                    }
                                )
                        }

                        // E-Mail
                        VStack(alignment: .leading, spacing: 4) {
                            Text(L.feedbackEmailLabel).font(.caption).foregroundStyle(.secondary)
                            TextField(L.feedbackEmailPlaceholder, text: $email)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .textFieldStyle(.roundedBorder)
                            if !isEmailValid {
                                Text(L.errorInvalidEmail).font(.caption).foregroundStyle(.red)
                            }
                        }

                        // Hintergrundinfos
                        VStack(alignment: .leading, spacing: 4) {
                            Text(L.feedbackAutoInfoTitle).font(.caption2).foregroundStyle(.secondary)
                            Text(L.feedbackAutoVersion(appVersion)).font(.caption).foregroundStyle(.secondary)
                            Text(L.feedbackAutoDevice(deviceModel)).font(.caption).foregroundStyle(.secondary)
                        }
                        .padding(10)
                        .background(Color(.tertiarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                        // Buttons
                        HStack(spacing: 12) {
                            Button(action: { dismiss() }) {
                                Text(L.feedbackCancel)
                                    .font(.headline)
                                    .foregroundStyle(theme.primary)
                                    .frame(maxWidth: .infinity)
                                    .frame(minHeight: 56)
                                    .background(theme.primary.opacity(0.1))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(BounceButtonStyle())

                            Button(action: sendFeedback) {
                                Group {
                                    if familyViewModel.isSendingFeedback {
                                        ProgressView().tint(theme.onPrimary)
                                    } else {
                                        Text(L.feedbackSend)
                                            .font(.headline)
                                    }
                                }
                                .foregroundStyle(canSend ? theme.onPrimary : theme.onSurface.opacity(0.38))
                                .frame(maxWidth: .infinity)
                                .frame(minHeight: 56)
                                .background(canSend ? theme.primary : theme.onSurface.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(BounceButtonStyle())
                            .disabled(!canSend)
                        }
                        .frame(minHeight: 56)
                    }
                }
                .padding(20)
            }
            .navigationTitle(L.feedbackTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .fontWeight(.semibold)
                    }
                    .buttonStyle(.borderless)
                    .foregroundStyle(theme.primary)
                }
            }
        }
    }

    private func sendFeedback() {
        familyViewModel.sendFeedback(
            category: categories[selectedCategory],
            message: message,
            email: email,
            appVersion: appVersion,
            device: deviceModel
        )
    }
}
