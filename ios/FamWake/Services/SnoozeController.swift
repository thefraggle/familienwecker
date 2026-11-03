import Foundation

/// Zentraler Controller für die Snooze-Verwaltung auf iOS.
/// Verhindert Limit-Bypasses und harmonisiert die 4 separaten Snooze-Stellen.
/// Bindet den Snooze-Reset an das Kalenderdatum (YYYY-MM-DD) der Weck-Session.
final class SnoozeController {
    static let shared = SnoozeController()

    private let userDefaults = UserDefaults.standard
    private let keySnoozeUntil = "snooze_until"
    private let keySnoozeCount = "snooze_count"
    private let keySnoozeDate = "snooze_session_date"

    private init() {}

    private var todayString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }

    /// Bereinigt den Snooze-Zähler, falls wir an einem neuen Kalendertag sind.
    private func resetIfNewDay() {
        let savedDate = userDefaults.string(forKey: keySnoozeDate)
        if savedDate != todayString {
            userDefaults.set(0, forKey: keySnoozeCount)
            userDefaults.removeObject(forKey: keySnoozeUntil)
            userDefaults.set(todayString, forKey: keySnoozeDate)
        }
    }

    var snoozeCount: Int {
        resetIfNewDay()
        return userDefaults.integer(forKey: keySnoozeCount)
    }

    var snoozeUntil: Date? {
        resetIfNewDay()
        let ts = userDefaults.double(forKey: keySnoozeUntil)
        guard ts > 0 else { return nil }
        return Date(timeIntervalSince1970: ts)
    }

    var canSnooze: Bool {
        return snoozeCount < SnoozeConfig.maxSnoozeCount
    }

    /// Führt einen Snooze aus: erhöht den Zähler, setzt die Endzeit und speichert das Datum.
    @discardableResult
    func triggerSnooze(durationMinutes: Int = SnoozeConfig.snoozeDurationMinutes) -> Date? {
        resetIfNewDay()
        let current = userDefaults.integer(forKey: keySnoozeCount)
        guard current < SnoozeConfig.maxSnoozeCount else { return nil }

        let newCount = current + 1
        let until = Date().addingTimeInterval(Double(durationMinutes * 60))
        userDefaults.set(newCount, forKey: keySnoozeCount)
        userDefaults.set(until.timeIntervalSince1970, forKey: keySnoozeUntil)
        userDefaults.set(todayString, forKey: keySnoozeDate)
        return until
    }

    /// Setzt den Snooze-Zustand explizit zurück (z.B. beim Aufwachen, Stop oder Logout).
    func reset() {
        userDefaults.removeObject(forKey: keySnoozeUntil)
        userDefaults.set(0, forKey: keySnoozeCount)
        userDefaults.removeObject(forKey: keySnoozeDate)
    }
}
