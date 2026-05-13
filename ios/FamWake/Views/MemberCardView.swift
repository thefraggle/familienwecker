import SwiftUI

struct MemberCardView: View {
    let member: FamilyMember
    let isMyProfile: Bool
    var onEdit: () -> Void
    var onDelete: () -> Void
    var onTogglePause: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            // Avatar Circle
            ZStack {
                Circle()
                    .fill(isMyProfile ? Color.accentColor : Color(.systemGray5))
                    .frame(width: 44, height: 44)
                Text(member.name.prefix(1).uppercased())
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(isMyProfile ? .white : Color(.label))
            }

            // Info
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(member.name)
                        .font(.subheadline).fontWeight(.semibold)
                    if isMyProfile {
                        Text("• Du")
                            .font(.caption)
                            .foregroundStyle(Color.accentColor)
                    }
                    if member.isPaused {
                        Image(systemName: "pause.circle.fill")
                            .foregroundStyle(.orange)
                            .font(.caption)
                    }
                }
                if let next = nextActiveDay() {
                    Text(next)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            // Actions
            Menu {
                Button(action: onEdit) {
                    Label("Bearbeiten", systemImage: "pencil")
                }
                Button(action: onTogglePause) {
                    Label(member.isPaused ? "Fortsetzen" : "Pausieren",
                          systemImage: member.isPaused ? "play.circle" : "pause.circle")
                }
                Divider()
                Button(role: .destructive, action: onDelete) {
                    Label("Löschen", systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle.fill")
                    .font(.title3)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .famWakeCard(cornerRadius: 20)
    }

    private func nextActiveDay() -> String? {
        let today = Calendar.current.component(.weekday, from: Date())
        let dow = today == 1 ? 7 : today - 1
        guard let profile = member.dayProfiles[dow], profile.isActive else {
            return "Heute nicht aktiv"
        }
        return "\(profile.earliestWakeUp.formatted()) – \(profile.latestWakeUp.formatted())"
    }
}
