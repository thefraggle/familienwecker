import SwiftUI

struct MainView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @EnvironmentObject var appState: AppState
    @State private var showSettings = false
    @State private var showAddMember = false
    @State private var editMemberId: String? = nil
    @State private var memberToDelete: FamilyMember? = nil
    @State private var showDeleteMemberAlert = false

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    colors: appState.colorScheme == .dark
                        ? [Color(.systemBackground), Color(.secondarySystemBackground)]
                        : [Color.accentColor.opacity(0.08), Color(.systemBackground)],
                    startPoint: .top, endPoint: .bottom
                ).ignoresSafeArea()

                List {
                    // Error Message
                    if let err = familyViewModel.errorMessage {
                        errorCard(err)
                    }

                    // Alarm Toggle Card
                    alarmToggleSection

                    // Snooze Banner
                    if let snooze = familyViewModel.snoozeUntil, familyViewModel.myMemberId != nil {
                        snoozeBanner(until: snooze)
                    }

                    // Kein Profil gewählt
                    if familyViewModel.myMemberId == nil && !familyViewModel.members.isEmpty {
                        noProfileWarning
                    }

                    // Schedule
                    scheduleSection

                    // Mitgliederliste
                    memberSection
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .contentMargins(.bottom, 88, for: .scrollContent)
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("FamWake ").font(.headline).bold() +
                    Text(L.appNameShort).font(.headline).fontWeight(.regular)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 8) {
                        // Offline / Sync Icon
                        if familyViewModel.isOffline {
                            Image(systemName: "icloud.slash")
                                .foregroundStyle(.secondary)
                                .font(.caption)
                        } else if familyViewModel.isSyncing {
                            RotatingIcon(systemName: "arrow.triangle.2.circlepath")
                                .font(.caption)
                        }

                        Button(action: { showSettings = true }) {
                            Image(systemName: "gearshape.fill")
                        }
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
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
                Text("\(member.name)?")
            }
            .onChange(of: familyViewModel.familyId) { _, newId in
                if newId == nil { appState.route = .familySetup }
            }
        }
        .safeAreaInset(edge: .bottom) {
            // FAB – Mitglied hinzufügen
            HStack {
                Spacer()
                Button(action: { showAddMember = true }) {
                    Label(L.addMemberTitleAdd, systemImage: "plus")
                        .font(.headline)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color.accentColor)
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                        .shadow(color: .accentColor.opacity(0.4), radius: 8, x: 0, y: 4)
                }
                .buttonStyle(BounceButtonStyle())
                .padding(.trailing, 20)
                .padding(.bottom, 8)
            }
        }
    }

    // MARK: - Sections

    @ViewBuilder
    private var alarmToggleSection: some View {
        Section {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(familyViewModel.isAlarmEnabled ? L.mainAlarmEnabled : L.mainAlarmDisabled)
                            .font(.headline).fontWeight(.black)
                        Text(familyViewModel.isAlarmEnabled ? L.mainAlarmEnabledDesc : L.mainAlarmDisabledDesc)
                            .font(.subheadline).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { familyViewModel.isAlarmEnabled },
                        set: { familyViewModel.setAlarmEnabled($0) }
                    ))
                    .disabled(familyViewModel.myMemberId == nil)
                }

                // Tooltip F
                if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipSwitchSeen && familyViewModel.myMemberId != nil {
                    TooltipBubble(text: L.tooltipAlarmSwitch) {
                        familyViewModel.markTooltipSeen(familyViewModel.tooltipKeySwitch)
                    }
                }

                // „Bin wach" Button
                if familyViewModel.myMemberId != nil && familyViewModel.isAlarmEnabled {
                    let isAwake = familyViewModel.isAwakeTodayLocal
                    Button(action: {
                        familyViewModel.myMemberId.map { familyViewModel.toggleAwakeMember($0) }
                    }) {
                        HStack {
                            Image(systemName: "sun.max.fill")
                                .font(.title3)
                            Text(isAwake ? L.awakeActiveDesc : L.awakeTodayDesc)
                                .font(.headline)
                            if isAwake {
                                Spacer()
                                Image(systemName: "checkmark")
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .padding(.horizontal, 16)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(isAwake ? Color.green : Color.accentColor)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .buttonStyle(BounceButtonStyle())
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
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 0, trailing: 16))
    }

    @ViewBuilder
    private func snoozeBanner(until: Date) -> some View {
        Section {
            HStack {
                Image(systemName: "zzz")
                    .foregroundStyle(Color.onlineIconLight)
                Text(L.mainSnoozeActive(timeString(until)))
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "#1B5E20"))
                Spacer()
                Button(L.cancelButton) {
                    familyViewModel.myMemberId.map { familyViewModel.cancelSnooze($0) }
                }
                .font(.caption)
                .foregroundStyle(Color.onlineIconLight)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(Color.onlineGreenLight)
            .clipShape(RoundedRectangle(cornerRadius: 32))
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
    }

    @ViewBuilder
    private var noProfileWarning: some View {
        Section {
            VStack(alignment: .leading, spacing: 4) {
                Text("⚠️ \(L.mainNoProfileWarning)")
                    .font(.subheadline).fontWeight(.bold)
                    .foregroundStyle(Color(.systemRed))
                Text(L.mainNoProfileWarningDesc)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding()
            .famWakeCard(cornerRadius: 24, isDark: appState.colorScheme == .dark)
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
    }

    @ViewBuilder
    private var scheduleSection: some View {
        Section {
            Text(L.mainCurrentSchedule)
                .font(.title3).fontWeight(.black)
                .padding(.top, 8)

            if let sched = familyViewModel.schedule {
                if !sched.isValid || sched.memberSchedules.isEmpty {
                    EmptyStateView(
                        title: L.emptyScheduleTitle,
                        description: L.emptyScheduleDescription,
                        systemImage: "moon.stars.fill"
                    )
                } else {
                    // Schedule Card
                    VStack(alignment: .leading, spacing: 6) {
                        Text(familyViewModel.isAlarmEnabled ? "✅ \(L.mainOptimalPlan)" : "⏸️ \(L.mainPlanPaused)")
                            .fontWeight(.bold)
                        if let breakfast = sched.breakfastTime {
                            Text("☕ \(L.mainScheduleBathroom(breakfast.formatted(), breakfast.formatted()))")
                                .font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    .padding()
                    .famWakeCard(cornerRadius: 24, isDark: appState.colorScheme == .dark)

                    // Tooltip B (Drag)
                    if sched.memberSchedules.count > 1 && familyViewModel.tooltipsEnabled && !familyViewModel.tooltipDragSeen {
                        TooltipBubble(text: L.tooltipDragHandle) {
                            familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyDrag)
                        }
                    }

                    // Drag & Drop Weckzeiten-Kacheln
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
                    systemImage: "moon.stars.fill"
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
                Text("⏰ \(sched.wakeUpTime.formatted()) – \(sched.member.name)")
                    .font(.headline).fontWeight(.bold)
                Text(L.mainScheduleBathroom(sched.bathroomStart.formatted(), sched.bathroomEnd.formatted()))
                    .font(.subheadline).foregroundStyle(.secondary)
                if let leave = sched.member.leaveHomeTime {
                    Text(L.mainScheduleLeave(leave.formatted()))
                        .font(.subheadline).foregroundStyle(.secondary)
                }
            }
            Spacer()
            Image(systemName: "line.3.horizontal")
                .foregroundStyle(.secondary)
        }
        .padding()
        .famWakeCard(cornerRadius: 16, isDark: appState.colorScheme == .dark)
    }

    @ViewBuilder
    private var memberSection: some View {
        Section {
            Divider()
            ForEach(familyViewModel.members) { member in
                MemberCardView(
                    member: member,
                    isMyProfile: member.id == familyViewModel.myMemberId,
                    onEdit: { editMemberId = member.id },
                    onDelete: {
                        memberToDelete = member
                        showDeleteMemberAlert = true
                    },
                    onTogglePause: {
                        var updated = member
                        updated.isPaused.toggle()
                        familyViewModel.addOrUpdateMember(updated)
                    }
                )
            }
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
    }

    @ViewBuilder
    private func errorCard(_ error: String) -> some View {
        Section {
            VStack(alignment: .leading, spacing: 8) {
                Text("⚠️ \(error)")
                    .font(.subheadline)
                    .foregroundStyle(Color(.systemRed))
                Button(L.cancelButton) { familyViewModel.clearErrorMessage() }
                    .font(.caption)
            }
            .padding()
            .background(Color(.systemRed).opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 24))
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 0, trailing: 16))
    }

    private func timeString(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.string(from: date)
    }
}

// Helper für identifiable String in Sheet
struct IdentifiableString: Identifiable {
    var id: String { value }
    let value: String
}
