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


                // Content Column (Android MemberCard.kt:70-158)
                VStack(alignment: .leading, spacing: 3) {
                    let dayInfo = nextActiveDayInfo()

                    // Row 1: Name + Alarm Badge + Awake Emoji (Android MemberCard.kt:73-118)
                    HStack(spacing: 6) {
                        Text(member.name)
                            .font(.subheadline).fontWeight(.bold)
                            .foregroundStyle(textColor)
                            .lineLimit(1)

                        if member.claimedByUserId != nil {
                            alarmStatusBadge
                        }

                        if member.isAwakeToday && dayInfo?.dayLabel == nil {
                            Text("☀️").font(.caption2)
                        }
                    }

                    // Row 2: Next active day label (Android MemberCard.kt:121-147)
                    if let info = dayInfo {
                        if let dayLabel = info.dayLabel {
                            Text(dayLabel)
                                .font(.caption2).fontWeight(.bold)
                                .foregroundStyle(textColor.opacity(0.7))
                        }
                        // Wake time range (Android MemberCard.kt:148-153)
                        HStack(spacing: 4) {
                            Image(systemName: "alarm.fill")
                                .font(.caption)
                                .foregroundStyle(textColor.opacity(0.6))
                            Text("\(info.earliest) – \(info.latest)")
                                .font(.subheadline).fontWeight(.bold)
                                .foregroundStyle(textColor)
                        }

                        // Bathroom + Breakfast info
                        Text("🛁 \(member.bathroomDurationMinutes) min   ☕ \(member.wantsBreakfast ? L.s("yes") : L.s("no"))")
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

                    // Edit: eigene oder unclaimed zur Visualisierung (ganze Kachel ist klickbar)
                    if member.claimedByUserId == nil || isMyProfile {
                        Button(action: onEdit) {
                            Image(systemName: "pencil.circle.fill")
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
                if offset == 0 {
                    let cal = Calendar.current
                    let now = Date()
                    let nowMinutes = cal.component(.hour, from: now) * 60 + cal.component(.minute, from: now)
                    if nowMinutes >= profile.latestWakeUp.totalMinutes {
                        continue
                    }
                }

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
