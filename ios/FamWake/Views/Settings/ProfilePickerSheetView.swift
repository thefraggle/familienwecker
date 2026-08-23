import SwiftUI

struct ProfilePickerSheetView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showProfileConfirmAlert: Bool
    @Binding var pendingClaimMemberId: String?

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        NavigationStack {
            List {
                ForEach(familyViewModel.members) { member in
                    Button(action: {
                        pendingClaimMemberId = member.id
                        showProfileConfirmAlert = true
                    }) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(member.name)
                                    .font(.headline)
                                    .foregroundStyle(theme.onSurface)
                                if member.id == familyViewModel.myMemberId {
                                    Text(L.settingsProfileClaimed)
                                        .font(.caption)
                                        .foregroundStyle(theme.tertiary)
                                } else if member.claimedByUserId != nil {
                                    Text(L.settingsProfileOther)
                                        .font(.caption)
                                        .foregroundStyle(theme.outline)
                                }
                            }
                            Spacer()
                            if member.id == familyViewModel.myMemberId {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(theme.tertiary)
                            }
                        }
                    }
                    .disabled(member.id == familyViewModel.myMemberId)
                }
            }
            .navigationTitle(L.settingsProfilePickerTitle)
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
}
