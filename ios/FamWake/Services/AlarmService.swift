import Foundation
import Combine
import AVFoundation
@preconcurrency import UserNotifications
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
        
        var soundName = "alarm_sound_v3.caf"
        if let soundUri = soundUri {
            let lowerUri = soundUri.lowercased()
            if soundUri == "default" || soundUri == "system" || soundUri.isEmpty {
                soundName = ""
            } else if lowerUri.hasSuffix(".caf") || lowerUri.hasSuffix(".mp3") || lowerUri.hasSuffix(".wav") {
                soundName = soundUri
            } else if let url = URL(string: soundUri) {
                soundName = url.lastPathComponent
            }
        }
        
        content.categoryIdentifier = "ALARM"
        content.userInfo = ["memberId": memberId, "memberName": memberName]

        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let isCriticalEnabled = settings.criticalAlertSetting == .enabled
            let sound: UNNotificationSound
            if soundName.isEmpty {
                sound = isCriticalEnabled ? .defaultCritical : .default
            } else {
                sound = isCriticalEnabled ?
                    .criticalSoundNamed(UNNotificationSoundName(soundName), withAudioVolume: 1.0) :
                    .init(named: UNNotificationSoundName(soundName))
            }
            content.sound = sound
            content.interruptionLevel = isCriticalEnabled ? .critical : .timeSensitive

            let needsRequest = settings.authorizationStatus == .notDetermined ||
                               (settings.authorizationStatus == .authorized && settings.criticalAlertSetting == .disabled)
            
            if needsRequest {
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge, .criticalAlert]) { granted, _ in
                    if granted {
                        Task { @MainActor in
                            self.addNotificationChain(content: content, wakeUpTime: wakeUpTime, memberId: memberId, isSnooze: isSnooze)
                        }
                    } else {
                        DispatchQueue.main.async { onPermissionDenied?() }
                    }
                }
            } else if settings.authorizationStatus == .denied {
                DispatchQueue.main.async { onPermissionDenied?() }
            } else {
                Task { @MainActor in
                    self.addNotificationChain(content: content, wakeUpTime: wakeUpTime, memberId: memberId, isSnooze: isSnooze)
                }
            }
        }
    }

    private func addNotificationChain(content: UNMutableNotificationContent, wakeUpTime: Date, memberId: String, isSnooze: Bool) {
        let cal = Calendar.current
        let prefix = isSnooze ? "alarm_snooze_\(memberId)" : "alarm_\(memberId)"
        let maxNotifications = isSnooze ? 3 : 5
        let interval = 30.0
        
        for index in 0..<maxNotifications {
            let targetTime = wakeUpTime.addingTimeInterval(Double(index) * interval)
            let comps = cal.dateComponents([.year, .month, .day, .hour, .minute, .second], from: targetTime)
            let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
            
            let request = UNNotificationRequest(
                identifier: "\(prefix)_\(index)",
                content: content,
                trigger: trigger
            )
            UNUserNotificationCenter.current().add(request)
        }
    }

    func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
    }

    func cancelWakeUp(memberId: String, isSnooze: Bool = false) {
        let prefix = isSnooze ? "alarm_snooze_\(memberId)" : "alarm_\(memberId)"
        var ids = [String]()
        for index in 0..<5 {
            ids.append("\(prefix)_\(index)")
        }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ids)
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: ids)
    }

    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge, .criticalAlert]) { _, _ in }
    }

    private var systemSoundTimer: Timer?

    // MARK: - Klingeln (im Vordergrund)
    func playAlarm(soundUri: String?) {
        stopAlarm() // Ensure any existing alarm is stopped
        
        var url: URL?
        if let uri = soundUri, !uri.isEmpty {
            if uri == "default" || uri == "system" {
                // Keep url nil to trigger system sound fallback below
            } else if uri.hasSuffix(".caf") || uri.hasSuffix(".wav") || uri.hasSuffix(".mp3") {
                let filename = (uri as NSString).deletingPathExtension
                let ext = (uri as NSString).pathExtension
                url = Bundle.main.url(forResource: filename, withExtension: ext)
            } else if let localUrl = URL(string: uri), localUrl.scheme == "file" {
                url = localUrl
            } else {
                let filename = (uri as NSString).deletingPathExtension
                let ext = (uri as NSString).pathExtension
                url = Bundle.main.url(forResource: filename, withExtension: ext)
            }
        }
        
        if url == nil && (soundUri == nil || soundUri == "alarm_sound_v3.caf" || soundUri == "") {
            url = Bundle.main.url(forResource: "alarm_sound_v3", withExtension: "caf")
        }

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
