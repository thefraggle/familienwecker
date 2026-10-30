import SwiftUI

struct SettingsFooterSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showDonationSheet: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "2.1.6"
    }

    var body: some View {
        VStack(spacing: 16) {
            // Version & Copyright Info
            VStack(spacing: 4) {
                Text("FamWake \(L.appNameShort) v\(appVersion)")
                    .font(.footnote)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .multilineTextAlignment(.center)

                Text(L.settingsFooterCopyright)
                    .font(.footnote)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
            .padding(.top, 8)

            // Links: Nutzungsbedingungen · Datenschutzerklärung · Impressum
            HStack(spacing: 16) {
                if let url = URL(string: L.settingsTermsOfUseUrl) {
                    Link(L.settingsTermsOfUse, destination: url)
                        .font(.footnote)
                        .foregroundStyle(theme.primary)
                }

                if let url = URL(string: L.settingsPrivacyPolicyUrl) {
                    Link(L.settingsPrivacyPolicy, destination: url)
                        .font(.footnote)
                        .foregroundStyle(theme.primary)
                }

                if let url = URL(string: L.settingsImprintUrl) {
                    Link(L.settingsImprint, destination: url)
                        .font(.footnote)
                        .foregroundStyle(theme.primary)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.bottom, 28)
    }
}
