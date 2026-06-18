import SwiftUI
import UserNotifications
import Lottie

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
    @State private var pendingReorderFrom: Int? = nil
    @State private var pendingReorderTo: Int? = nil
    @State private var showReorderConfirmation = false

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
                    // Offline Banner
                    if familyViewModel.isOffline {
                        HStack(spacing: 8) {
                            Image(systemName: "wifi.slash")
                                .font(.subheadline)
                            Text(L.offlineWriteHint)
                                .font(.subheadline)
                                .lineLimit(2)
                                .minimumScaleFactor(0.8)
                        }
                        .foregroundStyle(colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(colorScheme == .dark ? Color.snoozeAmberDark.opacity(0.8) : Color.snoozeAmberLight.opacity(0.9))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 0, trailing: 16))
                        .accessibilityLabel(L.errorOffline)
                    }

                    // Error Message
                    if let err = familyViewModel.errorMessage {
                        errorCard(err)
                            .padding(.top, 16)
                    }

                    // Alarm Toggle Card
                    AlarmToggleSection()

                    // Snooze Banner
                    if let snooze = familyViewModel.snoozeUntil, familyViewModel.myMemberId != nil {
                        snoozeBanner(until: snooze)
                    }

                    // No profile selected warning
                    if familyViewModel.myMemberId == nil && !familyViewModel.members.isEmpty {
                        noProfileWarning
                    }

                    // Schedule
                    ScheduleSection(
                        pendingReorderFrom: $pendingReorderFrom,
                        pendingReorderTo: $pendingReorderTo,
                        showReorderConfirmation: $showReorderConfirmation
                    )

                    // Member list
                    MemberSection(
                        editMemberId: $editMemberId,
                        memberToDelete: $memberToDelete,
                        showDeleteMemberAlert: $showDeleteMemberAlert,
                        onAddMember: { showAddMember = true }
                    )
                }
                .listStyle(.plain)
                .listRowSpacing(0)
                .scrollContentBackground(.hidden)
                .contentMargins(.bottom, 160, for: .scrollContent)

                // FABs
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        VStack(alignment: .trailing, spacing: 16) {
                            if authViewModel.isLoggedIn && !authViewModel.isAnonymous && familyViewModel.familyId != nil,
                               let fName = familyViewModel.familyName,
                               let code = familyViewModel.joinCode {
                                ShareLink(item: L.settingsShareMessage(fName, code)) {
                                    Image(systemName: "square.and.arrow.up")
                                        .font(.title3.weight(.semibold))
                                        .foregroundColor(theme.onSecondaryContainer)
                                        .frame(width: 48, height: 48)
                                        .background(theme.secondaryContainer)
                                        .clipShape(Circle())
                                        .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
                                }
                                .accessibilityLabel(L.settingsShareCode)
                            }
                            
                            if familyViewModel.members.count < 6 {
                                Button(action: { showAddMember = true }) {
                                    HStack(spacing: 8) {
                                        Image(systemName: "plus")
                                            .font(.title2.weight(.semibold))
                                        if familyViewModel.members.isEmpty {
                                            Text(L.addMemberTitleAdd)
                                                .font(.headline.weight(.semibold))
                                        }
                                    }
                                    .foregroundColor(theme.onPrimary)
                                    .padding(.horizontal, familyViewModel.members.isEmpty ? 24 : 0)
                                    .frame(minWidth: 56, minHeight: 56)
                                    .background(theme.primary)
                                    .clipShape(Capsule())
                                    .shadow(color: .black.opacity(0.2), radius: 6, x: 0, y: 3)
                                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: familyViewModel.members.isEmpty)
                                }
                                .accessibilityLabel(L.addMemberTitleAdd)
                                .accessibilityHint("Neues Familienmitglied hinzufügen")
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle(L.appNameShort)
            .onAppear {
                UIApplication.shared.isIdleTimerDisabled = true
            }
            .onDisappear {
                UIApplication.shared.isIdleTimerDisabled = false
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    HStack(alignment: .center, spacing: 16) {
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
            .alert(
                L.s("reorder_dialog_title"),
                isPresented: $showReorderConfirmation
            ) {
                let dayName = reorderDayName
                
                Button(String(format: L.s("reorder_dialog_today"), dayName)) {
                    if let fromIdx = pendingReorderFrom, let toIdx = pendingReorderTo {
                        familyViewModel.moveMemberOrder(fromIndex: fromIdx, toIndex: toIdx, wholeWeek: false)
                    }
                    pendingReorderFrom = nil
                    pendingReorderTo = nil
                }
                
                Button(L.s("reorder_dialog_week")) {
                    if let fromIdx = pendingReorderFrom, let toIdx = pendingReorderTo {
                        familyViewModel.moveMemberOrder(fromIndex: fromIdx, toIndex: toIdx, wholeWeek: true)
                    }
                    pendingReorderFrom = nil
                    pendingReorderTo = nil
                }
                
                Button(L.cancelButton, role: .cancel) {
                    pendingReorderFrom = nil
                    pendingReorderTo = nil
                    familyViewModel.recalculateSchedule()
                }
            } message: {
                let dayName = reorderDayName
                
                Text(String(format: L.s("reorder_dialog_message"), dayName))
            }
            .onChange(of: showReorderConfirmation) { _, isPresented in
                if !isPresented {
                    if pendingReorderFrom != nil || pendingReorderTo != nil {
                        pendingReorderFrom = nil
                        pendingReorderTo = nil
                        familyViewModel.recalculateSchedule()
                    }
                }
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
                    familyViewModel.checkSnoozeStatus()
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
    }

    private var reorderDayName: String {
        let cal = Calendar.current
        let now = Date()
        let today = cal.startOfDay(for: now)
        let targetDate = familyViewModel.schedule?.targetDate ?? today
        let weekdayRaw = cal.component(.weekday, from: targetDate)
        let dayOfWeek = familyViewModel.selectedDayOfWeek ?? (weekdayRaw == 1 ? 7 : weekdayRaw - 1)
        return L.s("weekday_\(dayOfWeek)")
    }

    // MARK: - Helper Sections

    @ViewBuilder
    private func snoozeBanner(until: Date) -> some View {
        let currentSnoozeCount = familyViewModel.snoozeCount
        Group {
            HStack {
                Image(systemName: "zzz")
                    .foregroundStyle(colorScheme == .dark ? Color.onlineIconDark : Color.onlineIconLight)
                Text("\(L.mainSnoozeActive(timeString(until))) (\(currentSnoozeCount)/\(SnoozeConfig.maxSnoozeCount))")
                    .font(.subheadline)
                    .foregroundStyle(colorScheme == .dark ? Color.snoozeTextDark : Color.snoozeTextLight)
                Spacer()
                Button(L.cancelButton) {
                    familyViewModel.myMemberId.map { familyViewModel.cancelSnooze($0) }
                }
                .font(.caption)
                .foregroundStyle(colorScheme == .dark ? Color.onlineIconDark : Color.onlineIconLight)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(colorScheme == .dark ? Color.onlineGreenDark.opacity(0.8) : Color.onlineGreenLight)
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
                        .fill(colorScheme == .dark ? theme.errorContainer.opacity(0.4) : theme.errorContainer.opacity(0.8))
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


    @ViewBuilder
    private func errorCard(_ error: String) -> some View {
        Group {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(theme.onErrorContainer)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(error)
                            .font(.subheadline)
                            .foregroundStyle(theme.onErrorContainer)
                        
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
                                .foregroundStyle(theme.onErrorContainer)
                            }
                            .padding(.top, 4)
                        }
                    }
                    
                    Spacer()
                    Button(action: { familyViewModel.clearErrorMessage() }) {
                        Image(systemName: "xmark")
                            .foregroundStyle(theme.onErrorContainer)
                    }
                }
                
                if error == L.errorFamilyNotFound {
                    Button(L.settingsLeaveFamily) {
                        familyViewModel.leaveFamily()
                        appState.route = .familySetup
                    }
                    .font(.caption).fontWeight(.bold)
                    .foregroundStyle(theme.onErrorContainer)
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
        f.locale = LanguageManager.shared.currentLocale
        f.timeStyle = .short // Uses system 12h/24h format
        return f.string(from: date)
    }

}

// Helper for identifiable String in Sheet
struct IdentifiableString: Identifiable {
    var id: String { value }
    let value: String
}
