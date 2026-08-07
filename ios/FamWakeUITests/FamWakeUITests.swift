import XCTest

@MainActor
class FamWakeUITests: XCTestCase {
    
    override func setUpWithError() throws {
        continueAfterFailure = false
    }
    
    func testCaptureScreenshotsLight() throws {
        XCUIDevice.shared.appearance = .light
        try captureScreenshots(suffix: "Light")
    }
    
    func testCaptureScreenshotsDark() throws {
        XCUIDevice.shared.appearance = .dark
        try captureScreenshots(suffix: "Dark")
    }
    
    private func captureScreenshots(suffix: String) throws {
        let app = XCUIApplication()
        
        // Pass screenshotMode argument to trigger mock data loading
        app.launchArguments = ["-screenshotMode"]
        
        // Initialize fastlane snapshot
        setupSnapshot(app)
        app.launch()
        
        // Wait for dashboard to render
        Thread.sleep(forTimeInterval: 2.0)
        
        // --- SCREENSHOT 1: Dashboard (Wecker AUS - Mond) ---
        // Tap on the main alarm toggle to switch it off
        let alarmToggle = app.switches["main_alarm_toggle"]
        if alarmToggle.waitForExistence(timeout: 5.0) {
            alarmToggle.tap()
            Thread.sleep(forTimeInterval: 1.5)
            snapshot("01_MainDashboard_Empty_\(suffix)")
            
            // Switch it back on for the next screenshots
            alarmToggle.tap()
            Thread.sleep(forTimeInterval: 1.5)
        }
        
        // --- SCREENSHOT 2: Dashboard (Main Weckplan aktiv) ---
        snapshot("02_MainDashboard_Active_\(suffix)")
        
        // --- SCREENSHOT 3: Settings eines Members (Weckzeit) ---
        // Tap on the dad's member list card to open settings (ID is member_list_card_mock_dad)
        let firstCard = app.buttons["member_list_card_mock_dad"]
        if firstCard.waitForExistence(timeout: 5.0) {
            firstCard.tap()
            Thread.sleep(forTimeInterval: 1.5)
            snapshot("03_MemberSettings_\(suffix)")
            
            // Go back to Main
            let backButton = app.navigationBars.buttons.element(boundBy: 0)
            if backButton.waitForExistence(timeout: 2.0) {
                backButton.tap()
                Thread.sleep(forTimeInterval: 1.5)
            }
        }
        
        // --- SCREENSHOT 4: Settings Screen (with Share Code) ---
        // Tap on gear icon to open settings (which shows the share link & code)
        let gearButton = app.buttons["settings_button"]
        if gearButton.waitForExistence(timeout: 5.0) {
            gearButton.tap()
            Thread.sleep(forTimeInterval: 1.5)
            snapshot("04_ShareFamily_\(suffix)")
        }
        
        app.terminate()
    }
}
