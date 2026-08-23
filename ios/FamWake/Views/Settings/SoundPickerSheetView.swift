import SwiftUI
import AVFoundation

struct SoundPickerSheetView: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Binding var previewPlayer: AVAudioPlayer?

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    private let sounds: [(id: String, nameKey: String, filename: String)] = [
        ("alarm_sound_v3.caf", "sound_name_standard", "alarm_sound_v3.caf"),
        ("Alarm01.wav", "sound_name_gentle_chime", "Alarm01.wav"),
        ("Alarm02.wav", "sound_name_digital_retro", "Alarm02.wav"),
        ("Alarm03.wav", "sound_name_classic_bell", "Alarm03.wav"),
        ("Alarm04.wav", "sound_name_bright_alert", "Alarm04.wav"),
        ("default", "sound_name_system_default", "")
    ]

    var body: some View {
        NavigationStack {
            List {
                ForEach(sounds, id: \.id) { sound in
                    Button(action: {
                        familyViewModel.setAlarmSoundUri(sound.id)
                        playPreview(sound.filename)
                    }) {
                        HStack {
                            Text(L.s(sound.nameKey))
                                .font(.body)
                                .foregroundStyle(theme.onSurface)
                            Spacer()
                            if (familyViewModel.alarmSoundUri ?? "alarm_sound_v3.caf") == sound.id {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(theme.tertiary)
                                    .fontWeight(.semibold)
                            }
                        }
                    }
                    .accessibilityLabel(L.s("accessibility_sound_option", L.s(sound.nameKey)))
                }
            }
            .navigationTitle(L.settingsAlarmTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: {
                        previewPlayer?.stop()
                        previewPlayer = nil
                        dismiss()
                    }) {
                        Image(systemName: "xmark")
                            .fontWeight(.semibold)
                    }
                    .buttonStyle(.borderless)
                    .foregroundStyle(theme.primary)
                }
            }
        }
    }

    private func playPreview(_ filename: String) {
        guard !filename.isEmpty else {
            previewPlayer?.stop()
            previewPlayer = nil
            AudioServicesPlaySystemSound(1005)
            return
        }
        
        let parts = filename.split(separator: ".")
        guard parts.count == 2,
              let url = Bundle.main.url(forResource: String(parts[0]), withExtension: String(parts[1])) else {
            return
        }
        
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
            previewPlayer?.stop()
            previewPlayer = try AVAudioPlayer(contentsOf: url)
            previewPlayer?.play()
        } catch {
            print("Preview error: \(error)")
        }
    }
}
