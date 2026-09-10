import SwiftUI

struct AlarmToggleSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        Group {
            VStack(alignment: .leading, spacing: 12) {
                if familyViewModel.isVacationActive, let vac = familyViewModel.vacationUntil, !vac.isEmpty {
                    HStack {
                        HStack(spacing: 6) {
                            Text("🌴").font(.title3)
                            Text(L.vacationModeBannerTitle)
                                .font(.headline).fontWeight(.bold)
                                .foregroundStyle(theme.onPrimaryContainer)
                        }
                        Spacer()
                        Button(action: {
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            familyViewModel.clearVacation()
                        }) {
                            Text(L.vacationModeEndButton)
                                .font(.caption).fontWeight(.semibold)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(theme.primary.opacity(0.4), lineWidth: 1)
                                )
                        }
                        .foregroundStyle(theme.primary)
                    }

                    Divider().background(theme.outline.opacity(0.2))

                    VStack(alignment: .leading, spacing: 4) {
                        let formattedVac = familyViewModel.formatVacationDate(vac)
                        Text(L.vacationModeLastDayOff(formattedVac))
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        if let firstAlarm = familyViewModel.getFirstAlarmDateAfterVacation(vac) {
                            Text(L.vacationModeFirstAlarm(firstAlarm))
                                .font(.subheadline).fontWeight(.medium)
                                .foregroundStyle(theme.primary)
                        } else {
                            Text(L.vacationModeNoAlarmAfter)
                                .font(.subheadline)
                                .foregroundStyle(theme.onSurfaceVariant)
                        }
                    }
                } else {
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
