import Foundation

// MARK: - FamilyMember
/// Mirror of shared/commonMain/.../model/FamilyModels.kt – FamilyMember
/// Firestore fields use "HH:mm" strings for times (e.g. "06:00"), matching the KMP/Android format.
struct FamilyMember: Identifiable, Codable, Equatable {
    var id: String
    var name: String
    var earliestWakeUp: DateComponents  // hour + minute
    var latestWakeUp: DateComponents
    var bathroomDurationMinutes: Int
    var wantsBreakfast: Bool
    var leaveHomeTime: DateComponents?
    var isPaused: Bool
    var isAwakeToday: Bool
    var lastResetDate: String
    var claimedByUserId: String?
    var claimedByUserName: String?
    var claimedByDeviceId: String?
    var sequenceOrder: Int
    var createdAt: Double?
    var lastUpdatedAt: Double?
    var deviceAlarmEnabled: Bool?
    var dayProfiles: [Int: DayProfile]?  // 1=Mo...7=So, nil = not configured

    init(
        id: String = UUID().uuidString,
        name: String,
        earliestWakeUp: DateComponents = DateComponents(hour: 6, minute: 0),
        latestWakeUp: DateComponents = DateComponents(hour: 7, minute: 30),
        bathroomDurationMinutes: Int = 20,
        wantsBreakfast: Bool = true,
        leaveHomeTime: DateComponents? = nil,
        isPaused: Bool = false,
        isAwakeToday: Bool = false,
        lastResetDate: String = "",
        claimedByUserId: String? = nil,
        claimedByUserName: String? = nil,
        claimedByDeviceId: String? = nil,
        sequenceOrder: Int = 0,
        createdAt: Double? = nil,
        lastUpdatedAt: Double? = nil,
        deviceAlarmEnabled: Bool? = nil,
        dayProfiles: [Int: DayProfile]? = nil
    ) {
        self.id = id
        self.name = name
        self.earliestWakeUp = earliestWakeUp
        self.latestWakeUp = latestWakeUp
        self.bathroomDurationMinutes = bathroomDurationMinutes
        self.wantsBreakfast = wantsBreakfast
        self.leaveHomeTime = leaveHomeTime
        self.isPaused = isPaused
        self.isAwakeToday = isAwakeToday
        self.lastResetDate = lastResetDate
        self.claimedByUserId = claimedByUserId
        self.claimedByUserName = claimedByUserName
        self.claimedByDeviceId = claimedByDeviceId
        self.sequenceOrder = sequenceOrder
        self.createdAt = createdAt
        self.lastUpdatedAt = lastUpdatedAt
        self.deviceAlarmEnabled = deviceAlarmEnabled
        self.dayProfiles = dayProfiles
    }
}

// MARK: - DayProfile
/// Mirror of shared/commonMain/.../model/FamilyModels.kt – DayProfile
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

    /// Mo–Fr active, Sa–So inactive (default for new members)
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
    var targetDate: Date?  // Datum für das der Schedule berechnet wurde (nil = heute)
}

// MARK: - ScheduleMessage
enum ScheduleMessage: Equatable {
    case optimal
    case noActiveSchedule
    case memberConflict(String)
    case timeAdjusted(Int)
    case breakfastReduced(Int)
    case breakfastAndTimeAdjusted(Int, Int)
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

    /// Parse "HH:mm" string to DateComponents (matches KMP LocalTime.toString() format)
    static func fromTimeString(_ str: String) -> DateComponents? {
        let parts = str.split(separator: ":")
        guard parts.count >= 2,
              let h = Int(parts[0]),
              let m = Int(parts[1]) else { return nil }
        return DateComponents(hour: h, minute: m)
    }

    /// Convert to "HH:mm" string (matches KMP LocalTime.toString() format)
    func toTimeString() -> String {
        let h = hour ?? 0
        let m = minute ?? 0
        return String(format: "%02d:%02d", h, m)
    }
}
