import Foundation
import Combine
import AVFoundation
import UserNotifications

/// Alarm-Service – Wrapper um UNUserNotificationCenter (iOS-Äquivalent zu AlarmScheduler.kt)
@MainActor
final class AlarmService: ObservableObject {
    static let shared = AlarmService()
    private var audioPlayer: AVAudioPlayer?

    private init() {}

    func scheduleAlarms(for members: [FamilyMember], myMemberId: String?, schedule: FamilySchedule?) {
        // Alle alten Alarme canceln
        cancelAll()
        guard let myId = myMemberId,
              let member = members.first(where: { $0.id == myId }),
              let schedule,
              schedule.isValid else { return }

        guard let mySched = schedule.memberSchedules.first(where: { $0.member.id == myId }) else { return }

        let hour = mySched.wakeUpTime.hour ?? 6
        let minute = mySched.wakeUpTime.minute ?? 0

        scheduleLocalAlarm(memberId: myId, memberName: member.name, hour: hour, minute: minute, isSnooze: false)
    }

    func scheduleLocalAlarm(memberId: String, memberName: String, hour: Int, minute: Int, isSnooze: Bool) {
        let content = UNMutableNotificationContent()
        content.title = memberName
        content.body = L.ringingWakeUp(memberName)
        content.sound = UNNotificationSound(named: UNNotificationSoundName("alarm_default.caf"))
        content.categoryIdentifier = "ALARM"
        content.userInfo = ["memberId": memberId, "memberName": memberName]
        content.interruptionLevel = .timeSensitive

        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute

        let trigger: UNNotificationTrigger
        if isSnooze {
            trigger = UNTimeIntervalNotificationTrigger(timeInterval: 5 * 60, repeats: false)
        } else {
            trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        }

        let request = UNNotificationRequest(
            identifier: isSnooze ? "alarm_snooze_\(memberId)" : "alarm_\(memberId)",
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().add(request)
    }

    func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    func cancelSnooze(memberId: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(
            withIdentifiers: ["alarm_snooze_\(memberId)"]
        )
    }

    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    // MARK: - Klingeln (im Vordergrund)
    func playAlarm(soundUri: String?) {
        var url: URL?
        if let uri = soundUri { url = URL(string: uri) }
        if url == nil { url = Bundle.main.url(forResource: "alarm_default", withExtension: "caf") }

        guard let soundUrl = url else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
        audioPlayer = try? AVAudioPlayer(contentsOf: soundUrl)
        audioPlayer?.numberOfLoops = -1
        audioPlayer?.play()
    }

    func stopAlarm() {
        audioPlayer?.stop()
        audioPlayer = nil
        try? AVAudioSession.sharedInstance().setActive(false)
    }
}
