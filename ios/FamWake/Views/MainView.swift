import SwiftUI
import UserNotifications

struct MainView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var appState: AppState
    @Environment(\.scenePhase) var scenePhase
    @State private var showSettings = false
    @State private var showAddMember = false
    @State private var editMemberId: String? = nil
    @State private var memberToDelete: FamilyMember? = nil
    @State private var showDeleteMemberAlert = false
    @State private var showLoginSheet = false
    @Environment(\.colorScheme) private var colorScheme

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        NavigationStack {
            ZStack {
                // Background gradient matching Android MainScreen
                LinearGradient(
                    colors: colorScheme == .dark
                        ? [theme.surface, theme.background]
                        : [theme.primaryContainer.opacity(0.5), theme.background],
                    startPoint: .top, endPoint: .bottom
                ).ignoresSafeArea()

                List {
                    // Error Message
                    if let err = familyViewModel.errorMessage {
                        errorCard(err)
                            .padding(.top, 16)
                    }

                    // Alarm Toggle Card
                    alarmToggleSection

                    // Snooze Banner
                    if let snooze = familyViewModel.snoozeUntil, familyViewModel.myMemberId != nil {
                        snoozeBanner(until: snooze)
                    }

                    // No profile selected warning
                    if familyViewModel.myMemberId == nil && !familyViewModel.members.isEmpty {
                        noProfileWarning
                    }

                    // Unclaimed first member warning (Android MainScreen.kt:517-528)
                    unclaimedFirstWarning

                    // Schedule
                    scheduleSection

                    // Member list
                    memberSection
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .contentMargins(.bottom, 88, for: .scrollContent)
            }
            .navigationTitle(L.appNameShort)
            .onAppear {
                UIApplication.shared.isIdleTimerDisabled = true
            }
            .onDisappear {
                UIApplication.shared.isIdleTimerDisabled = false
            }
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if familyViewModel.isOffline {
                        Image(systemName: "icloud.slash")
                            .foregroundStyle(theme.outline)
                            .font(.caption)
                    } else if familyViewModel.isSyncing {
                        RotatingIcon(systemName: "arrow.triangle.2.circlepath", color: theme.tertiary)
                            .font(.caption)
                    }

                    Button(action: { showSettings = true }) {
                        Image(systemName: "gearshape.fill")
                            .foregroundStyle(theme.onSurface)
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
                    .environment(\.colorScheme, colorScheme)
                    .preferredColorScheme(colorScheme)
            }
            .sheet(isPresented: $showAddMember) {
                AddEditMemberView(memberId: nil) { showAddMember = false }
            }
            .sheet(item: Binding(
                get: { editMemberId.map { IdentifiableString(value: $0) } },
                set: { editMemberId = $0?.value }
            )) { id in
                AddEditMemberView(memberId: id.value) { editMemberId = nil }
            }
            .alert(L.settingsDeleteMemberTitle, isPresented: $showDeleteMemberAlert, presenting: memberToDelete) { member in
                Button(L.settingsDeleteMemberConfirm, role: .destructive) {
                    familyViewModel.deleteMember(member.id)
                }
                Button(L.cancelButton, role: .cancel) {}
            } message: { member in
                Text(String(format: L.s("delete_member_text"), member.name))
            }
            .onChange(of: familyViewModel.familyId) { _, newId in
                if newId == nil { appState.route = .familySetup }
            }
            .onAppear {
                if familyViewModel.familyId == nil {
                    appState.route = .familySetup
                }
            }
            .onChange(of: scenePhase) { _, newPhase in
                if newPhase == .active {
                    if familyViewModel.errorMessage == L.errorAlarmPermission {
                        UNUserNotificationCenter.current().getNotificationSettings { settings in
                            if settings.authorizationStatus == .authorized {
                                DispatchQueue.main.async {
                                    familyViewModel.clearErrorMessage()
                                    familyViewModel.recalculateSchedule()
                                }
                            }
                        }
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            VStack(alignment: .trailing, spacing: 16) {
                // Share FAB
                if authViewModel.isLoggedIn && !authViewModel.isAnonymous && familyViewModel.familyId != nil,
                   let fName = familyViewModel.familyName,
                   let code = familyViewModel.joinCode {
                    HStack {
                        Spacer()
                        ShareLink(item: L.settingsShareMessage(fName, code)) {
                            Image(systemName: "square.and.arrow.up")
                                .font(.title3)
                                .padding(14)
                                .background(theme.primaryContainer)
                                .foregroundStyle(theme.onPrimaryContainer)
                                .clipShape(Circle())
                                .shadow(color: theme.primaryContainer.opacity(0.4), radius: 8, x: 0, y: 4)
                        }
                        .buttonStyle(BounceButtonStyle())
                        .padding(.trailing, 20)
                    }
                }
                
                // FAB – Add Member (matching Android FAB)
                if familyViewModel.members.count < 6 {
                    HStack {
                        Spacer()
                        Button(action: { showAddMember = true }) {
                            Label(L.addMemberTitleAdd, systemImage: "plus")
                                .font(.headline)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 12)
                                .background(theme.primary)
                                .foregroundStyle(theme.onPrimary)
                                .clipShape(Capsule())
                                .shadow(color: theme.primary.opacity(0.4), radius: 8, x: 0, y: 4)
                        }
                        .buttonStyle(BounceButtonStyle())
                        .padding(.trailing, 20)
                        .padding(.bottom, 8)
                    }
                }
            }
        }
    }

    // MARK: - Sections

    @ViewBuilder
    private var alarmToggleSection: some View {
        Group {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(familyViewModel.isAlarmEnabled ? L.mainAlarmEnabled : L.mainAlarmDisabled)
                            .font(.title3).fontWeight(.bold)
                            .foregroundStyle(theme.onPrimaryContainer)
                        Text(familyViewModel.isAlarmEnabled ? L.mainAlarmEnabledDesc : L.mainAlarmDisabledDesc)
                            .font(.subheadline).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { familyViewModel.isAlarmEnabled },
                        set: { familyViewModel.setAlarmEnabled($0) }
                    ))
                    .labelsHidden()
                    .disabled(familyViewModel.myMemberId == nil)
                    .tint(theme.secondary)
                }

                // Tooltip F
                if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipSwitchSeen && familyViewModel.myMemberId != nil {
                    TooltipBubble(text: L.tooltipAlarmSwitch) {
                        familyViewModel.markTooltipSeen(familyViewModel.tooltipKeySwitch)
                    }
                }

                // "I'm awake" Button
                if familyViewModel.isAwakeButtonVisible {
                    let isAwake = familyViewModel.isAwakeTodayLocal
                    Button(action: {
                        familyViewModel.myMemberId.map { familyViewModel.toggleAwakeMember($0) }
                    }) {
                        HStack {
                            Image(systemName: isAwake ? "sun.max.fill" : "sun.max")
                                .font(.body)
                            Text(isAwake ? L.awakeActiveDesc : L.awakeTodayDesc)
                                .font(.subheadline).fontWeight(.semibold)
                            if isAwake {
                                Spacer()
                                Image(systemName: "checkmark")
                                    .font(.body).fontWeight(.bold)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(minHeight: 48)
                        .padding(.horizontal, 16)
                        .background(isAwake ? theme.secondary : theme.primary)
                        .foregroundStyle(isAwake ? theme.onSecondary : theme.onPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .animation(.easeInOut(duration: 0.2), value: isAwake)

                    // Tooltip A
                    if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipAwakeSeen {
                        TooltipBubble(text: L.tooltipAwakeButton) {
                            familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyAwake)
                        }
                    }
                }
            }
            .padding()
            .famWakeCard(cornerRadius: 32, isDark: appState.colorScheme == .dark)
            .padding(.bottom, 12)
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 24, trailing: 16))
    }

    @ViewBuilder
    private func snoozeBanner(until: Date) -> some View {
        Group {
            HStack {
                Image(systemName: "zzz")
                    .foregroundStyle(appState.colorScheme == .dark ? Color.onlineIconDark : Color.onlineIconLight)
                Text(L.mainSnoozeActive(timeString(until)))
                    .font(.subheadline)
                    .foregroundStyle(appState.colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight)
                Spacer()
                Button(L.cancelButton) {
                    familyViewModel.myMemberId.map { familyViewModel.cancelSnooze($0) }
                }
                .font(.caption)
                .foregroundStyle(appState.colorScheme == .dark ? Color.onlineIconDark : Color.onlineIconLight)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(appState.colorScheme == .dark ? Color.onlineGreenDark.opacity(0.8) : Color.onlineGreenLight)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
            .padding(.bottom, 12)
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
    }

    @ViewBuilder
    private var noProfileWarning: some View {
        Group {
            Button(action: { showSettings = true }) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("⚠️ \(L.mainNoProfileWarning)")
                        .font(.subheadline).fontWeight(.bold)
                        .foregroundStyle(theme.onErrorContainer)
                    Text(L.mainNoProfileWarningDesc)
                        .font(.caption)
                        .foregroundStyle(theme.onErrorContainer.opacity(0.8))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(
                    .regularMaterial,
                    in: RoundedRectangle(cornerRadius: 24, style: .continuous)
                )
                .background(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(appState.colorScheme == .dark ? theme.errorContainer.opacity(0.4) : theme.errorContainer.opacity(0.8))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(theme.outline.opacity(0.2), lineWidth: 1)
                )
                .padding(.bottom, 12)
            }
            .buttonStyle(.plain)
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
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
                        .foregroundStyle(appState.colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight)
                    Text(String(format: L.s("main_unclaimed_first_desc"), member.name))
                        .font(.caption)
                        .foregroundStyle((appState.colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight).opacity(0.85))
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(appState.colorScheme == .dark ? Color.snoozeAmberDark.opacity(0.8) : Color.snoozeAmberLight.opacity(0.9))
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke((appState.colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight).opacity(0.4), lineWidth: 1)
                )
                .padding(.bottom, 12)
            }
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
        }
    }

    @ViewBuilder
    private var scheduleSection: some View {
        Group {
            Text(L.mainCurrentSchedule)
                .font(.title2).fontWeight(.black)
                .foregroundStyle(theme.onBackground)
                .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 4, trailing: 16))
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
                        // Error Card with AutoFix
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundStyle(Color.orange)
                                Text("Konflikt im Zeitplan")
                                    .fontWeight(.bold)
                                    .foregroundStyle(Color.orange)
                            }
                            
                            let msgText: String = {
                                switch sched.scheduleMessage {
                                case .memberConflict(let name):
                                    return String(format: L.s("schedule_message_member_conflict"), name)
                                default:
                                    return "Zeiten überschneiden sich."
                                }
                            }()
                            
                            Text(msgText)
                                .font(.subheadline)
                                .foregroundStyle(theme.onSurfaceVariant)
                            
                            Button(action: {
                                familyViewModel.applyAutoFix()
                            }) {
                                Text(L.scheduleAutoFix)
                                    .font(.subheadline).fontWeight(.bold)
                                    .foregroundStyle(theme.onPrimary)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(theme.primary)
                                    .cornerRadius(8)
                            }
                            .padding(.top, 4)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.orange.opacity(0.15))
                        .background(.regularMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(Color.orange.opacity(0.5), lineWidth: 1)
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
                                Text(targetDate.formatted(.dateTime.weekday(.wide).day().month(.wide)))
                                    .font(.subheadline).fontWeight(.bold)
                                    .foregroundStyle(theme.primary)
                            } else if !familyViewModel.isAlarmEnabled, let targetDate = sched.targetDate {
                                Text(targetDate.formatted(.dateTime.weekday(.wide).day().month(.wide)))
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
                        .famWakeCard(cornerRadius: 24, isDark: appState.colorScheme == .dark)
                        .padding(.bottom, 12)
                    }

                    // Tooltip B (Drag)
                    if sched.memberSchedules.count > 1 && familyViewModel.tooltipsEnabled && !familyViewModel.tooltipDragSeen {
                        TooltipBubble(text: L.tooltipDragHandle) {
                            familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyDrag)
                        }
                    }

                    // Drag & Drop schedule tiles
                    ForEach(sched.memberSchedules) { memberSched in
                        scheduleCard(memberSched)
                    }
                    .onMove { from, to in
                        if let fromIdx = from.first {
                            let movedMemberId = sched.memberSchedules[fromIdx].member.id
                            let targetMemberId = to < sched.memberSchedules.count
                                ? sched.memberSchedules[to].member.id : nil
                            if let fromInMembers = familyViewModel.members.firstIndex(where: { $0.id == movedMemberId }) {
                                let toInMembers: Int
                                if let targetId = targetMemberId,
                                   let t = familyViewModel.members.firstIndex(where: { $0.id == targetId }) {
                                    toInMembers = fromInMembers < t ? t - 1 : t
                                } else {
                                    toInMembers = familyViewModel.members.count - 1
                                }
                                familyViewModel.moveMemberOrder(fromInMembers, toInMembers)
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

    @ViewBuilder
    private func scheduleCard(_ sched: MemberSchedule) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: "alarm")
                        .foregroundStyle(theme.error)
                    Text("\(sched.wakeUpTime.formatted()) – \(sched.member.name)")
                        .font(.headline).fontWeight(.bold)
                        .foregroundStyle(theme.onPrimaryContainer)
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
        }
        .padding()
        .famWakeCard(cornerRadius: 16, isDark: appState.colorScheme == .dark)
        .padding(.bottom, 12)
    }

    @ViewBuilder
    private var memberSection: some View {
        Group {
            Text(L.mainFamilyMembers)
                .font(.title2).fontWeight(.black)
                .foregroundStyle(theme.onBackground)
                .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 4, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

            // Global Buffer Stepper (Android style)
            if familyViewModel.members.count > 1 {
                HStack {
                    Image(systemName: "timer")
                        .foregroundStyle(theme.primary)
                    Text(L.bufferAfterBath)
                        .font(.subheadline)
                        .foregroundStyle(theme.onBackground)
                    Spacer()
                    
                    Button(action: {
                        familyViewModel.setGlobalBufferMinutes(max(0, familyViewModel.globalBufferMinutes - 5))
                    }) {
                        Image(systemName: "minus.circle.fill")
                            .foregroundStyle(familyViewModel.globalBufferMinutes > 0 ? theme.primary : theme.outline.opacity(0.3))
                            .font(.title2)
                    }
                    .disabled(familyViewModel.globalBufferMinutes <= 0)
                    .buttonStyle(.plain)
                    
                    Text("\(familyViewModel.globalBufferMinutes) min")
                        .font(.subheadline).fontWeight(.bold)
                        .frame(minWidth: 50, alignment: .center)
                    
                    Button(action: {
                        familyViewModel.setGlobalBufferMinutes(min(15, familyViewModel.globalBufferMinutes + 5))
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .foregroundStyle(familyViewModel.globalBufferMinutes < 15 ? theme.primary : theme.outline.opacity(0.3))
                            .font(.title2)
                    }
                    .disabled(familyViewModel.globalBufferMinutes >= 15)
                    .buttonStyle(.plain)
                }
                .padding()
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .padding(.bottom, 12)
            }

            if familyViewModel.members.count >= 6 {
                Text(L.mainMemberLimitReached)
                    .font(.footnote)
                    .foregroundStyle(theme.error)
                    .padding(.bottom, 8)
            }

            if familyViewModel.members.isEmpty {
                EmptyStateView(
                    title: L.emptyMembersTitle,
                    description: L.emptyMembersDescription,
                    lottieName: "family"
                )
                .padding(.vertical, 20)
            } else {
                ForEach(familyViewModel.members) { member in
                    MemberCardView(
                        member: member,
                        isMyProfile: member.id == familyViewModel.myMemberId,
                        isAlarmEnabled: familyViewModel.isAlarmEnabled,
                        onEdit: { editMemberId = member.id },
                        onDelete: {
                            memberToDelete = member
                            showDeleteMemberAlert = true
                        },
                        onTogglePause: {
                            familyViewModel.togglePauseMember(member.id)
                        }
                    )
                    .padding(.bottom, 12)
                }
            }
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
    }

    @ViewBuilder
    private func errorCard(_ error: String) -> some View {
        Group {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(theme.error)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(error)
                            .font(.subheadline)
                            .foregroundStyle(theme.error)
                        
                        if error == L.errorAlarmPermission {
                            Button(action: {
                                if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }) {
                                HStack(spacing: 4) {
                                    Text(L.settingsTitle)
                                        .font(.caption).fontWeight(.bold)
                                    Image(systemName: "chevron.right")
                                        .font(.caption2).fontWeight(.bold)
                                }
                                .foregroundStyle(theme.error)
                            }
                            .padding(.top, 4)
                        }
                    }
                    
                    Spacer()
                    Button(action: { familyViewModel.clearErrorMessage() }) {
                        Image(systemName: "xmark")
                            .foregroundStyle(theme.error)
                    }
                }
                
                if error == L.errorFamilyNotFound {
                    Button(L.settingsLeaveFamily) {
                        familyViewModel.leaveFamily()
                        appState.route = .familySetup
                    }
                    .font(.caption).fontWeight(.bold)
                    .foregroundStyle(theme.error)
                }
            }
            .padding()
            .background(theme.errorContainer.opacity(0.85))
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .padding(.bottom, 12)
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 0, trailing: 16))
    }

    private func timeString(_ date: Date) -> String {
        let f = DateFormatter()
        f.timeStyle = .short // Uses system 12h/24h format
        return f.string(from: date)
    }

}

// Helper for identifiable String in Sheet
struct IdentifiableString: Identifiable {
    var id: String { value }
    let value: String
}
