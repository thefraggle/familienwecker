import SwiftUI

struct SettingsFooterSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showDonationSheet: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 16) {
                Link(L.settingsPrivacyPolicy, destination: URL(string: L.settingsPrivacyPolicyUrl)!)
                    .font(.footnote)
                    .foregroundStyle(theme.primary)
                Text("•").foregroundStyle(theme.outline)
                Link(L.settingsImprint, destination: URL(string: L.settingsImprintUrl)!)
                    .font(.footnote)
                    .foregroundStyle(theme.primary)
            }
            .padding(.top, 8)

            Text("FamWake v\(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "2.1.3") (Build \(Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"))")
                .font(.caption2)
                .foregroundStyle(theme.outline)
        }
        .padding(.bottom, 24)
    }
}
