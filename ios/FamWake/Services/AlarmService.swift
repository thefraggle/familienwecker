import Foundation
import Combine
import AVFoundation
import AlarmKit
import ActivityKit
import SwiftUI
import AppIntents
import FirebaseFirestore
import UserNotifications

struct OpenFamWakeIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "alarm_stop_button"
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
        // TODO: Audit M6 – App Group Suite verwenden wenn Entitlement konfiguriert
        await AlarmService.shared.stopAlarm()
        
        // Geister-Alarm-Schutz: Wenn globaler Switch OFF → Alarm sofort canceln
        let isAlarmEnabled = UserDefaults.standard.bool(forKey: "alarm_enabled")
        if !isAlarmEnabled {
            await AlarmService.shared.cancelWakeUp(memberId: memberId)
            return .result()
        }
        
        let snoozeUntil = UserDefaults.standard.double(forKey: "snooze_until")
        let hasActiveSnooze = snoozeUntil > Date().timeIntervalSince1970
        
        if !hasActiveSnooze {
            await AlarmService.shared.cancelWakeUp(memberId: memberId)
            UserDefaults.standard.removeObject(forKey: "snooze_until")
            
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .showGreetingView, object: nil, userInfo: ["memberId": memberId, "memberName": memberName])
            }
        }
        
        return .result()
    }
}

