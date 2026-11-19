import XCTest
@testable import FamWake

final class SnoozeControllerTests: XCTestCase {

    private var suiteName: String!
    private var defaults: UserDefaults!
    private var currentDate: Date!

    override func setUp() {
        super.setUp()
        suiteName = "SnoozeControllerTests.\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suiteName)!
        currentDate = Date(timeIntervalSince1970: 1_760_000_000) // 2025-10-09
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: suiteName)
        defaults = nil
        super.tearDown()
    }

    private func makeSUT() -> SnoozeController {
        SnoozeController(userDefaults: defaults, now: { self.currentDate })
    }

    func testTriggerSnooze_enforcesMaxCount() {
        let sut = makeSUT()

        XCTAssertTrue(sut.canSnooze)
        let first = sut.triggerSnooze()
        XCTAssertNotNil(first)
        XCTAssertEqual(sut.snoozeCount, 1)

        let second = sut.triggerSnooze()
        XCTAssertNotNil(second)
        XCTAssertEqual(sut.snoozeCount, 2)
        XCTAssertFalse(sut.canSnooze)

        let third = sut.triggerSnooze()
        XCTAssertNil(third)
        XCTAssertEqual(sut.snoozeCount, 2)
    }

    func testCurrentStatus_keepsCountWhenRecentlyExpired_andClearsWhenStale() {
        let sut = makeSUT()
        _ = sut.triggerSnooze(durationMinutes: 5)

        // 10 Minuten später (5 Min abgelaufen < 30 Min Stale-Schwelle): Count bleibt erhalten
        currentDate = currentDate.addingTimeInterval(10 * 60)
        let recent = sut.currentStatus()
        XCTAssertNil(recent.until)
        XCTAssertEqual(recent.count, 1)

        // 40 Minuten später (35 Min abgelaufen > 30 Min Stale-Schwelle): Alles bereinigt
        currentDate = currentDate.addingTimeInterval(30 * 60)
        let stale = sut.currentStatus()
        XCTAssertNil(stale.until)
        XCTAssertEqual(stale.count, 0)
    }

    func testNewDay_resetsCountAndUntil() {
        let sut = makeSUT()
        _ = sut.triggerSnooze()
        _ = sut.triggerSnooze()
        XCTAssertFalse(sut.canSnooze)

        // Nächster Kalendertag (+24h)
        currentDate = currentDate.addingTimeInterval(24 * 60 * 60)
        XCTAssertTrue(sut.canSnooze)
        XCTAssertEqual(sut.snoozeCount, 0)
        XCTAssertNil(sut.activeSnoozeUntil)
    }

    func testClearIfNotActive_preservesActiveSnooze() {
        let sut = makeSUT()
        _ = sut.triggerSnooze(durationMinutes: 5)

        XCTAssertFalse(sut.clearIfNotActive())
        XCTAssertEqual(sut.snoozeCount, 1)
        XCTAssertNotNil(sut.activeSnoozeUntil)

        // Nach Ablauf wird bereinigt
        currentDate = currentDate.addingTimeInterval(6 * 60)
        XCTAssertTrue(sut.clearIfNotActive())
        XCTAssertEqual(sut.snoozeCount, 0)
        XCTAssertNil(sut.snoozeUntil)
    }
}
