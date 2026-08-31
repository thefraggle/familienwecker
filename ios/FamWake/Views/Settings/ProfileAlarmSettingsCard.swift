import SwiftUI

struct ProfileAlarmSettingsCard: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var showProfilePicker: Bool
    @Binding var showSoundPicker: Bool

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        SettingsCardContainer {
            // Header
            SettingsSectionHeader(icon: "person.fill", title: L.settingsProfileTitle)

            Text(L.settingsProfileDesc)
                .font(.subheadline)
                .foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                .padding(.bottom, 8)

            // Profile picker button
            let selectedMember = familyViewModel.members.first { $0.id == familyViewModel.myMemberId }
            let label = familyViewModel.members.isEmpty
                ? L.settingsNoMembers
                : (selectedMember?.name ?? L.settingsPleaseSelect)

            Button(action: {
                if familyViewModel.isOffline {
                    familyViewModel.errorMessage = L.errorProfileClaimOffline
                } else if !familyViewModel.members.isEmpty {
                    showProfilePicker = true
                }
            }) {
                HStack {
                    Text(label).font(.body)
                    Spacer()
                    Image(systemName: "person.fill").font(.caption)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .padding(.horizontal, 16)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(theme.outline.opacity(0.4), lineWidth: 1))
            .disabled(familyViewModel.members.isEmpty)
            .accessibilityLabel(L.s("accessibility_profile_picker"))
            .accessibilityHint(L.s("accessibility_profile_picker_hint"))

            Divider()
                .background(theme.outline.opacity(0.15))
                .padding(.vertical, 8)
            
            // Alarm Sound Picker
            Text(L.settingsAlarmTitle)
                .font(.caption).bold().foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                .padding(.top, 12)
            
            Button(action: {
                showSoundPicker = true
            }) {
                HStack {
                    Text(getSoundDisplayName(familyViewModel.alarmSoundUri)).font(.body)
                    Spacer()
                    Image(systemName: "speaker.wave.2.fill").font(.caption)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .padding(.horizontal, 16)
            }
            .foregroundStyle(theme.onSurface)
            .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(theme.outline.opacity(0.4), lineWidth: 1))
            .accessibilityLabel(L.s("accessibility_sound_picker"))
            .accessibilityHint(L.s("accessibility_sound_picker_hint"))

            if familyViewModel.tooltipsEnabled && !familyViewModel.tooltipAlarmSoundSeen {
                TooltipBubble(text: L.tooltipAlarmSound) {
                    familyViewModel.markTooltipSeen(familyViewModel.tooltipKeyAlarmSound)
                }
            }

            Divider()
                .background(theme.outline.opacity(0.15))
                .padding(.vertical, 8)

            // Sanftes Wecken Toggle
            Toggle(isOn: Binding(
                get: { familyViewModel.isGentleWakeEnabled },
                set: { familyViewModel.setGentleWakeEnabled($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L.settingsGentleWakeTitle)
                        .font(.body)
                        .foregroundStyle(theme.onSurface)
                    Text(L.settingsGentleWakeDesc)
                        .font(.caption)
                        .foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                }
            }
            .tint(Color.sunriseOrange500)
            .padding(.vertical, 4)
        }
    }

    private func getSoundDisplayName(_ uri: String?) -> String {
        guard let uri = uri else { return L.s("sound_name_standard") }
        switch uri {
        case "alarm_sound_v3.caf": return L.s("sound_name_standard")
        case "Alarm01.wav": return L.s("sound_name_gentle_chime")
        case "Alarm02.wav": return L.s("sound_name_digital_retro")
        case "Alarm03.wav": return L.s("sound_name_classic_bell")
        case "Alarm04.wav": return L.s("sound_name_bright_alert")
        case "default": return L.s("sound_name_system_default")
        default: return uri
        }
    }
}