// MARK: - Snooze Intent (REAKTIVIERT)
// Führt Weckwiederholung im Hintergrund ohne Vordergrund-Erzwingung durch.
struct SnoozeNotifyIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Snooze"
    static var openAppWhenRun: Bool = false

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
        // TODO: Audit M6 – App Group Suite verwenden wenn Entitlement konfiguriert
        await MainActor.run {
            AlarmService.shared.stopAlarm()
        }
        
        // Geister-Alarm-Schutz: Wenn globaler Switch OFF → Alarm sofort canceln, kein Snooze
        let isAlarmEnabled = UserDefaults.standard.bool(forKey: "alarm_enabled")
        if !isAlarmEnabled {
            let uuid = AlarmService.getUUID(for: memberId)
            try? await AlarmManager.shared.cancel(id: uuid)
            return .result()
        }


        // Snooze-Count prüfen (Max aus SnoozeConfig)
        let currentCount = UserDefaults.standard.integer(forKey: "snooze_count")
        if currentCount >= SnoozeConfig.maxSnoozeCount {
            // Max erreicht – AlarmKit-Alarm canceln damit Lock-Screen sich dismissed
            if let uuid = AlarmService.readUUID(for: memberId) {
                try? await AlarmManager.shared.cancel(id: uuid)
            }
            
            // Snooze-State aufräumen
            UserDefaults.standard.removeObject(forKey: "snooze_until")
            UserDefaults.standard.set(0, forKey: "snooze_count")
            
            // Lokale Notification als Hinweis auf dem Lock-Screen
            let content = UNMutableNotificationContent()
            content.title = NSLocalizedString("snooze_not_possible_title", comment: "")
            content.body = NSLocalizedString("snooze_max_reached", comment: "")
            content.sound = .default
            let request = UNNotificationRequest(identifier: "max_snooze", content: content, trigger: nil)
            try? await UNUserNotificationCenter.current().add(request)
            
            // Begrüßungsansicht öffnen (wie Stop-Button) – greift wenn User entsperrt
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .showGreetingView, object: nil, userInfo: [
                    "memberId": self.memberId,
                    "memberName": self.memberName
                ])
            }
            return .result()
        }
        let newCount = currentCount + 1

        let snoozeDate = Date().addingTimeInterval(TimeInterval(SnoozeConfig.snoozeDurationMinutes * 60))
        UserDefaults.standard.set(snoozeDate.timeIntervalSince1970, forKey: "snooze_until")
        UserDefaults.standard.set(newCount, forKey: "snooze_count")
        
        let alarmSoundUri = UserDefaults.standard.string(forKey: "alarm_sound_uri")
        try await AlarmService.scheduleWakeUpDirect(
            wakeUpTime: snoozeDate,
            memberId: memberId,
            memberName: memberName,
            soundUri: alarmSoundUri,
            isSnooze: true
        )

        // Snooze-State nach Firestore synchronisieren
        if let familyId = UserDefaults.standard.string(forKey: "family_id"),
           let myMemberId = UserDefaults.standard.string(forKey: "my_member_id") {
            let ref = Firestore.firestore()
                .collection("families").document(familyId)
                .collection("members").document(myMemberId)
            try? await ref.updateData([
                "snoozeUntil": Timestamp(date: snoozeDate),
                "snoozeCount": newCount
            ])
        }
        
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .snoozeAlarmFromNotification, object: nil, userInfo: [
                "memberId": self.memberId,
                "memberName": self.memberName
            ])
        }
        
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        
        return .result()
    }
}

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

    // MARK: - UUID Management (Keychain-basiert, überlebt Reinstall)
    
    nonisolated static func getUUID(for memberId: String) -> UUID {
        let key = "alarm_uuid_\(memberId)"
        // 1. Keychain prüfen (überlebt Reinstall)
        if let uuidStr = KeychainHelper.read(key: key), let uuid = UUID(uuidString: uuidStr) {
            return uuid
        }
        // 2. Legacy: UserDefaults prüfen und in Keychain migrieren
        if let uuidStr = UserDefaults.standard.string(forKey: key), let uuid = UUID(uuidString: uuidStr) {
            KeychainHelper.save(key: key, value: uuidStr)
            return uuid
        }
        return generateNewUUID(for: memberId)
    }

    nonisolated private static func generateNewUUID(for memberId: String) -> UUID {
        let key = "alarm_uuid_\(memberId)"
        let newUUID = UUID()
        KeychainHelper.save(key: key, value: newUUID.uuidString)
        UserDefaults.standard.set(newUUID.uuidString, forKey: key) // Backward compat
        return newUUID
    }

    private func getUUID(for memberId: String) -> UUID {
        return Self.getUUID(for: memberId)
    }

    func scheduleWakeUpAsync(wakeUpTime: Date, memberId: String, memberName: String, soundUri: String?, isSnooze: Bool) async throws {
        try await Self.scheduleWakeUpDirect(
            wakeUpTime: wakeUpTime,
            memberId: memberId,
            memberName: memberName,
            soundUri: soundUri,
            isSnooze: isSnooze
        )
    }

    static func scheduleWakeUpDirect(wakeUpTime: Date, memberId: String, memberName: String, soundUri: String?, isSnooze: Bool) async throws {
        // snooze_count wird NICHT hier zurückgesetzt – das passiert in:
        // - cancelSnooze() (User bricht Snooze ab)
        // - checkSnoozeStatus() (Snooze abgelaufen, stale cleanup)
        
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
        
        let alert: AlarmPresentation.Alert
        if #available(iOS 26.1, *) {
            alert = AlarmPresentation.Alert(
                title: LocalizedStringResource(stringLiteral: memberName),
                secondaryButton: AlarmButton(text: "Snooze", textColor: .white, systemImageName: "zzz"),
                secondaryButtonBehavior: .custom
            )
        } else {
            alert = AlarmPresentation.Alert(
                title: LocalizedStringResource(stringLiteral: memberName),
                stopButton: AlarmButton(text: "Stop", textColor: .white, systemImageName: "stop.circle"),
                secondaryButton: AlarmButton(text: "Snooze", textColor: .white, systemImageName: "zzz"),
                secondaryButtonBehavior: .custom
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
            secondaryIntent: SnoozeNotifyIntent(memberId: memberId, memberName: memberName),
            sound: finalSoundNameToUse.map { .named($0) } ?? .default
        )
        
        let oldUuid = self.getUUID(for: memberId)
        try? await AlarmManager.shared.cancel(id: oldUuid)
        
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
                let nsError = error as NSError
                // Nur bei echtem Berechtigungsfehler den Banner zeigen,
                // nicht bei jedem beliebigen AlarmKit-Fehler
                let isPermissionError = nsError.domain == "AlarmKit" && nsError.code == 1
                DispatchQueue.main.async { 
                    UserDefaults.standard.set(errStr, forKey: "last_alarm_error")
                    if isPermissionError {
                        onPermissionDenied?()
                    }
                }
            }
        }
        schedulingTasks[memberId] = task
    }

    func cancelAll() async {
        // 1. Aus Keychain lesen (überlebt Reinstall!)
        for uuid in KeychainHelper.readAllAlarmUUIDs() {
            try? await AlarmManager.shared.cancel(id: uuid)
        }
        // 2. Legacy: Auch UserDefaults prüfen (Keys NICHT löschen – werden bei
        //    generateNewUUID() überschrieben; Löschen verursacht Inkonsistenz mit Keychain)
        for key in UserDefaults.standard.dictionaryRepresentation().keys where key.hasPrefix("alarm_uuid_") {
            if let uuidStr = UserDefaults.standard.string(forKey: key), let uuid = UUID(uuidString: uuidStr) {
                try? await AlarmManager.shared.cancel(id: uuid)
            }
        }
    }

    /// Liest die UUID ohne zu generieren – gibt nil zurück wenn nicht vorhanden.
    /// Für Cancel-Pfade: verhindert, dass eine nie geplante UUID gecancelt wird.
    nonisolated static func readUUID(for memberId: String) -> UUID? {
        let key = "alarm_uuid_\(memberId)"
        if let uuidStr = KeychainHelper.read(key: key), let uuid = UUID(uuidString: uuidStr) {
            return uuid
        }
        if let uuidStr = UserDefaults.standard.string(forKey: key), let uuid = UUID(uuidString: uuidStr) {
            KeychainHelper.save(key: key, value: uuidStr)
            return uuid
        }
        return nil
    }

    func cancelWakeUp(memberId: String) {
        Task {
            if let uuid = Self.readUUID(for: memberId) {
                try? await AlarmManager.shared.cancel(id: uuid)
            }
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
