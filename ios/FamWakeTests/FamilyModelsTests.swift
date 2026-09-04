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
}
