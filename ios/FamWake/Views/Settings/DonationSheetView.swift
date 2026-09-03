import SwiftUI
import RevenueCat

struct DonationSheetView: View {
    @EnvironmentObject var donationViewModel: DonationViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showDonationSheet: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    private var isDark: Bool { colorScheme == .dark }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                switch donationViewModel.purchaseState {
                case .loading:
                    ProgressView()
                        .scaleEffect(1.5)
                        .padding()
                    Text(L.s("settings_donate_purchase_loading"))
                        .font(.headline)
                        .foregroundStyle(theme.onSurface)

                case .success:
                    Image(systemName: "heart.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(theme.tertiary)
                    Text(L.s("settings_donate_success"))
                        .font(.title3).fontWeight(.bold)
                        .foregroundStyle(theme.onSurface)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: {
                        donationViewModel.resetState()
                        showDonationSheet = false
                    }) {
                        Text(L.okButton)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(theme.primary)
                            .foregroundStyle(theme.onPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.horizontal, 32)
                    .padding(.top, 16)

                case .error(let message):
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 50))
                        .foregroundStyle(theme.error)
                    Text(L.s("settings_donate_error_generic"))
                        .font(.headline)
                        .foregroundStyle(theme.onSurface)
                    Text(message)
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: {
                        donationViewModel.resetState()
                    }) {
                        Text(L.s("error_profile_claim_retry") != "error_profile_claim_retry" ? L.s("error_profile_claim_retry") : L.s("back_desc"))
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(theme.primary)
                            .foregroundStyle(theme.onPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.horizontal, 32)
                    .padding(.top, 16)

                case .idle:
                    Image(systemName: "heart.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(theme.tertiary)

                    Text(L.settingsSupportTitle)
                        .font(.title2).fontWeight(.bold)
                        .foregroundStyle(theme.onSurface)

                    Text(L.s("settings_support_desc"))
                        .font(.body)
                        .foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)

                    if let offerings = donationViewModel.offerings {
                        if let currentOffering = offerings.current, !currentOffering.availablePackages.isEmpty {
                            VStack(spacing: 12) {
                                ForEach(currentOffering.availablePackages, id: \.identifier) { pkg in
                                    Button(action: {
                                        donationViewModel.purchase(package: pkg)
                                    }) {
                                        donationOption(
                                            emoji: getEmoji(for: pkg.identifier),
                                            title: pkg.storeProduct.localizedTitle,
                                            price: pkg.storeProduct.localizedPriceString
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.horizontal)
                        } else {
                            Text(L.s("settings_donate_no_offers"))
                                .font(.body)
                                .foregroundStyle(theme.error)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                        }
                    } else {
                        ProgressView()
                        Text(L.s("settings_donate_loading"))
                            .font(.caption)
                            .foregroundStyle(theme.outline)
                    }
                }

                Spacer()
            }
            .navigationTitle(L.settingsSupportTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: {
                        donationViewModel.resetState()
                        showDonationSheet = false
                    }) {
                        Image(systemName: "xmark")
                            .fontWeight(.semibold)
                    }
                    .buttonStyle(.borderless)
                    .foregroundStyle(theme.primary)
                }
            }
            .onAppear {
                donationViewModel.loadOfferings()
            }
        }
    }

    @ViewBuilder
    private func donationOption(emoji: String, title: String, price: String) -> some View {
        HStack {
            Text(emoji).font(.title2)
            Text(title).font(.body).fontWeight(.medium)
            Spacer()
            Text(price).font(.subheadline).foregroundStyle(theme.outline)
        }
        .frame(minHeight: 56)
        .padding(.horizontal, 16)
        .background(
            isDark ? theme.primaryContainer : theme.surfaceVariant.opacity(0.3)
        )
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(theme.outline.opacity(0.15), lineWidth: 0.5)
        )
    }

    private func getEmoji(for packageIdentifier: String) -> String {
        let id = packageIdentifier.lowercased()
        if id.contains("small") || id.contains("coffee") {
            return "☕"
        } else if id.contains("medium") || id.contains("pizza") {
            return "🍕"
        } else if id.contains("big") || id.contains("party") || id.contains("mega") {
            return "🎉"
        } else {
            return "❤️"
        }
    }
}
