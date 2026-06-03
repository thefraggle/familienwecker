import SwiftUI

/// Member Card – 1:1 Spiegel von Android MemberCard.kt
/// Layout: Avatar | Name + Alarm-Badge + Weckzeit + Bad-Info | Aktions-Buttons
struct MemberCardView: View {
    let member: FamilyMember
    let isMyProfile: Bool
    let isAlarmEnabled: Bool
    var onEdit: () -> Void
    var onDelete: () -> Void
    var onTogglePause: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    private var isDark: Bool { colorScheme == .dark }

    // Claimed by another user – no tap-to-edit (Android MemberCard.kt:52-55)
    private var isOtherUserClaim: Bool {
        member.claimedByUserId != nil && !isMyProfile
    }

    // Android MemberCard.kt:39-50 – Paused dimming
    private var backgroundColor: Color {
        if member.isPaused {
            return isDark ? theme.surfaceVariant.opacity(0.4) : theme.surfaceVariant
        }
        return isDark ? theme.primaryContainer.opacity(0.4) : theme.primaryContainer
    }

    private var textColor: Color {
        member.isPaused ? theme.onSurfaceVariant : theme.onPrimaryContainer
    }

    var body: some View {
        Button(action: {
            // Ganze Kachel klickbar zum Editieren (Android MemberCard.kt:55)
            if !isOtherUserClaim { onEdit() }
        }) {
            HStack(spacing: 16) {
                // Avatar (Android MemberCard.kt:73-82)


                // Content Column
                VStack(alignment: .leading, spacing: 4) {
                    // Row 1: Name
                    Text(member.name)
                        .font(.body).fontWeight(.bold)
                        .foregroundStyle(textColor)
                        .lineLimit(1)

                    // Row 2: Status & Awake Emoji
                    HStack(spacing: 6) {
                        if member.claimedByUserId != nil {
                            alarmStatusBadge
                        } else if member.isPaused {
                            Text(L.memberStatusPaused)
                                .font(.subheadline).fontWeight(.semibold)
                                .foregroundStyle(textColor.opacity(0.8))
                        }

                        if member.isAwakeToday {
                            Text("☀️").font(.subheadline)
                        }
                    }
                }

                Spacer()

                // Action Buttons (Android MemberCard.kt:161-208)
                HStack(spacing: 8) {
                    // Pause: nur unclaimed non-self (Android MemberCard.kt:164)
                    if member.claimedByUserId == nil && !isMyProfile {
                        Button(action: onTogglePause) {
                            Image(systemName: member.isPaused ? "play.circle.fill" : "pause.circle.fill")
                                .font(.title3)
                                .foregroundStyle(theme.outline)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(member.isPaused ? L.memberStatusActive : L.memberStatusPaused)
                    }

                    // Edit: eigene oder unclaimed zur Visualisierung (ganze Kachel ist klickbar)
                    if member.claimedByUserId == nil || isMyProfile {
                        Button(action: onEdit) {
                            Image(systemName: "pencil.circle.fill")
                                .font(.title3)
                                .foregroundStyle(theme.outline)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(L.addMemberTitleEdit)
                    }

                    // Delete: eigene oder unclaimed (Android MemberCard.kt:200)
                    if member.claimedByUserId == nil || isMyProfile {
                        Button(action: onDelete) {
                            Image(systemName: "trash.circle.fill")
                                .font(.title3)
                                .foregroundStyle(theme.error)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(L.s("delete_member_confirm_title"))
                    }
                }
            }
            .padding(16)
            .background(isDark ? backgroundColor : backgroundColor.opacity(0.8))
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(theme.outline.opacity(0.15), lineWidth: 0.5)
            )
            .shadow(color: .black.opacity(isDark ? 0.2 : 0.06), radius: 12, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(member.name), \(member.isPaused ? L.memberStatusPaused : "")")
    }

    // MARK: - Alarm Status Badge (Android MemberCard.kt:86-114)
    @ViewBuilder
    private var alarmStatusBadge: some View {
        let allDaysInactive = member.dayProfiles?.values.allSatisfy { !$0.isActive } ?? false

        let isOff: Bool = {
            if isMyProfile {
                return !isAlarmEnabled || member.isPaused || allDaysInactive
            } else {
                return member.deviceAlarmEnabled == false || member.isPaused || allDaysInactive
            }
        }()

        let rawText = isOff ? L.s("main_member_alarm_off") : L.s("main_member_alarm_on")
        let cleanText = rawText
            .replacingOccurrences(of: "(", with: "")
            .replacingOccurrences(of: ")", with: "")
            .replacingOccurrences(of: "（", with: "")
            .replacingOccurrences(of: "）", with: "")

        Text(cleanText)
            .font(.subheadline).fontWeight(.semibold)
            .foregroundStyle(isOff ? theme.error : textColor.opacity(0.8))
    }
}
