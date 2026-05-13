import SwiftUI

struct MemberCardView: View {
    let member: FamilyMember
    let isMyProfile: Bool
    var onEdit: () -> Void
    var onDelete: () -> Void
    var onTogglePause: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        HStack(spacing: 12) {
            // Avatar Circle – matches Android MemberCard colors
            ZStack {
                Circle()
                    .fill(isMyProfile ? theme.tertiary : theme.surfaceVariant)
                    .frame(width: 44, height: 44)
                Text(member.name.prefix(1).uppercased())
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(isMyProfile ? theme.onTertiary : theme.onSurfaceVariant)
            }

            // Info
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(member.name)
                        .font(.subheadline).fontWeight(.semibold)
                        .foregroundStyle(theme.onPrimaryContainer)
                    if isMyProfile {
                        Text("• Du")
                            .font(.caption)
                            .foregroundStyle(theme.tertiary)
                    }
                    if member.isPaused {
                        Image(systemName: "pause.circle.fill")
                            .foregroundStyle(theme.tertiary)
                            .font(.caption)
                    }
                }
                if let next = nextActiveDay() {
                    Text(next)
                        .font(.caption)
                        .foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                }
            }

            Spacer()

            // Actions
            Menu {
                Button(action: onEdit) {
                    Label(L.addMemberTitleEdit, systemImage: "pencil")
                }
                Button(action: onTogglePause) {
                    Label(member.isPaused ? L.s("member_resume") : L.s("member_pause"),
                          systemImage: member.isPaused ? "play.circle" : "pause.circle")
                }
                Divider()
                Button(role: .destructive, action: onDelete) {
                    Label(L.settingsDeleteMemberConfirm, systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle.fill")
                    .font(.title3)
                    .foregroundStyle(theme.outline)
            }
        }
        .padding()
        .famWakeCard(cornerRadius: 20, isDark: colorScheme == .dark)
    }

    private func nextActiveDay() -> String? {
        let today = Calendar.current.component(.weekday, from: Date())
        let dow = today == 1 ? 7 : today - 1
        guard let profiles = member.dayProfiles,
              let profile = profiles[dow], profile.isActive else {
            return L.s("member_not_active_today")
        }
        return "\(profile.earliestWakeUp.formatted()) – \(profile.latestWakeUp.formatted())"
    }
}
