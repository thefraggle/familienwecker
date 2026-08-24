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
                    ProfilePickerRowView(
                        member: member,
                        myMemberId: familyViewModel.myMemberId,
                        theme: theme,
                        onSelect: {
                            pendingClaimMemberId = member.id
                            showProfileConfirmAlert = true
                        }
                    )
                }
            }
            .navigationTitle(L.settingsProfileTitle)
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

private struct ProfilePickerRowView: View {
    let member: FamilyMember
    let myMemberId: String?
    let theme: FamWakeTheme
    let onSelect: () -> Void

    private var isMe: Bool {
        member.id == myMemberId
    }

    var body: some View {
        Button(action: onSelect) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(member.name)
                        .font(.headline)
                        .foregroundStyle(theme.onSurface)

                    if !isMe && member.claimedByUserId != nil {
                        Text(L.settingsAlreadyClaimed)
                            .font(.caption)
                            .foregroundStyle(theme.outline)
                    }
                }
                Spacer()
                if isMe {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(theme.tertiary)
                }
            }
        }
        .disabled(isMe)
    }
}
