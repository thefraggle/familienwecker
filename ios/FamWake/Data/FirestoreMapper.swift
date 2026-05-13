import Foundation
import FirebaseFirestore

// MARK: - Firestore ↔ FamilyMember Mapping
// Mirrors shared/androidMain/.../data/FamilyMemberMapper.kt
// Firestore stores times as "HH:mm" strings (e.g. "06:00") via KMP LocalTime.toString()

extension FamilyMember {

    /// Parse a Firestore document into a FamilyMember
    static func fromFirestore(_ data: [String: Any], id: String) -> FamilyMember {
        let name = data["name"] as? String ?? ""

        let earliest = DateComponents.fromTimeString(data["earliestWakeUp"] as? String ?? "06:00")
            ?? .from(hour: 6, minute: 0)
        let latest = DateComponents.fromTimeString(data["latestWakeUp"] as? String ?? "07:30")
            ?? .from(hour: 7, minute: 30)

        let bathroom = (data["bathroomDurationMinutes"] as? NSNumber)?.intValue ?? 20
        let breakfast = data["wantsBreakfast"] as? Bool ?? true
        let paused = data["isPaused"] as? Bool ?? false
        let awake = data["isAwakeToday"] as? Bool ?? false
        let resetDate = data["lastResetDate"] as? String ?? ""
        let seqOrder = (data["sequenceOrder"] as? NSNumber)?.intValue ?? 0

        let claimedUid = data["claimedByUserId"] as? String
        let claimedName = data["claimedByUserName"] as? String
        let claimedDevice = data["claimedByDeviceId"] as? String
        let alarmEnabled = data["deviceAlarmEnabled"] as? Bool

        // Leave home time
        let leaveHome = (data["leaveHomeTime"] as? String).flatMap { DateComponents.fromTimeString($0) }

        // Timestamps – can be Timestamp or Number
        let createdAt: Double? = parseTimestamp(data["createdAt"])
        let lastUpdated: Double? = parseTimestamp(data["lastUpdatedAt"])

        // Day profiles
        let dayProfiles = parseDayProfiles(data["dayProfiles"])

        return FamilyMember(
            id: id,
            name: name,
            earliestWakeUp: earliest,
            latestWakeUp: latest,
            bathroomDurationMinutes: bathroom,
            wantsBreakfast: breakfast,
            leaveHomeTime: leaveHome,
            isPaused: paused,
            isAwakeToday: awake,
            lastResetDate: resetDate,
            claimedByUserId: claimedUid,
            claimedByUserName: claimedName,
            claimedByDeviceId: claimedDevice,
            sequenceOrder: seqOrder,
            createdAt: createdAt,
            lastUpdatedAt: lastUpdated,
            deviceAlarmEnabled: alarmEnabled,
            dayProfiles: dayProfiles
        )
    }

    /// Convert FamilyMember to Firestore-compatible dictionary
    func toFirestoreMap() -> [String: Any] {
        var data: [String: Any] = [
            "name": name,
            "earliestWakeUp": earliestWakeUp.toTimeString(),
            "latestWakeUp": latestWakeUp.toTimeString(),
            "bathroomDurationMinutes": bathroomDurationMinutes,
            "wantsBreakfast": wantsBreakfast,
            "isPaused": isPaused,
            "isAwakeToday": isAwakeToday,
            "lastResetDate": lastResetDate,
            "sequenceOrder": sequenceOrder,
            "createdAt": createdAt ?? Date().timeIntervalSince1970 * 1000,
            "lastUpdatedAt": FieldValue.serverTimestamp()
        ]

        if let leave = leaveHomeTime {
            data["leaveHomeTime"] = leave.toTimeString()
        } else {
            data["leaveHomeTime"] = NSNull()
        }

        data["claimedByUserId"] = claimedByUserId as Any? ?? NSNull()
        data["claimedByUserName"] = claimedByUserName as Any? ?? NSNull()
        data["claimedByDeviceId"] = claimedByDeviceId as Any? ?? NSNull()
        data["deviceAlarmEnabled"] = deviceAlarmEnabled as Any? ?? NSNull()

        // Day profiles
        if let profiles = dayProfiles {
            var dpData: [String: Any] = [:]
            for (day, profile) in profiles {
                dpData["\(day)"] = [
                    "isActive": profile.isActive,
                    "earliestWakeUp": profile.earliestWakeUp.toTimeString(),
                    "latestWakeUp": profile.latestWakeUp.toTimeString(),
                    "bathroomDurationMinutes": profile.bathroomDurationMinutes,
                    "wantsBreakfast": profile.wantsBreakfast,
                    "leaveHomeTime": profile.leaveHomeTime?.toTimeString() as Any? ?? NSNull()
                ]
            }
            data["dayProfiles"] = dpData
        }

        return data
    }
}

// MARK: - Private Helpers

private func parseTimestamp(_ raw: Any?) -> Double? {
    switch raw {
    case let ts as Timestamp:
        return Double(ts.seconds) * 1000.0
    case let num as NSNumber:
        return num.doubleValue
    default:
        return nil
    }
}

private func parseDayProfiles(_ raw: Any?) -> [Int: DayProfile]? {
    guard let dpData = raw as? [String: [String: Any]] else { return nil }
    var result: [Int: DayProfile] = [:]
    for (key, dp) in dpData {
        guard let day = Int(key) else { continue }
        let active = dp["isActive"] as? Bool ?? true
        let earliest = DateComponents.fromTimeString(dp["earliestWakeUp"] as? String ?? "06:00")
            ?? .from(hour: 6, minute: 0)
        let latest = DateComponents.fromTimeString(dp["latestWakeUp"] as? String ?? "07:30")
            ?? .from(hour: 7, minute: 30)
        let bathroom = (dp["bathroomDurationMinutes"] as? NSNumber)?.intValue ?? 20
        let breakfast = dp["wantsBreakfast"] as? Bool ?? true
        let leave = (dp["leaveHomeTime"] as? String).flatMap { DateComponents.fromTimeString($0) }

        result[day] = DayProfile(
            isActive: active,
            earliestWakeUp: earliest,
            latestWakeUp: latest,
            bathroomDurationMinutes: bathroom,
            wantsBreakfast: breakfast,
            leaveHomeTime: leave
        )
    }
    return result.isEmpty ? nil : result
}
