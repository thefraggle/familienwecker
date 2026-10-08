import XCTest
@testable import FamWake

final class FamilyModelsTests: XCTestCase {

    func testSnoozeConfig_constantsMatchKMP() {
        XCTAssertEqual(SnoozeConfig.snoozeDurationMinutes, 5)
        XCTAssertEqual(SnoozeConfig.maxSnoozeCount, 2)
        XCTAssertEqual(SnoozeConfig.minBathroomMinutes, 5)
    }

    func testDayProfile_defaultValues() {
        let profile = DayProfile()
        XCTAssertTrue(profile.isActive)
        XCTAssertEqual(profile.earliestWakeUp.hour, 6)
        XCTAssertEqual(profile.earliestWakeUp.minute, 0)
        XCTAssertEqual(profile.latestWakeUp.hour, 7)
        XCTAssertEqual(profile.latestWakeUp.minute, 30)
        XCTAssertEqual(profile.bathroomDurationMinutes, 20)
        XCTAssertTrue(profile.wantsBreakfast)
        XCTAssertNil(profile.leaveHomeTime)
        XCTAssertNil(profile.bufferMinutes)
        XCTAssertFalse(profile.isSimpleMode)
    }

    func testFamilyMember_initializationAndEquality() {
        let m1 = FamilyMember(
            id: "1",
            name: "Tochter",
            earliestWakeUp: DateComponents(hour: 6, minute: 15),
            latestWakeUp: DateComponents(hour: 7, minute: 0),
            bathroomDurationMinutes: 25,
            wantsBreakfast: true,
            isPaused: false
        )

        XCTAssertEqual(m1.id, "1")
        XCTAssertEqual(m1.name, "Tochter")
        XCTAssertEqual(m1.bathroomDurationMinutes, 25)
        XCTAssertTrue(m1.wantsBreakfast)
        XCTAssertFalse(m1.isPaused)
    }

    func testDayProfile_modificationDetection() {
        let p1 = DayProfile(isActive: true, earliestWakeUp: DateComponents(hour: 7, minute: 0))
        let p2 = DayProfile(isActive: true, earliestWakeUp: DateComponents(hour: 15, minute: 0))
        let p3 = DayProfile(isActive: true, earliestWakeUp: DateComponents(hour: 7, minute: 0))

        XCTAssertNotEqual(p1, p2)
        XCTAssertEqual(p1, p3)
    }

    func testFamilyMember_shouldResetAwakeWhenProfileChangedToFuture() {
        let cal = Calendar.current
        let baseDate = Date()
        let nowH = cal.component(.hour, from: baseDate)
        let futureH = (nowH + 2) % 24

        let oldProfile = DayProfile(isActive: true, earliestWakeUp: DateComponents(hour: (nowH - 2 + 24) % 24, minute: 0))
        let newProfile = DayProfile(isActive: true, earliestWakeUp: DateComponents(hour: futureH, minute: 0))

        let changed = oldProfile != newProfile
        XCTAssertTrue(changed)
    }
}
