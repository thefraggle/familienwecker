import SwiftUI

struct AlarmToggleSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        Group {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(familyViewModel.isAlarmEnabled ? L.mainAlarmEnabled : L.mainAlarmDisabled)
                            .font(.title3).fontWeight(.bold)
                            .foregroundStyle(theme.onPrimaryContainer)
                        let alarmDescText: String = {
                            if familyViewModel.isVacationActive, let vac = familyViewModel.vacationUntil {
                                let fVac = familyViewModel.formatVacationDate(vac)
                                return L.vacationModeAlarmPausedDesc(fVac)
                            } else if familyViewModel.isAlarmEnabled {
                                return L.mainAlarmEnabledDesc
                            } else {
                                return L.mainAlarmDisabledDesc
                            }
                        }()
                        Text(alarmDescText)
                            .font(.subheadline).foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { familyViewModel.isAlarmEnabled },
                        set: {
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            familyViewModel.setAlarmEnabled($0)
                        }
                    ))
                    .labelsHidden()
                    .disabled(familyViewModel.myMemberId == nil)
                    .tint(theme.secondary)
                    .accessibilityLabel(familyViewModel.isAlarmEnabled ? L.mainAlarmEnabled : L.mainAlarmDisabled)
                    .accessibilityIdentifier("main_alarm_toggle")
                }

                if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipSwitchSeen {
                    TooltipBubble(text: L.tooltipAlarmSwitch) {
                        familyViewModel.markTooltipSeen(familyViewModel.tooltipKeySwitch)
                    }
                }

                // Phase 1: "I'm awake" Button (nur vor Weckzeit und noch nicht wach)
                if familyViewModel.isAwakeButtonVisible {
                    Button(action: {
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        familyViewModel.myMemberId.map { familyViewModel.toggleAwakeMember($0) }
                    }) {
                        HStack {
                            Image(systemName: "sun.max")
                                .font(.body)
                            Text(L.awakeTodayDesc)
                                .font(.subheadline).fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(minHeight: 48)
                        .padding(.horizontal, 16)
                        .background(theme.primary)
                        .foregroundStyle(theme.onPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(L.awakeTodayDesc)

                    // Tooltip A
                    if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipAwakeSeen {
                        TooltipBubble(text: L.tooltipAwakeButton) {
                            familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyAwake)
                        }
                    }
                }

                // Phase 2: "Bad ist frei! 🚿"-Button (nach Weckzeit oder nach "Ich bin wach")
                if familyViewModel.isBathroomFreeButtonVisible {
                    Button(action: {
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        familyViewModel.notifyBathroomFree()
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "shower.fill")
                                .font(.body)
                            Text(familyViewModel.bathroomFreeSent ? L.bathroomFreeSuccess : L.bathroomFreeButton)
                                .font(.subheadline).fontWeight(.semibold)
                            if familyViewModel.bathroomFreeSending {
                                Spacer()
                                ProgressView()
                                    .tint(theme.onSecondaryContainer)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(minHeight: 48)
                        .padding(.horizontal, 16)
                        .background(familyViewModel.bathroomFreeSent ? theme.tertiaryContainer : theme.secondaryContainer)
                        .foregroundStyle(familyViewModel.bathroomFreeSent ? theme.onTertiaryContainer : theme.onSecondaryContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(familyViewModel.bathroomFreeSending || familyViewModel.bathroomFreeSent)
                    .animation(.easeInOut(duration: 0.2), value: familyViewModel.bathroomFreeSent)
                }
            }
            .padding()
            .famWakeCard(cornerRadius: 32, isDark: colorScheme == .dark)
            .padding(.bottom, 12)
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 24, trailing: 16))
    }
}
