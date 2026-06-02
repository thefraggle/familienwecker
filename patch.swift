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
    
    @Parameter(title: "Member Name")
    var memberName: String
    
    init() {}
    
    init(memberName: String) {
        self.memberName = memberName
    }
    
    func perform() async throws -> some IntentResult {
        let name = memberName
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: .showRingingView,
                object: nil,
                userInfo: ["memberId": "unknown", "memberName": name]
            )
        }
        return .result()
    }
}

struct SnoozeFamWakeIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Snooze"
    static var openAppWhenRun: Bool = false
    
    @Parameter(title: "Member Name")
    var memberName: String
    
    init() {}
    
    init(memberName: String) {
        self.memberName = memberName
    }
    
    func perform() async throws -> some IntentResult {
        let name = memberName
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: NSNotification.Name("snoozeAlarmFromNotification"),
                object: nil,
                userInfo: ["memberId": "unknown", "memberName": name, "snoozeTime": Date().addingTimeInterval(300)]
            )
        }
        return .result()
    }
}
