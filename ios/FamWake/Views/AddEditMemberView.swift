import SwiftUI

// MARK: - DayProfile Validation
func validateDayProfile(_ profile: DayProfile) -> [String] {
    var errors: [String] = []
    if profile.isSimpleMode { return errors }
    if profile.latestWakeUp < profile.earliestWakeUp {
        errors.append(L.validationLatestBeforeEarliest)
    }
    let leaveH = profile.leaveHomeTime?.hour ?? 8
    let leaveM = profile.leaveHomeTime?.minute ?? 0
    let latestH = profile.latestWakeUp.hour ?? 7
    let latestM = profile.latestWakeUp.minute ?? 30
    let leaveTotal = leaveH * 60 + leaveM
    let bathroomEndTotal = latestH * 60 + latestM + profile.bathroomDurationMinutes
    if leaveTotal < bathroomEndTotal {
        errors.append(L.validationLeaveTooEarly)
    }
    return errors
}

// MARK: - AddEditMemberView
struct AddEditMemberView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    var memberId: String?
    var onDone: () -> Void

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    @State private var name: String = ""
    @State private var dayProfiles: [Int: DayProfile] = DayProfile.defaults()
    @State private var selectedDay: Int = 1  // 1=Mo
    @State private var showCopyDialog = false
    @State private var copyTargets: Set<Int> = []
    @State private var showDiscardAlert = false

    // Initiale Werte zum Vergleich
    @State private var initialName: String = ""
    @State private var initialProfiles: [Int: DayProfile] = [:]

    @State private var initialized = false

    var hasChanges: Bool { name != initialName || dayProfiles != initialProfiles }
    var hasAnyError: Bool { dayProfiles.values.filter { $0.isActive }.contains { !validateDayProfile($0).isEmpty } }

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    colors: colorScheme == .dark
                        ? [theme.surface, theme.background]
                        : [theme.primaryContainer.opacity(0.5), theme.background],
                    startPoint: .top, endPoint: .bottom
                ).ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 16) {
                        // Fehlerkarte
                        if let err = familyViewModel.errorMessage {
                            Text("⚠️ \(err)")
                                .foregroundStyle(theme.error)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(theme.errorContainer.opacity(0.3))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                                .onTapGesture { familyViewModel.clearError() }
                        }

                        // Name
                        TextField(L.addMemberNameLabel, text: $name)
                            .textFieldStyle(.roundedBorder)

                        // Wochentags-Chips
                        Text(L.addMemberDayProfilesTitle)
                            .font(.headline).fontWeight(.bold)
                            .foregroundStyle(theme.onSurface)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        HStack(spacing: 4) {
                            ForEach(1...7, id: \.self) { day in
                                weekdayChip(day)
                            }
                        }

                        // Tooltip G
                        if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipWeekdaysSeen {
                            TooltipBubble(text: L.tooltipWeekdays) {
                                familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyWeekdays)
                            }
                        }

                        // Copy Button
                        Button(action: { showCopyDialog = true }) {
                            HStack {
                                Image(systemName: "doc.on.doc")
                                Text(L.addMemberCopyToDays(L.weekday(selectedDay)))
                            }
                            .font(.subheadline).fontWeight(.semibold)
                            .foregroundStyle(theme.primary)
                            .padding(.vertical, 4)
                        }
                        
                        // DayProfile für selectedDay
                        let profile = dayProfiles[selectedDay] ?? DayProfile()
                        DayProfileCard(
                            dayLabel: L.weekday(selectedDay),
                            profile: profile,
                            globalBufferMinutes: familyViewModel.globalBufferMinutes,
                            showTooltipWakeWindow: familyViewModel.tooltipsEnabled && !familyViewModel.tooltipWakeWindowSeen,
                            onDismissWakeWindow: { familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyWakeWindow) },
                            showTooltipBathroom: familyViewModel.tooltipsEnabled && !familyViewModel.tooltipBathroomSeen,
                            onDismissBathroom: { familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyBathroom) },
                            showTooltipBuffer: familyViewModel.tooltipsEnabled && !familyViewModel.tooltipBufferSeen,
                            onDismissBuffer: { familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyBuffer) }
                        ) { updated in
                            dayProfiles[selectedDay] = updated
                        }

                        Spacer(minLength: 80) // Platz für Bottom-Button
                    }
                    .padding(16)
                }

                // Bottom Save Button
                VStack {
                    Spacer()
                    Button(L.addMemberSubmit) {
                        saveMember()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(theme.tertiary)
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: 56)
                    .clipShape(Capsule())
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || hasAnyError)
                    .padding(16)
                    .background(
                        LinearGradient(
                            colors: colorScheme == .dark
                                ? [theme.surface, theme.background]
                                : [theme.primaryContainer.opacity(0.5), theme.background],
                            startPoint: .top, endPoint: .bottom
                        )
                    )
                }
            }
            .navigationTitle(memberId == nil ? L.addMemberTitleAdd : L.addMemberTitleEdit)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(L.cancelButton) {
                        if hasChanges { showDiscardAlert = true } else { onDone() }
                    }
                    .buttonStyle(.borderless)
                    .foregroundStyle(theme.tertiary)
                }
            }
            .alert(L.unsavedChangesTitle, isPresented: $showDiscardAlert) {
                Button(L.unsavedChangesDiscard, role: .destructive) { onDone() }
                Button(L.unsavedChangesKeep, role: .cancel) {}
            } message: {
                Text(L.unsavedChangesMessage)
            }
            .sheet(isPresented: $showCopyDialog) {
                CopyToOtherDaysSheet(
                    sourceDay: selectedDay,
                    onCopy: { targets in
                        let sourceProfile = dayProfiles[selectedDay] ?? DayProfile()
                        targets.forEach { day in
                            dayProfiles[day] = sourceProfile
                        }
                    }
                )
            }
            .task {
                if !initialized {
                    initializeData()
                    initialized = true
                }
            }
        }
    }

    @ViewBuilder
    private func weekdayChip(_ day: Int) -> some View {
        let profile = dayProfiles[day] ?? DayProfile()
        let isSelected = selectedDay == day
        let isActive = profile.isActive
        let hasError = isActive && !validateDayProfile(profile).isEmpty

        Button(action: { selectedDay = day }) {
            Text(L.weekdayShort(day))
                .font(.caption).fontWeight(.semibold)
                .foregroundStyle(chipTextColor(isSelected: isSelected, isActive: isActive, hasError: hasError))
                .frame(maxWidth: .infinity)
                .frame(minHeight: 36)
                .background(chipBgColor(isSelected: isSelected, isActive: isActive, hasError: hasError))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(hasError ? theme.error : Color.clear, lineWidth: 1)
                )
        }
    }

    private func chipBgColor(isSelected: Bool, isActive: Bool, hasError: Bool) -> Color {
        if hasError { return isSelected ? theme.error.opacity(0.2) : theme.error.opacity(0.1) }
        if isSelected && isActive { return theme.tertiary }
        if isSelected { return theme.surfaceVariant }
        if isActive { return theme.surfaceVariant.opacity(0.6) }
        return theme.surfaceVariant.opacity(0.3)
    }

    private func chipTextColor(isSelected: Bool, isActive: Bool, hasError: Bool) -> Color {
        if hasError { return theme.error }
        if isSelected && isActive { return theme.onTertiary }
        if isActive { return theme.onSurface }
        return theme.onSurfaceVariant.opacity(0.5)
    }

    private func currentDayOfWeek() -> Int {
        let weekday = Calendar.current.component(.weekday, from: Date())
        // Convert to: 1 = Monday, ..., 7 = Sunday
        let mapped = (weekday + 5) % 7 + 1
        return mapped
    }

    private func initializeData() {
        let allMembers = familyViewModel.members
        if let mid = memberId, let member = allMembers.first(where: { $0.id == mid }) {
            name = member.name
            let profiles = member.dayProfiles ?? [:]
            dayProfiles = profiles.isEmpty ? DayProfile.defaults() : profiles
        } else {
            name = ""
            dayProfiles = DayProfile.defaults()
        }
        initialName = name
        initialProfiles = dayProfiles

        let today = currentDayOfWeek()
        var targetDay = today
        if !(dayProfiles[today]?.isActive ?? false) {
            for offset in 1..<7 {
                let checkDay = (today - 1 + offset) % 7 + 1
                if dayProfiles[checkDay]?.isActive ?? false {
                    targetDay = checkDay
                    break
                }
            }
        }
        selectedDay = targetDay
    }

    private func saveMember() {
        let refProfile = dayProfiles[1] ?? dayProfiles.values.first ?? DayProfile()
        
        var memberToSave: FamilyMember
        if let mid = memberId, let existing = familyViewModel.members.first(where: { $0.id == mid }) {
            memberToSave = existing
            memberToSave.name = name.trimmingCharacters(in: .whitespaces)
            memberToSave.earliestWakeUp = refProfile.earliestWakeUp
            memberToSave.latestWakeUp = refProfile.latestWakeUp
            memberToSave.bathroomDurationMinutes = refProfile.bathroomDurationMinutes
            memberToSave.wantsBreakfast = refProfile.wantsBreakfast
            memberToSave.leaveHomeTime = refProfile.leaveHomeTime
            memberToSave.isSimpleMode = refProfile.isSimpleMode
            memberToSave.dayProfiles = dayProfiles
        } else {
            memberToSave = FamilyMember(
                id: UUID().uuidString,
                name: name.trimmingCharacters(in: .whitespaces),
                earliestWakeUp: refProfile.earliestWakeUp,
                latestWakeUp: refProfile.latestWakeUp,
                bathroomDurationMinutes: refProfile.bathroomDurationMinutes,
                wantsBreakfast: refProfile.wantsBreakfast,
                leaveHomeTime: refProfile.leaveHomeTime,
                dayProfiles: dayProfiles,
                isSimpleMode: refProfile.isSimpleMode
            )
        }
        
        familyViewModel.addOrUpdateMember(memberToSave)
        onDone()
    }
}

