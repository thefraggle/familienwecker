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
        
        // Dismiss alarm permission error banner if present so screenshots look clean
        let dismissBtn = app.buttons["error_dismiss_button"]
        if dismissBtn.waitForExistence(timeout: 3.0) {
            dismissBtn.tap()
            Thread.sleep(forTimeInterval: 0.5)
        }
        
        // --- SCREENSHOT 1: Dashboard (Main Weckplan) ---
        Thread.sleep(forTimeInterval: 2.0)
        snapshot("01_MainDashboard_\(suffix)")
        
        // --- SCREENSHOT 2: Settings eines Members (Weckzeit) ---
        // Tap on the dad's member list card to open settings (ID is member_list_card_mock_dad)
        let firstCard = app.buttons["member_list_card_mock_dad"]
        if firstCard.waitForExistence(timeout: 5.0) {
            firstCard.tap()
            Thread.sleep(forTimeInterval: 1.0)
            snapshot("02_MemberSettings_\(suffix)")
            
            // Go back to Main
            let backButton = app.navigationBars.buttons.element(boundBy: 0)
            if backButton.waitForExistence(timeout: 2.0) {
                backButton.tap()
                Thread.sleep(forTimeInterval: 1.0)
            }
        }
        
        // --- SCREENSHOT 3: Settings Screen (with Share Code) ---
        // Tap on gear icon to open settings (which shows the share link & code)
        let gearButton = app.buttons["settings_button"]
        if gearButton.waitForExistence(timeout: 5.0) {
            gearButton.tap()
            Thread.sleep(forTimeInterval: 1.0)
            snapshot("03_ShareFamily_\(suffix)")
        }
        
        app.terminate()
    }
}
