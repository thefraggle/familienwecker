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
                ZStack {
                    Circle()
                        .fill(isMyProfile ? theme.tertiary : theme.surfaceVariant)
                        .frame(width: 44, height: 44)
                    Text(member.name.prefix(1).uppercased())
                        .font(.headline).fontWeight(.bold)
                        .foregroundStyle(isMyProfile ? theme.onTertiary : theme.onSurfaceVariant)
                }

                // Content Column (Android MemberCard.kt:70-158)
                VStack(alignment: .leading, spacing: 3) {
                    // Row 1: Name + Alarm Badge + Awake Emoji (Android MemberCard.kt:73-118)
                    HStack(spacing: 6) {
                        Text(member.name)
                            .font(.subheadline).fontWeight(.bold)
                            .foregroundStyle(textColor)
                            .lineLimit(1)

                        if member.claimedByUserId != nil {
                            alarmStatusBadge
                        }

                        if member.isAwakeToday {
                            Text("☀️").font(.caption2)
                        }
                    }

                    // Row 2: Next active day label (Android MemberCard.kt:121-147)
                    if let dayInfo = nextActiveDayInfo() {
                        if let dayLabel = dayInfo.dayLabel {
                            Text(dayLabel)
                                .font(.caption2).fontWeight(.bold)
                                .foregroundStyle(textColor.opacity(0.7))
                        }
                        // Wake time range (Android MemberCard.kt:148-153)
                        Text("\(dayInfo.earliest) – \(dayInfo.latest)")
                            .font(.subheadline).fontWeight(.bold)
                            .foregroundStyle(textColor)

                        // Bathroom + Breakfast info (Android MemberCard.kt:154-157)
                        Text(L.s("main_bathroom_info", "\(member.bathroomDurationMinutes)", member.wantsBreakfast ? L.s("yes") : L.s("no")))
                            .font(.caption)
                            .foregroundStyle(textColor.opacity(0.9))
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
                    }

                    // Delete: eigene oder unclaimed (Android MemberCard.kt:200)
                    if member.claimedByUserId == nil || isMyProfile {
                        Button(action: onDelete) {
                            Image(systemName: "trash.circle.fill")
                                .font(.title3)
                                .foregroundStyle(theme.error)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(backgroundColor)
                    .shadow(color: .black.opacity(isDark ? 0 : 0.08), radius: 8, x: 0, y: 4)
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(theme.outline.opacity(0.15), lineWidth: 1)
                    )
            )
        }
        .buttonStyle(.plain)
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

        Text(isOff ? L.s("main_member_alarm_off") : L.s("main_member_alarm_on"))
            .font(.system(size: 10, weight: .bold))
            .foregroundStyle(isOff ? theme.error : textColor.opacity(0.7))
    }

    // MARK: - Next Active Day (Android MemberCard.kt:121-147)
    private struct DayInfo {
        let dayLabel: String?
        let earliest: String
        let latest: String
    }

    private func nextActiveDayInfo() -> DayInfo? {
        let allInactive = member.dayProfiles?.values.allSatisfy { !$0.isActive } ?? true
        if allInactive { return nil }

        let today = Calendar.current.component(.weekday, from: Date())
        let todayDow = today == 1 ? 7 : today - 1 // 1=Mo .. 7=So

        for offset in 0..<7 {
            let checkDow = ((todayDow - 1 + offset) % 7) + 1
            if let profiles = member.dayProfiles, let profile = profiles[checkDow], profile.isActive {
                let dayLabel: String? = offset == 0 ? nil : {
                    let cal = Calendar.current
                    if let futureDate = cal.date(byAdding: .day, value: offset, to: Date()) {
                        return futureDate.formatted(.dateTime.weekday(.wide)).localizedCapitalized
                    }
                    return nil
                }()

                return DayInfo(
                    dayLabel: dayLabel,
                    earliest: profile.earliestWakeUp.formatted(),
                    latest: profile.latestWakeUp.formatted()
                )
            }
        }
        return nil
    }
}