// MARK: - DayProfileCard
private struct DayProfileCard: View {
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    let dayLabel: String
    var profile: DayProfile
    var globalBufferMinutes: Int
    var showTooltipWakeWindow: Bool = false
    var onDismissWakeWindow: () -> Void = {}
    var showTooltipBathroom: Bool = false
    var onDismissBathroom: () -> Void = {}
    var showTooltipBuffer: Bool = false
    var onDismissBuffer: () -> Void = {}
    var onChange: (DayProfile) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Text(dayLabel)
                    .font(.headline).fontWeight(.bold)
                Spacer()
                HStack(spacing: 6) {
                    Text(L.addMemberDayActive)
                        .font(.caption).foregroundStyle(.secondary)
                    Toggle("", isOn: Binding(
                        get: { profile.isActive },
                        set: { onChange(profile.withActive($0)) }
                    ))
                    .labelsHidden()
                }
            }

            if profile.isActive {
                Divider()

                // Einfacher Modus Toggle
                Toggle(isOn: Binding(
                    get: { profile.isSimpleMode },
                    set: { onChange(profile.withSimpleMode($0)) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L.simpleModeTitle)
                            .font(.body)
                        Text(L.simpleModeDesc)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .tint(theme.tertiary)

                Divider()

                if profile.isSimpleMode {
                    // Nur die Weckzeit (latestWakeUp) anzeigen
                    DatePickerRow(
                        label: L.addMemberLatestWake,
                        time: Binding(
                            get: { profile.latestWakeUp.asTime ?? Date() },
                            set: { onChange(profile.withLatest(.from(hour: Calendar.current.component(.hour, from: $0), minute: Calendar.current.component(.minute, from: $0)))) }
                        ),
                        theme: theme
                    )
                } else {
                    // Früheste Weckzeit
                    DatePickerRow(
                        label: L.addMemberEarliestWake,
                        time: Binding(
                            get: { profile.earliestWakeUp.asTime ?? Date() },
                            set: { onChange(profile.withEarliest(.from(hour: Calendar.current.component(.hour, from: $0), minute: Calendar.current.component(.minute, from: $0)))) }
                        ),
                        theme: theme
                    )

                    // Späteste Weckzeit
                    let latestError = profile.latestWakeUp < profile.earliestWakeUp
                    VStack(alignment: .leading, spacing: 4) {
                        DatePickerRow(
                            label: L.addMemberLatestWake,
                            time: Binding(
                                get: { profile.latestWakeUp.asTime ?? Date() },
                                set: { onChange(profile.withLatest(.from(hour: Calendar.current.component(.hour, from: $0), minute: Calendar.current.component(.minute, from: $0)))) }
                            ),
                            isError: latestError,
                            theme: theme
                        )
                        if latestError {
                            Text(L.validationLatestBeforeEarliest)
                                .font(.caption).foregroundStyle(.red)
                        }
                    }

                    // Tooltip C
                    if showTooltipWakeWindow {
                        TooltipBubble(text: L.tooltipWakeWindow, onDismiss: onDismissWakeWindow)
                    }

                    // Baddauer Stepper
                    HStack {
                        Text(L.addMemberBathroomDuration)
                            .font(.body)
                        Spacer()
                        HStack(spacing: 12) {
                            Button {
                                if profile.bathroomDurationMinutes > 5 {
                                    onChange(profile.withBathroom(profile.bathroomDurationMinutes - 5))
                                }
                            } label: {
                                Image(systemName: "minus.circle.fill").font(.title2).foregroundStyle(theme.tertiary)
                            }
                            Text("\(profile.bathroomDurationMinutes) min")
                                .font(.headline).fontWeight(.semibold)
                                .frame(minWidth: 64)
                                .multilineTextAlignment(.center)
                            Button {
                                if profile.bathroomDurationMinutes < 120 {
                                    onChange(profile.withBathroom(profile.bathroomDurationMinutes + 5))
                                }
                            } label: {
                                Image(systemName: "plus.circle.fill").font(.title2).foregroundStyle(theme.tertiary)
                            }
                        }
                    }

                    // Tooltip D
                    if showTooltipBathroom {
                        TooltipBubble(text: L.tooltipBathroom, onDismiss: onDismissBathroom)
                    }

                    // Puffer nach Bad (Individueller Override)
                    HStack {
                        Text(L.bufferAfterBath)
                            .font(.body)
                        Spacer()
                        HStack(spacing: 12) {
                            let effectiveValue = profile.bufferMinutes ?? globalBufferMinutes
                            Button {
                                let newVal = max(0, effectiveValue - 5)
                                if newVal == globalBufferMinutes {
                                    onChange(profile.withBuffer(nil))
                                } else {
                                    onChange(profile.withBuffer(newVal))
                                }
                            } label: {
                                Image(systemName: "minus.circle.fill")
                                    .font(.title2)
                                    .foregroundStyle(effectiveValue > 0 ? theme.tertiary : Color.gray.opacity(0.5))
                            }
                            .disabled(effectiveValue <= 0)
                            
                            Text("\(effectiveValue) min")
                                .font(.headline)
                                .fontWeight(profile.bufferMinutes != nil ? .bold : .regular)
                                .italic(profile.bufferMinutes == nil)
                                .frame(minWidth: 64)
                                .multilineTextAlignment(.center)
                                
                            Button {
                                let newVal = min(15, effectiveValue + 5)
                                if newVal == globalBufferMinutes {
                                    onChange(profile.withBuffer(nil))
                                } else {
                                    onChange(profile.withBuffer(newVal))
                                }
                            } label: {
                                Image(systemName: "plus.circle.fill")
                                    .font(.title2)
                                    .foregroundStyle(effectiveValue < 15 ? theme.tertiary : Color.gray.opacity(0.5))
                            }
                            .disabled(effectiveValue >= 15)
                        }
                    }
                    
                    // Tooltip E (Buffer)
                    if showTooltipBuffer {
                        TooltipBubble(text: L.tooltipBuffer, onDismiss: onDismissBuffer)
                    }

                    // Frühstück
                    Toggle(L.addMemberWantsBreakfast, isOn: Binding(
                        get: { profile.wantsBreakfast },
                        set: { onChange(profile.withBreakfast($0)) }
                    ))

                    // Abfahrtszeit
                    let leaveTotal = (profile.leaveHomeTime?.hour ?? 8) * 60 + (profile.leaveHomeTime?.minute ?? 0)
                    let bathroomEndTotal = (profile.latestWakeUp.hour ?? 7) * 60 + (profile.latestWakeUp.minute ?? 30) + profile.bathroomDurationMinutes
                    let leaveError = leaveTotal < bathroomEndTotal
                    VStack(alignment: .leading, spacing: 4) {
                        DatePickerRow(
                            label: L.addMemberLeaveHome,
                            time: Binding(
                                get: { profile.leaveHomeTime?.asTime ?? Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date())! },
                                set: { onChange(profile.withLeave(.from(hour: Calendar.current.component(.hour, from: $0), minute: Calendar.current.component(.minute, from: $0)))) }
                            ),
                            isError: leaveError,
                            theme: theme
                        )
                        if leaveError {
                            Text(L.validationLeaveTooEarly).font(.caption).foregroundStyle(.red)
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(Color(.secondarySystemBackground))
                .shadow(color: .black.opacity(0.07), radius: 8, x: 0, y: 4)
        )
    }
}

// MARK: - DatePickerRow
private struct DatePickerRow: View {
    let label: String
    @Binding var time: Date
    var isError: Bool = false
    var theme: FamWakeTheme

    var body: some View {
        HStack {
            Text(label)
                .font(.body)
                .foregroundStyle(isError ? .red : Color(.label))
            Spacer()
            DatePicker("", selection: $time, displayedComponents: .hourAndMinute)
                .labelsHidden()
                .accentColor(isError ? .red : theme.tertiary)
        }
    }
}

// MARK: - CopyToOtherDaysSheet
struct CopyToOtherDaysSheet: View {
    let sourceDay: Int
    var onCopy: (Set<Int>) -> Void
    @State private var selected: Set<Int> = []
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        NavigationStack {
            List {
                ForEach(Array(1...7), id: \.self) { day in
                    if day != sourceDay {
                        HStack {
                            Text(L.weekday(day))
                            Spacer()
                            if selected.contains(day) {
                                Image(systemName: "checkmark").foregroundStyle(theme.tertiary)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            if selected.contains(day) { selected.remove(day) }
                            else { selected.insert(day) }
                        }
                    }
                }
            }
            .navigationTitle(L.addMemberCopyDialogTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L.addMemberCopyApply) {
                        onCopy(selected)
                        dismiss()
                    }
                    .disabled(selected.isEmpty)
                    .buttonStyle(.borderless)
                    .foregroundStyle(selected.isEmpty ? Color.gray : theme.tertiary)
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button(L.cancelButton) { dismiss() }
                    .buttonStyle(.borderless)
                    .foregroundStyle(theme.tertiary)
                }
            }
        }
    }
}

