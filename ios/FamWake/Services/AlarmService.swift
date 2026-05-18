import Foundation
import Combine
import AVFoundation
import UserNotifications
import AudioToolbox

/// Alarm-Service – Wrapper um UNUserNotificationCenter (iOS-Äquivalent zu AlarmScheduler.kt)
@MainActor
final class AlarmService: ObservableObject {
    static let shared = AlarmService()
    private var audioPlayer: AVAudioPlayer?

    private init() {}

    func scheduleWakeUp(wakeUpTime: Date, memberId: String, memberName: String, soundUri: String?, isSnooze: Bool, onPermissionDenied: (() -> Void)? = nil) {
        let content = UNMutableNotificationContent()
        content.title = memberName
        content.body = L.ringingWakeUp(memberName)
        // Einfache Zuordnung des Custom Sounds oder Fallback
        if let soundUri = soundUri, let url = URL(string: soundUri), url.pathExtension == "caf" || url.pathExtension == "mp3" {
            content.sound = UNNotificationSound(named: UNNotificationSoundName(url.lastPathComponent))
        } else {
            content.sound = UNNotificationSound(named: UNNotificationSoundName("alarm_sound_v3.caf"))
        }
        content.categoryIdentifier = "ALARM"
        content.userInfo = ["memberId": memberId, "memberName": memberName]
        content.interruptionLevel = .timeSensitive

        let cal = Calendar.current
        var trigger: UNNotificationTrigger
        if isSnooze {
            trigger = UNTimeIntervalNotificationTrigger(timeInterval: 5 * 60, repeats: false)
        } else {
            let comps = cal.dateComponents([.year, .month, .day, .hour, .minute], from: wakeUpTime)
            trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        }

        let request = UNNotificationRequest(
            identifier: isSnooze ? "alarm_snooze_\(memberId)" : "alarm_\(memberId)",
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .notDetermined {
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                    if granted {
                        UNUserNotificationCenter.current().add(request)
                        // Trigger recalculate again or just clear error if needed, but since it's granted, the alarm is set.
                    } else {
                        DispatchQueue.main.async { onPermissionDenied?() }
                    }
                }
            } else if settings.authorizationStatus != .authorized {
                DispatchQueue.main.async { onPermissionDenied?() }
            } else {
                UNUserNotificationCenter.current().add(request)
            }
        }
    }

    func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    func cancelWakeUp(memberId: String, isSnooze: Bool = false) {
        let id = isSnooze ? "alarm_snooze_\(memberId)" : "alarm_\(memberId)"
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
    }

    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    private var systemSoundTimer: Timer?

    // MARK: - Klingeln (im Vordergrund)
    func playAlarm(soundUri: String?) {
        stopAlarm() // Ensure any existing alarm is stopped
        
        var url: URL?
        if let uri = soundUri { url = URL(string: uri) }
        if url == nil { url = Bundle.main.url(forResource: "alarm_sound_v3", withExtension: "caf") }

        if let soundUrl = url, let player = try? AVAudioPlayer(contentsOf: soundUrl) {
            try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
            try? AVAudioSession.sharedInstance().setActive(true)
            audioPlayer = player
            audioPlayer?.numberOfLoops = -1
            audioPlayer?.play()
        } else {
            // Fallback auf System Sound Dauerschleife via AudioServices
            // SystemSoundID 1005 (Alarm) oder 1033 (Ringer) oder kSystemSoundID_Vibrate (4095)
            AudioServicesPlaySystemSound(1005)
            AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
            
            systemSoundTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { _ in
                AudioServicesPlaySystemSound(1005)
                AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        }
    }

    func stopAlarm() {
        audioPlayer?.stop()
        audioPlayer = nil
        try? AVAudioSession.sharedInstance().setActive(false)
        
        systemSoundTimer?.invalidate()
        systemSoundTimer = nil
    }
}
