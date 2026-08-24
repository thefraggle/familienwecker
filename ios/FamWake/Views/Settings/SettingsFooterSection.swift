import SwiftUI

struct SettingsFooterSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showDonationSheet: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "2.1.4"
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
                Link(L.settingsTermsOfUse, destination: URL(string: L.settingsTermsOfUseUrl)!)
                    .font(.footnote)
                    .foregroundStyle(theme.primary)

                Link(L.settingsPrivacyPolicy, destination: URL(string: L.settingsPrivacyPolicyUrl)!)
                    .font(.footnote)
                    .foregroundStyle(theme.primary)

                Link(L.settingsImprint, destination: URL(string: L.settingsImprintUrl)!)
                    .font(.footnote)
                    .foregroundStyle(theme.primary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.bottom, 28)
    }
}