// MARK: - DayProfile Helpers
extension DayProfile {
    func withActive(_ v: Bool) -> DayProfile { var c = self; c.isActive = v; return c }
    func withEarliest(_ v: DateComponents) -> DayProfile { var c = self; c.earliestWakeUp = v; return c }
    func withLatest(_ v: DateComponents) -> DayProfile {
        var c = self
        c.latestWakeUp = v
        if c.isSimpleMode {
            c.earliestWakeUp = v
        }
        return c
    }
    func withBathroom(_ v: Int) -> DayProfile { var c = self; c.bathroomDurationMinutes = v; return c }
    func withBreakfast(_ v: Bool) -> DayProfile { var c = self; c.wantsBreakfast = v; return c }
    func withLeave(_ v: DateComponents?) -> DayProfile { var c = self; c.leaveHomeTime = v; return c }
    func withBuffer(_ v: Int?) -> DayProfile { var c = self; c.bufferMinutes = v; return c }
    func withSimpleMode(_ v: Bool) -> DayProfile {
        var c = self
        c.isSimpleMode = v
        if v {
            c.bathroomDurationMinutes = 0
            c.wantsBreakfast = false
            c.earliestWakeUp = c.latestWakeUp
            c.bufferMinutes = nil
            c.leaveHomeTime = nil
        }
        return c
    }
}

extension DateComponents {
    var asTime: Date? {
        var comps = DateComponents()
        comps.hour = hour
        comps.minute = minute
        return Calendar.current.date(from: comps)
    }
}
