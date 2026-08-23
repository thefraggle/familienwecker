import SwiftUI

struct SettingsFooterSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showDonationSheet: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 16) {
                Link(L.s("settings_privacy"), destination: URL(string: L.settingsPrivacyPolicyUrl)!)
                    .font(.footnote)
                    .foregroundStyle(theme.primary)
                Text("•").foregroundStyle(theme.outline)
                Link(L.s("settings_imprint"), destination: URL(string: "https://familien-wecker.de/impressum")!)
                    .font(.footnote)
                    .foregroundStyle(theme.primary)
            }
            .padding(.top, 8)

            Text("FamWake v\(familyViewModel.appVersion) (Build \(familyViewModel.buildNumber))")
                .font(.caption2)
                .foregroundStyle(theme.outline)
        }
        .padding(.bottom, 24)
    }
}
