import XCTest
@testable import FamWake

final class ReviewHelperTests: XCTestCase {

    private var defaults: UserDefaults!
    private var suiteName: String!

    override func setUp() {
        super.setUp()
        suiteName = "ReviewHelperTests.\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suiteName)!
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: suiteName)
        defaults = nil
        super.tearDown()
    }

    func testFirstCall_setsInstallTimeAndReturnsFalse() {
        let now = Date(timeIntervalSince1970: 1_700_000_000)
        let result = ReviewHelper.shouldShowReview(defaults: defaults, now: now, hour: 14)
        XCTAssertFalse(result)
        XCTAssertEqual(defaults.double(forKey: ReviewHelper.installTimeKey), now.timeIntervalSince1970, accuracy: 0.001)
    }

    func testBeforeThreeDays_returnsFalse() {
        let install = Date(timeIntervalSince1970: 1_700_000_000)
        defaults.set(install.timeIntervalSince1970, forKey: ReviewHelper.installTimeKey)
        let twoDaysLater = install.addingTimeInterval(2 * 24 * 60 * 60)

        XCTAssertFalse(ReviewHelper.shouldShowReview(defaults: defaults, now: twoDaysLater, hour: 14))
    }

    func testAfterThreeDays_outsideMorningHours_returnsTrue() {
        let install = Date(timeIntervalSince1970: 1_700_000_000)
        defaults.set(install.timeIntervalSince1970, forKey: ReviewHelper.installTimeKey)
        let threeDaysLater = install.addingTimeInterval(3 * 24 * 60 * 60)

        XCTAssertTrue(ReviewHelper.shouldShowReview(defaults: defaults, now: threeDaysLater, hour: 14))
    }

    func testMorningHoursBlocked_between6And9() {
        let install = Date(timeIntervalSince1970: 1_700_000_000)
        defaults.set(install.timeIntervalSince1970, forKey: ReviewHelper.installTimeKey)
        let fourDaysLater = install.addingTimeInterval(4 * 24 * 60 * 60)

        XCTAssertFalse(ReviewHelper.shouldShowReview(defaults: defaults, now: fourDaysLater, hour: 6))
        XCTAssertFalse(ReviewHelper.shouldShowReview(defaults: defaults, now: fourDaysLater, hour: 8))
        XCTAssertTrue(ReviewHelper.shouldShowReview(defaults: defaults, now: fourDaysLater, hour: 9))
        XCTAssertTrue(ReviewHelper.shouldShowReview(defaults: defaults, now: fourDaysLater, hour: 5))
    }

    func testSecondPrompt_requiresNineDaysSinceInstallAndFiveDaysSinceFirstPrompt() {
        let install = Date(timeIntervalSince1970: 1_700_000_000)
        let firstPrompt = install.addingTimeInterval(3 * 24 * 60 * 60)
        defaults.set(install.timeIntervalSince1970, forKey: ReviewHelper.installTimeKey)
        defaults.set(firstPrompt.timeIntervalSince1970, forKey: ReviewHelper.lastPromptTimeKey)

        // Day 7: 4 days after first prompt -> too early
        let day7 = install.addingTimeInterval(7 * 24 * 60 * 60)
        XCTAssertFalse(ReviewHelper.shouldShowReview(defaults: defaults, now: day7, hour: 14))

        // Day 9: 6 days after first prompt -> allowed
        let day9 = install.addingTimeInterval(9 * 24 * 60 * 60)
        XCTAssertTrue(ReviewHelper.shouldShowReview(defaults: defaults, now: day9, hour: 14))

        // After second prompt on day 9 -> no further prompt
        defaults.set(day9.timeIntervalSince1970, forKey: ReviewHelper.lastPromptTimeKey)
        let day20 = install.addingTimeInterval(20 * 24 * 60 * 60)
        XCTAssertFalse(ReviewHelper.shouldShowReview(defaults: defaults, now: day20, hour: 14))
    }
}
