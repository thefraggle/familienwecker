import Foundation
import Combine
import AVFoundation
import AlarmKit
import ActivityKit
import SwiftUI
import AppIntents

struct OpenFamWakeIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Wecker beenden"
    static var openAppWhenRun: Bool = true
    
    @Parameter(title: "Member ID")
    var memberId: String
    
    @Parameter(title: "Member Name")
    var memberName: String
    
    init() {}
    
    init(memberId: String, memberName: String) {
        self.memberId = memberId
        self.memberName = memberName
    }
    
    func perform() async throws -> some IntentResult {
        // Cancel fallback push notification and system sound if active
        await AlarmService.shared.stopAlarm()
        await AlarmService.shared.cancelWakeUp(memberId: memberId)
        UserDefaults.standard.removeObject(forKey: "snooze_until")
        
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .showGreetingView, object: nil, userInfo: ["memberId": memberId, "memberName": memberName])
        }
        return .result()
    }
}

// MARK: - Custom Snooze Intent (DEAKTIVIERT – iOS 26 Beta Bug)
// AlarmKit .custom secondaryButtonBehavior killt den geplanten Snooze-Alarm
// beim Dismiss des ersten Alarms. Nutzen stattdessen natives .countdown.
// TODO: Reaktivieren sobald Apple das fixt.
// Siehe: .antigravity/todo.md

struct FamWakeAlarmMetadata: AlarmMetadata {
    var memberId: String
}

/// Alarm-Service – Wrapper um AlarmKit
@MainActor
final class AlarmService: ObservableObject {
    static let shared = AlarmService()
    private var audioPlayer: AVAudioPlayer?

    private init() {}

    private var schedulingTasks: [String: Task<Void, Never>] = [:]

    private func getUUID(for memberId: String) -> UUID {
        let key = "alarm_uuid_\(memberId)"
        if let uuidStr = UserDefaults.standard.string(forKey: key), let uuid = UUID(uuidString: uuidStr) {
            return uuid
        }
        return generateNewUUID(for: memberId)
    }

    private func generateNewUUID(for memberId: String) -> UUID {
        let key = "alarm_uuid_\(memberId)"
        let newUUID = UUID()
        UserDefaults.standard.set(newUUID.uuidString, forKey: key)
        return newUUID
    }

    func scheduleWakeUpAsync(wakeUpTime: Date, memberId: String, memberName: String, soundUri: String?, isSnooze: Bool) async throws {
        let status = try await AlarmManager.shared.requestAuthorization()
        if status != .authorized {
            throw NSError(domain: "AlarmKit", code: 1, userInfo: [NSLocalizedDescriptionKey: "Not authorized"])
        }
        
        var finalSoundNameToUse: String? = nil
        if let soundUri = soundUri {
            let lowerUri = soundUri.lowercased()
            var soundName = "alarm_sound_v3.caf"
            if soundUri == "default" || soundUri == "system" || soundUri.isEmpty {
                soundName = ""
            } else if lowerUri.hasSuffix(".caf") || lowerUri.hasSuffix(".mp3") || lowerUri.hasSuffix(".wav") {
                soundName = soundUri
            } else if let url = URL(string: soundUri) {
                soundName = url.lastPathComponent
            }
            if !soundName.isEmpty {
                if Bundle.main.path(forResource: (soundName as NSString).deletingPathExtension, ofType: (soundName as NSString).pathExtension) != nil {
                    #if targetEnvironment(simulator)
                    finalSoundNameToUse = (soundName as NSString).deletingPathExtension
                    #else
                    finalSoundNameToUse = soundName
                    #endif
                }
            }
        }
        
        
        // Natives iOS-Snooze via .countdown – iOS verwaltet den Snooze-Alarm intern.
        let alert: AlarmPresentation.Alert
        if #available(iOS 26.1, *) {
            alert = AlarmPresentation.Alert(
                title: LocalizedStringResource(stringLiteral: memberName),
                secondaryButton: AlarmButton(text: "Snooze", textColor: .white, systemImageName: "zzz"),
                secondaryButtonBehavior: .countdown
            )
        } else {
            alert = AlarmPresentation.Alert(
                title: LocalizedStringResource(stringLiteral: memberName),
                stopButton: AlarmButton(text: "Stop", textColor: .white, systemImageName: "stop.circle"),
                secondaryButton: AlarmButton(text: "Snooze", textColor: .white, systemImageName: "zzz"),
                secondaryButtonBehavior: .countdown
            )
        }
        let presentation = AlarmPresentation(alert: alert)
        
        let attributes = AlarmAttributes<FamWakeAlarmMetadata>(
            presentation: presentation,
            metadata: FamWakeAlarmMetadata(memberId: memberId),
            tintColor: .sunriseOrange500
        )
        
        let config = AlarmManager.AlarmConfiguration.alarm(
            schedule: Alarm.Schedule.fixed(wakeUpTime),
            attributes: attributes,
            stopIntent: OpenFamWakeIntent(memberId: memberId, memberName: memberName),
            sound: finalSoundNameToUse == nil ? .default : .named(finalSoundNameToUse!)
        )
        
        let oldUuid = self.getUUID(for: memberId)
        do {
            try await AlarmManager.shared.cancel(id: oldUuid)
        } catch {
            print("Cancel skipped or failed: \(error)")
        }
        
        let uuid = self.generateNewUUID(for: memberId)
        try await AlarmManager.shared.schedule(id: uuid, configuration: config)
    }

    func scheduleWakeUp(wakeUpTime: Date, memberId: String, memberName: String, soundUri: String?, isSnooze: Bool, onPermissionDenied: (() -> Void)? = nil, onSuccess: (() -> Void)? = nil) {
        schedulingTasks[memberId]?.cancel()
        
        let task = Task {
            do {
                if Task.isCancelled { return }
                try await scheduleWakeUpAsync(wakeUpTime: wakeUpTime, memberId: memberId, memberName: memberName, soundUri: soundUri, isSnooze: isSnooze)
                if Task.isCancelled { return }
                DispatchQueue.main.async { onSuccess?() }
            } catch {
                let errStr = String(describing: error)
                print("AlarmKit Error: \(errStr)")
                DispatchQueue.main.async { 
                    UserDefaults.standard.set(errStr, forKey: "last_alarm_error")
                    onPermissionDenied?() 
                }
            }
        }
        schedulingTasks[memberId] = task
    }

    func cancelAll() {
        // Not easily supported with UUIDs unless we track all UUIDs
    }

    func cancelWakeUp(memberId: String, isSnooze: Bool = false) {
        Task {
            let uuid = self.getUUID(for: memberId)
            try? await AlarmManager.shared.cancel(id: uuid)
        }
    }

    func requestPermission() {
        Task {
            try? await AlarmManager.shared.requestAuthorization()
        }
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
