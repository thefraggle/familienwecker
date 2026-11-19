import Foundation

/// Zentraler Controller für die Snooze-Verwaltung auf iOS.
/// Kapselt alle Snooze-Konstanten, Tages-Resets, Stale-Bereinigung und Limit-Prüfungen.
final class SnoozeController: @unchecked Sendable {
    static let shared = SnoozeController()

    // Snooze-Konstanten – 1:1 Parität mit KMP SnoozeConfig.kt
    nonisolated static let snoozeDurationMinutes = 5
    nonisolated static let maxSnoozeCount = 2
    nonisolated static let minBathroomMinutes: Int64 = 5
    private static let staleThresholdSeconds: TimeInterval = 30 * 60

    struct Status: Equatable {
        let until: Date?
        let count: Int
        var isActive: Bool { until != nil }
    }

    private let userDefaults: UserDefaults
    private let now: () -> Date
    private let keySnoozeUntil = "snooze_until"
    private let keySnoozeCount = "snooze_count"
    private let keySnoozeDate = "snooze_session_date"

    init(
        userDefaults: UserDefaults = .standard,
        now: @escaping () -> Date = Date.init
    ) {
        self.userDefaults = userDefaults
        self.now = now
    }

    private func dayString(for date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    /// Bereinigt den Snooze-Zähler, falls wir an einem neuen Kalendertag sind.
    private func resetIfNewDay(currentDate: Date) {
        let today = dayString(for: currentDate)
        let savedDate = userDefaults.string(forKey: keySnoozeDate)
        if savedDate != today {
            userDefaults.set(0, forKey: keySnoozeCount)
            userDefaults.removeObject(forKey: keySnoozeUntil)
            userDefaults.set(today, forKey: keySnoozeDate)
        }
    }

    var snoozeCount: Int {
        resetIfNewDay(currentDate: now())
        return userDefaults.integer(forKey: keySnoozeCount)
    }

    var snoozeUntil: Date? {
        resetIfNewDay(currentDate: now())
        let ts = userDefaults.double(forKey: keySnoozeUntil)
        guard ts > 0 else { return nil }
        return Date(timeIntervalSince1970: ts)
    }

    /// Gibt das Snooze-Enddatum nur zurück, wenn es noch in der Zukunft liegt.
    var activeSnoozeUntil: Date? {
        let current = now()
        resetIfNewDay(currentDate: current)
        let ts = userDefaults.double(forKey: keySnoozeUntil)
        guard ts > 0 else { return nil }
        let until = Date(timeIntervalSince1970: ts)
        return until > current ? until : nil
    }

    var canSnooze: Bool {
        snoozeCount < Self.maxSnoozeCount
    }

    /// Wertet den aktuellen Snooze-Zustand inklusive Stale-Schwelle (>30 Min) aus.
    func currentStatus() -> Status {
        let current = now()
        resetIfNewDay(currentDate: current)
        let ts = userDefaults.double(forKey: keySnoozeUntil)
        guard ts > 0 else {
            return Status(until: nil, count: userDefaults.integer(forKey: keySnoozeCount))
        }

        let until = Date(timeIntervalSince1970: ts)
        if until > current {
            return Status(until: until, count: userDefaults.integer(forKey: keySnoozeCount))
        }

        let staleThreshold = current.addingTimeInterval(-Self.staleThresholdSeconds)
        if until < staleThreshold {
            userDefaults.removeObject(forKey: keySnoozeUntil)
            userDefaults.set(0, forKey: keySnoozeCount)
            return Status(until: nil, count: 0)
        }

        // Gerade erst abgelaufen – Count bleibt für Max-Snooze-Schutz erhalten
        return Status(until: nil, count: userDefaults.integer(forKey: keySnoozeCount))
    }

    /// Führt einen Snooze aus: erhöht den Zähler, setzt die Endzeit und speichert das Datum.
    @discardableResult
    func triggerSnooze(durationMinutes: Int = SnoozeController.snoozeDurationMinutes) -> Date? {
        let currentDate = now()
        resetIfNewDay(currentDate: currentDate)
        let current = userDefaults.integer(forKey: keySnoozeCount)
        guard current < Self.maxSnoozeCount else { return nil }

        let newCount = current + 1
        let until = currentDate.addingTimeInterval(Double(durationMinutes * 60))
        userDefaults.set(newCount, forKey: keySnoozeCount)
        userDefaults.set(until.timeIntervalSince1970, forKey: keySnoozeUntil)
        userDefaults.set(dayString(for: currentDate), forKey: keySnoozeDate)
        return until
    }

    /// Setzt den Snooze-Zustand nur zurück, wenn kein aktiver Snooze läuft.
    /// Verhindert, dass ein laufender Snooze beim Tages-Reset verworfen wird.
    @discardableResult
    func clearIfNotActive() -> Bool {
        guard activeSnoozeUntil == nil else { return false }
        userDefaults.set(0, forKey: keySnoozeCount)
        userDefaults.removeObject(forKey: keySnoozeUntil)
        return true
    }

    /// Setzt den Snooze-Zustand explizit zurück (z.B. beim Aufwachen, Stop oder Logout).
    func reset() {
        userDefaults.removeObject(forKey: keySnoozeUntil)
        userDefaults.set(0, forKey: keySnoozeCount)
        userDefaults.removeObject(forKey: keySnoozeDate)
    }
}
