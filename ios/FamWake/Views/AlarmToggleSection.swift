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

                if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipSwitchSeen {
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
            .famWakeCard(cornerRadius: 32, isDark: colorScheme == .dark)
            .padding(.bottom, 12)
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 24, trailing: 16))
    }
}
