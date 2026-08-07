import SwiftUI

struct ScheduleSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var pendingReorderFrom: Int?
    @Binding var pendingReorderTo: Int?
    @Binding var showReorderConfirmation: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        Group {
            Text(L.mainCurrentSchedule)
                .font(.title2).fontWeight(.black)
                .foregroundStyle(theme.onBackground)
                .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 4, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

            // Weekday pills (Android style) – ViewThatFits für iPhone SE (320pt) Kompatibilität
            ViewThatFits(in: .horizontal) {
                weekdayPills
                ScrollView(.horizontal, showsIndicators: false) {
                    weekdayPills
                }
            }
            .padding(.vertical, 8)
            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 8, trailing: 16))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            if let sched = familyViewModel.schedule {
                if sched.memberSchedules.isEmpty {
                    EmptyStateView(
                        title: L.emptyScheduleTitle,
                        description: L.emptyScheduleDescription,
                        lottieName: "mond"
                    )
                } else {
                    if !sched.isValid {
                        // Error Card with AutoFix (styled matching Android)
                        let cardColor = colorScheme == .dark ? Color.snoozeAmberDark.opacity(0.8) : Color.snoozeAmberLight.opacity(0.9)
                        let textColor = colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight
                        
                        VStack(alignment: .leading, spacing: 8) {
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundStyle(textColor)
                                    .font(.title3)
                                
                                let msgText: String = {
                                    switch sched.scheduleMessage {
                                    case .memberConflict(let name):
                                        if name.isEmpty {
                                            return L.s("schedule_message_no_valid")
                                        } else {
                                            return String(format: L.s("schedule_message_member_conflict"), name)
                                        }
                                    default:
                                        return L.s("schedule_message_no_valid")
                                    }
                                }()
                                
                                Text(msgText)
                                    .fontWeight(.bold)
                                    .foregroundStyle(textColor)
                            }
                            
                            let descText: String = {
                                switch sched.scheduleMessage {
                                case .memberConflict(let name):
                                    if name.isEmpty {
                                        return L.s("schedule_message_no_valid_desc")
                                    } else {
                                        return L.s("schedule_message_member_conflict_desc")
                                    }
                                default:
                                    return L.s("schedule_message_no_valid_desc")
                                }
                            }()
                            
                            Text(descText)
                                .font(.caption)
                                .foregroundStyle(textColor.opacity(0.85))
                                .padding(.leading, 28)
                            
                            // Fallback info for claimed user
                            if let myId = familyViewModel.myMemberId,
                               let myMember = sched.memberSchedules.first(where: { $0.member.id == myId }),
                               familyViewModel.isAlarmEnabled {
                                Spacer(minLength: 4)
                                Text(L.mainFallbackAlarmActive(myMember.wakeUpTime.formatted()))
                                    .font(.caption).fontWeight(.bold)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(textColor.opacity(0.1))
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    .padding(.leading, 28)
                                    .foregroundStyle(textColor)
                            }
                            
                            Spacer(minLength: 8)
                            
                            Button(action: {
                                familyViewModel.applyAutoFix()
                            }) {
                                Text(L.scheduleAutoFix)
                                    .font(.subheadline).fontWeight(.bold)
                                    .foregroundStyle(theme.onPrimary)
                                    .padding(.horizontal, 20)
                                    .padding(.vertical, 10)
                                    .background(theme.primary)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(BounceButtonStyle())
                            .accessibilityLabel(L.s("accessibility_autofix"))
                            .padding(.leading, 28)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(cardColor)
                        .background(.regularMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(textColor.opacity(0.4), lineWidth: 1)
                        )
                        .padding(.bottom, 12)
                    } else {
                        // Schedule Card
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(spacing: 6) {
                                Image(systemName: familyViewModel.isAlarmEnabled ? "checkmark.circle.fill" : "pause.circle.fill")
                                    .foregroundStyle(familyViewModel.isAlarmEnabled ? theme.primary : theme.outline)
                                Text(familyViewModel.isAlarmEnabled ? L.mainOptimalPlan : L.mainPlanPaused)
                                    .fontWeight(.bold)
                                    .foregroundStyle(familyViewModel.isAlarmEnabled ? theme.onPrimaryContainer : theme.onSurfaceVariant)
                            }
                            
                            if familyViewModel.isAlarmEnabled, let targetDate = sched.targetDate {
                                Text(targetDate.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(LanguageManager.shared.currentLocale)))
                                    .font(.subheadline).fontWeight(.bold)
                                    .foregroundStyle(theme.primary)
                            } else if !familyViewModel.isAlarmEnabled, let targetDate = sched.targetDate {
                                Text(targetDate.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(LanguageManager.shared.currentLocale)))
                                    .font(.subheadline)
                                    .foregroundStyle(theme.primary.opacity(0.8))
                            }
                            
                            let msgText: String? = {
                                switch sched.scheduleMessage {
                                case .timeAdjusted(let min):
                                    return L.scheduleMessageTimeAdjusted(min)
                                case .breakfastReduced(let min):
                                    return L.scheduleMessageBreakfastReduced(min)
                                case .breakfastAndTimeAdjusted(let r, let s):
                                    return L.scheduleMessageBreakfastAndTimeAdjusted(r, s)
                                case .bufferReduced(let r, let s):
                                    return L.scheduleMessageBufferReduced(r, s)
                                default:
                                    return nil
                                }
                            }()
                            
                            if let warn = msgText {
                                Text("⚠️ \(warn)")
                                    .font(.subheadline)
                                    .foregroundStyle(Color.red)
                                    .padding(.top, 4)
                            }
                            
                            if let breakfast = sched.breakfastTime {
                                Text(L.mainSharedBreakfast(breakfast.formatted()))
                                    .font(.subheadline).foregroundStyle(theme.onSurfaceVariant.opacity(0.8))
                            }
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .famWakeCard(cornerRadius: 24, isDark: colorScheme == .dark)
                        .padding(.bottom, 12)
                        
                        unclaimedFirstWarning
                        
                        // Tooltip B (Drag)
                        if sched.memberSchedules.count > 1 && familyViewModel.tooltipsEnabled && !familyViewModel.tooltipDragSeen {
                            TooltipBubble(text: L.tooltipDragHandle) {
                                familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyDrag)
                            }
                        }

                        // Drag & Drop schedule tiles
                        ForEach(Array(sched.memberSchedules.enumerated()), id: \.element.id) { index, memberSched in
                            VStack(spacing: 0) {
                                scheduleCard(memberSched)
                                
                                if index < sched.memberSchedules.count - 1 && memberSched.bufferAfter > 0 {
                                    HStack(alignment: .center) {
                                        Rectangle()
                                            .fill(theme.outline.opacity(0.2))
                                            .frame(height: 1)
                                        
                                        HStack(spacing: 4) {
                                            Image(systemName: "timer")
                                                .font(.caption2)
                                                .foregroundStyle(theme.outline.opacity(0.5))
                                            Text(L.bufferBetweenDisplay(Int(memberSched.bufferAfter)))
                                                .font(.caption2)
                                                .fontWeight(.medium)
                                                .foregroundStyle(theme.outline.opacity(0.6))
                                        }
                                        .padding(.horizontal, 4)
                                        
                                        Rectangle()
                                            .fill(theme.outline.opacity(0.2))
                                            .frame(height: 1)
                                    }
                                    .padding(.top, 16)
                                    .padding(.bottom, 16)
                                    .padding(.horizontal, 8)
                                } else {
                                    Spacer(minLength: 16)
                                        .frame(height: 16)
                                }
                            }
                            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                        }
                        .onMove { from, to in
                            if let fromIdx = from.first {
                                pendingReorderFrom = fromIdx
                                pendingReorderTo = to
                                showReorderConfirmation = true
                            }
                        }
                    }
                }
            } else {
                EmptyStateView(
                    title: L.emptyScheduleTitle,
                    description: L.emptyScheduleDescription,
                    lottieName: "mond"
                )
            }
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
    }
    
    // MARK: - Weekday-Chips (extrahiert für ViewThatFits)
    private var weekdayPills: some View {
        HStack(spacing: 4) {
            let calendar = Calendar.current
            let symbols = calendar.shortWeekdaySymbols
            let daysOfWeek = [symbols[1], symbols[2], symbols[3], symbols[4], symbols[5], symbols[6], symbols[0]]
            
            ForEach(1...7, id: \.self) { dayValue in
                let dayName = daysOfWeek[dayValue - 1]
                let isSelected = familyViewModel.selectedDayOfWeek == dayValue
                
                Button(action: {
                    if isSelected {
                        familyViewModel.selectDayOfWeek(nil)
                    } else {
                        familyViewModel.selectDayOfWeek(dayValue)
                    }
                }) {
                    Text(dayName.prefix(2).uppercased())
                        .font(.caption)
                        .fontWeight(isSelected ? .bold : .semibold)
                        .frame(minWidth: 38, maxWidth: .infinity)
                        .frame(height: 38)
                        .background(isSelected ? theme.primary : (colorScheme == .dark ? theme.surfaceVariant.opacity(0.3) : theme.surfaceVariant.opacity(0.6)))
                        .foregroundStyle(isSelected ? theme.onPrimary : theme.onSurfaceVariant)
                        .clipShape(Circle())
                        .overlay(
                            Circle()
                                .stroke(isSelected ? theme.primary : theme.outline.opacity(0.1), lineWidth: 1)
                        )
                }
                .buttonStyle(BounceButtonStyle())
                .accessibilityLabel("\(dayName)\(isSelected ? L.s("accessibility_selected") : "")")
            }
        }
    }
    
    // Warnung: Ungeclaimter Member an erster Stelle im Schedule (Android MainScreen.kt:517-528)
    @ViewBuilder
    private var unclaimedFirstWarning: some View {
        let firstScheduledMember = familyViewModel.schedule?.memberSchedules.first?.member
        let showWarning = familyViewModel.myMemberId != nil
            && firstScheduledMember != nil
            && firstScheduledMember?.claimedByUserId == nil
            && firstScheduledMember?.id != familyViewModel.myMemberId

        if showWarning, let member = firstScheduledMember {
            Group {
                VStack(alignment: .leading, spacing: 4) {
                    Text("⚠️ \(String(format: L.s("main_unclaimed_first_title"), member.name))")
                        .font(.subheadline).fontWeight(.bold)
                        .foregroundStyle(colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight)
                    Text(String(format: L.s("main_unclaimed_first_desc"), member.name))
                        .font(.caption)
                        .foregroundStyle((colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight).opacity(0.85))
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(colorScheme == .dark ? Color.snoozeAmberDark.opacity(0.8) : Color.snoozeAmberLight.opacity(0.9))
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke((colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight).opacity(0.4), lineWidth: 1)
                )
                .padding(.bottom, 12)
            }
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
        }
    }

    @ViewBuilder
    private func scheduleCard(_ sched: MemberSchedule) -> some View {
        let isSnoozed = sched.member.snoozeUntil != nil && sched.member.snoozeUntil! > Date()
        let isOtherMember = sched.member.id != familyViewModel.myMemberId
        let isMe = !isOtherMember
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: "alarm")
                        .foregroundStyle(theme.error)
                    Text("\(sched.wakeUpTime.formatted()) \u{2013} \(sched.member.name)")
                        .font(.headline).fontWeight(.bold)
                        .foregroundStyle(theme.onPrimaryContainer)
                    if isMe {
                        Text(L.s("schedule_you_badge"))
                            .font(.caption2).fontWeight(.bold)
                            .foregroundStyle(theme.primary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(theme.primary.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    }
                    if isSnoozed {
                        Text("\u{1F4A4}")
                    }
                    if isSnoozed && isOtherMember {
                        Text(L.scheduleMemberSnoozed)
                            .font(.caption)
                            .foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                    }
                }
                
                HStack(spacing: 6) {
                    Image(systemName: "bathtub.fill")
                        .font(.caption)
                        .foregroundStyle(theme.onSurfaceVariant.opacity(0.6))
                    Text(L.mainScheduleBathroom(sched.bathroomStart.formatted(), sched.bathroomEnd.formatted()))
                        .font(.subheadline).foregroundStyle(theme.onSurfaceVariant.opacity(0.8))
                }
                
                if let leave = sched.member.leaveHomeTime {
                    Text(L.mainScheduleLeave(leave.formatted()))
                        .font(.subheadline).foregroundStyle(theme.onSurfaceVariant.opacity(0.8))
                }
            }
            Spacer()
            Image(systemName: "line.3.horizontal")
                .foregroundStyle(theme.outline.opacity(0.6))
                .font(.title3)
                .accessibilityLabel(L.s("accessibility_drag_handle"))
        }
        .padding()
        .background(
            isMe
            ? (colorScheme == .dark ? theme.primary.opacity(0.12) : theme.primaryContainer.opacity(0.3))
            : Color.clear
        )
        .famWakeCard(cornerRadius: 16, isDark: colorScheme == .dark)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(isMe ? theme.primary.opacity(0.5) : Color.clear, lineWidth: 1.5)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(L.s("accessibility_schedule_card", sched.member.name, sched.wakeUpTime.formatted(), sched.bathroomStart.formatted(), sched.bathroomEnd.formatted()))
        .accessibilityIdentifier("member_card_\(sched.member.id)")
    }
}
