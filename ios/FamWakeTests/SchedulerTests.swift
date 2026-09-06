import XCTest
@testable import FamWake

final class SchedulerTests: XCTestCase {

    private let scheduler = Scheduler()

    private func makeMember(
        id: String = "m1",
        name: String = "Test",
        earliest: (Int, Int) = (6, 0),
        latest: (Int, Int) = (7, 30),
        duration: Int = 20,
        breakfast: Bool = true,
        leave: (Int, Int)? = nil,
        isPaused: Bool = false,
        breakfastDuration: Int? = nil
    ) -> FamilyMember {
        FamilyMember(
            id: id,
            name: name,
            earliestWakeUp: DateComponents(hour: earliest.0, minute: earliest.1),
            latestWakeUp: DateComponents(hour: latest.0, minute: latest.1),
            bathroomDurationMinutes: duration,
            wantsBreakfast: breakfast,
            leaveHomeTime: leave.map { DateComponents(hour: $0.0, minute: $0.1) },
            isPaused: isPaused,
            breakfastDurationMinutes: breakfastDuration
        )
    }

    func testEmptyMembers_returnsNoActiveSchedule() {
        let result = scheduler.calculateIdealSchedule(members: [])
        XCTAssertTrue(result.memberSchedules.isEmpty)
        XCTAssertEqual(result.scheduleMessage, .noActiveSchedule)
    }

    func testAllPaused_returnsNoActiveSchedule() {
        let members = [
            makeMember(id: "m1", isPaused: true),
            makeMember(id: "m2", isPaused: true)
        ]
        let result = scheduler.calculateIdealSchedule(members: members)
        XCTAssertEqual(result.scheduleMessage, .noActiveSchedule)
    }

    func testSingleMember_producesOptimalSchedule() {
        let member = makeMember(id: "m1")
        let result = scheduler.calculateIdealSchedule(members: [member])
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.scheduleMessage, .optimal)
        XCTAssertEqual(result.memberSchedules.count, 1)
    }

    func testTwoMembers_noConflict_producesOptimalSchedule() {
        let members = [
            makeMember(id: "m1", earliest: (6, 0), latest: (7, 0), duration: 20, breakfast: false),
            makeMember(id: "m2", earliest: (7, 0), latest: (8, 0), duration: 20, breakfast: false)
        ]
        let result = scheduler.calculateIdealSchedule(members: members, breakfastDurationMinutes: 0)
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.scheduleMessage, .optimal)
        XCTAssertEqual(result.memberSchedules.count, 2)
    }

    func testPausedMembers_areIgnored() {
        let members = [
            makeMember(id: "m1", isPaused: false),
            makeMember(id: "m2", isPaused: true)
        ]
        let result = scheduler.calculateIdealSchedule(members: members)
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.memberSchedules.count, 1)
        XCTAssertEqual(result.memberSchedules.first?.member.id, "m1")
    }

    func testMemberLimit_max6Active() {
        let members = (1...8).map { i in
            makeMember(id: "m\(i)", name: "M\(i)", earliest: (5, 0), latest: (8, 0), duration: 10, breakfast: false)
        }
        let result = scheduler.calculateIdealSchedule(members: members, breakfastDurationMinutes: 0)
        XCTAssertLessThanOrEqual(result.memberSchedules.count, 6)
    }

    func testBuffer_appliesGapBetweenMembers() {
        let members = [
            makeMember(id: "m1", earliest: (6, 0), latest: (7, 0), duration: 15, breakfast: false),
            makeMember(id: "m2", earliest: (7, 0), latest: (8, 0), duration: 15, breakfast: false)
        ]
        let result = scheduler.calculateIdealSchedule(members: members, breakfastDurationMinutes: 0, globalBufferMinutes: 5)
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.memberSchedules.count, 2)

        let m1End = result.memberSchedules[0].bathroomEnd
        let m2Start = result.memberSchedules[1].wakeUpTime

        let m1EndMin = (m1End.hour ?? 0) * 60 + (m1End.minute ?? 0)
        let m2StartMin = (m2Start.hour ?? 0) * 60 + (m2Start.minute ?? 0)
        XCTAssertGreaterThanOrEqual(m2StartMin - m1EndMin, 5, "m2 must start at least 5 minutes after m1 bathroom end")
    }

    func testIndividualBreakfastDuration_respectedInSchedule() {
        let m1 = makeMember(
            id: "m1",
            earliest: (6, 0),
            latest: (7, 30),
            duration: 20,
            breakfast: true,
            leave: (7, 30),
            breakfastDuration: 15
        )
        let result = scheduler.calculateIdealSchedule(members: [m1], breakfastDurationMinutes: 30)
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.memberSchedules[0].wakeUpTime.hour, 6)
        XCTAssertEqual(result.memberSchedules[0].wakeUpTime.minute, 55)
        XCTAssertEqual(result.memberSchedules[0].bathroomEnd.hour, 7)
        XCTAssertEqual(result.memberSchedules[0].bathroomEnd.minute, 15)
    }
}
