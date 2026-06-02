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
