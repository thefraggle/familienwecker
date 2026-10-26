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

    func testLateEveningShift_calculatesWithoutHourOverflow() {
        let m1 = makeMember(
            id: "m1",
            earliest: (23, 0),
            latest: (23, 45),
            duration: 30,
            breakfast: false,
            leave: nil
        )
        let result = scheduler.calculateIdealSchedule(members: [m1], breakfastDurationMinutes: 0)
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.memberSchedules.count, 1)
        let bathEnd = result.memberSchedules[0].bathroomEnd
        XCTAssertLessThan(bathEnd.hour ?? 0, 24, "Hour must always be < 24")
    }

    func testSimpleModeMember_doesNotMaskConflictBetweenBathroomUsers() {
        let m1 = makeMember(id: "m1", name: "Anna", earliest: (6, 55), latest: (7, 0), duration: 40, breakfast: false)
        var mSimple = makeMember(id: "m2", name: "Kind", earliest: (7, 0), latest: (7, 0), duration: 0, breakfast: false)
        mSimple.isSimpleMode = true
        let m3 = makeMember(id: "m3", name: "Ben", earliest: (6, 55), latest: (7, 0), duration: 40, breakfast: false)

        let result = scheduler.calculateIdealSchedule(members: [m1, mSimple, m3], breakfastDurationMinutes: 0)
        XCTAssertFalse(result.isValid, "Conflict between bathroom users must not be masked by a SimpleMode member")
    }

    func testActiveSnooze_allowsTemporaryForwardShiftAcrossSimpleModeMember() {
        var m1 = makeMember(id: "m1", name: "Snoozer", earliest: (6, 50), latest: (6, 50), duration: 20, breakfast: false)
        m1.snoozeUntil = Date().addingTimeInterval(300)
        m1.snoozeCount = 2
        var mSimple = makeMember(id: "m2", name: "Simple", earliest: (7, 0), latest: (7, 0), duration: 0, breakfast: false)
        mSimple.isSimpleMode = true
        let m3 = makeMember(id: "m3", name: "Nachfolger", earliest: (6, 40), latest: (7, 0), duration: 20, breakfast: false)

        let result = scheduler.calculateIdealSchedule(members: [m1, mSimple, m3], breakfastDurationMinutes: 0)
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.memberSchedules[2].wakeUpTime.hour, 7)
        XCTAssertEqual(result.memberSchedules[2].wakeUpTime.minute, 10)
    }

    func testEarlyShift_customIndividualBreakfastDuration_allowsBeforeFourAm() {
        let earlyBird = makeMember(
            id: "early1",
            earliest: (2, 0),
            latest: (3, 0),
            duration: 20,
            breakfast: true,
            leave: (3, 30),
            breakfastDuration: 15
        )
        let result = scheduler.calculateIdealSchedule(members: [earlyBird], breakfastDurationMinutes: 30)
        XCTAssertTrue(result.isValid)
        XCTAssertEqual(result.memberSchedules[0].bathroomEnd.hour, 3)
        XCTAssertEqual(result.memberSchedules[0].bathroomEnd.minute, 15)
        XCTAssertEqual(result.memberSchedules[0].wakeUpTime.hour, 2)
        XCTAssertEqual(result.memberSchedules[0].wakeUpTime.minute, 55)
    }

    func testSimpleModeIntermediary_preservesBufferBetweenBathroomUsers() {
        var m1 = makeMember(id: "m1", name: "User A", earliest: (6, 0), latest: (7, 0), duration: 30, breakfast: false)
        m1.dayProfiles = ["mon": DayProfile(bufferMinutes: 10)]

        var m2 = makeMember(id: "m2", name: "Simple User", earliest: (6, 30), latest: (6, 30), duration: 0, breakfast: false)
        m2.isSimpleMode = true

        let m3 = makeMember(id: "m3", name: "User B", earliest: (6, 40), latest: (6, 40), duration: 20, breakfast: false)

        let result = scheduler.calculateIdealSchedule(members: [m1, m2, m3], globalBufferMinutes: 5, breakfastDurationMinutes: 0)
        XCTAssertTrue(result.isValid)
        let s1 = result.memberSchedules.first { $0.member.id == "m1" }!
        let s3 = result.memberSchedules.first { $0.member.id == "m3" }!
        XCTAssertEqual(s3.wakeUpTime.hour, 6)
        XCTAssertEqual(s3.wakeUpTime.minute, 40)
        let s1EndMinutes = (s1.bathroomEnd.hour ?? 0) * 60 + (s1.bathroomEnd.minute ?? 0)
        XCTAssertLessThanOrEqual(s1EndMinutes, 6 * 60 + 30)
    }
}
