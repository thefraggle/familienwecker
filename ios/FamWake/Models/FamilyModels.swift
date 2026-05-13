import Foundation

// MARK: - FamilyMember
struct FamilyMember: Identifiable, Codable, Equatable {
    var id: String
    var name: String
    var earliestWakeUp: DateComponents  // Stunde + Minute
    var latestWakeUp: DateComponents
    var bathroomDurationMinutes: Int
    var wantsBreakfast: Bool
    var leaveHomeTime: DateComponents?
    var isPaused: Bool
    var claimedByUserId: String?
    var claimedByUserName: String?
    var createdAt: Double?
    var dayProfiles: [Int: DayProfile]  // 1=Mo...7=So
    var order: Int

    init(
        id: String = UUID().uuidString,
        name: String,
        earliestWakeUp: DateComponents = DateComponents(hour: 6, minute: 0),
        latestWakeUp: DateComponents = DateComponents(hour: 7, minute: 30),
        bathroomDurationMinutes: Int = 20,
        wantsBreakfast: Bool = true,
        leaveHomeTime: DateComponents? = nil,
        isPaused: Bool = false,
        claimedByUserId: String? = nil,
        claimedByUserName: String? = nil,
        createdAt: Double? = nil,
        dayProfiles: [Int: DayProfile] = DayProfile.defaults(),
        order: Int = 0
    ) {
        self.id = id
        self.name = name
        self.earliestWakeUp = earliestWakeUp
        self.latestWakeUp = latestWakeUp
        self.bathroomDurationMinutes = bathroomDurationMinutes
        self.wantsBreakfast = wantsBreakfast
        self.leaveHomeTime = leaveHomeTime
        self.isPaused = isPaused
        self.claimedByUserId = claimedByUserId
        self.claimedByUserName = claimedByUserName
        self.createdAt = createdAt
        self.dayProfiles = dayProfiles
        self.order = order
    }
}

// MARK: - DayProfile
struct DayProfile: Codable, Equatable {
    var isActive: Bool
    var earliestWakeUp: DateComponents
    var latestWakeUp: DateComponents
    var bathroomDurationMinutes: Int
    var wantsBreakfast: Bool
    var leaveHomeTime: DateComponents?

    init(
        isActive: Bool = true,
        earliestWakeUp: DateComponents = DateComponents(hour: 6, minute: 0),
        latestWakeUp: DateComponents = DateComponents(hour: 7, minute: 30),
        bathroomDurationMinutes: Int = 20,
        wantsBreakfast: Bool = true,
        leaveHomeTime: DateComponents? = nil
    ) {
        self.isActive = isActive
        self.earliestWakeUp = earliestWakeUp
        self.latestWakeUp = latestWakeUp
        self.bathroomDurationMinutes = bathroomDurationMinutes
        self.wantsBreakfast = wantsBreakfast
        self.leaveHomeTime = leaveHomeTime
    }

    /// Mo–Fr aktiv, Sa–So inaktiv (Standard)
    static func defaults() -> [Int: DayProfile] {
        Dictionary(uniqueKeysWithValues: (1...7).map { day in
            (day, DayProfile(isActive: day <= 5))
        })
    }
}

// MARK: - MemberSchedule
struct MemberSchedule: Identifiable {
    var id: String { member.id }
    var member: FamilyMember
    var wakeUpTime: DateComponents
    var bathroomStart: DateComponents
    var bathroomEnd: DateComponents
}

// MARK: - FamilySchedule
struct FamilySchedule {
    var memberSchedules: [MemberSchedule]
    var breakfastTime: DateComponents?
    var isValid: Bool
    var scheduleMessage: ScheduleMessage
}

// MARK: - ScheduleMessage
enum ScheduleMessage {
    case optimal
    case noActiveSchedule
    case memberConflict(String)
    case timeAdjusted
    case breakfastReduced
    case breakfastAndTimeAdjusted
}

// MARK: - DateComponents Helpers
extension DateComponents {
    var asDate: Date? {
        Calendar.current.date(from: self)
    }

    func formatted(_ format: String = "HH:mm") -> String {
        var cal = Calendar.current
        cal.timeZone = TimeZone.current
        guard let date = cal.date(from: self) else { return "--:--" }
        let f = DateFormatter()
        f.dateFormat = format
        return f.string(from: date)
    }

    static func from(hour: Int, minute: Int) -> DateComponents {
        DateComponents(hour: hour, minute: minute)
    }

    var totalMinutes: Int {
        (hour ?? 0) * 60 + (minute ?? 0)
    }

    static func < (lhs: DateComponents, rhs: DateComponents) -> Bool {
        lhs.totalMinutes < rhs.totalMinutes
    }
}
