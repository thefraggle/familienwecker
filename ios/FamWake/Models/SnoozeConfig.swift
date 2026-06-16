import Foundation

/// Snooze-Konstanten – 1:1 Parität mit KMP SnoozeConfig.kt
/// nonisolated: Wird auch aus Intents ohne @MainActor gelesen
enum SnoozeConfig {
    nonisolated static let snoozeDurationMinutes = 5
    nonisolated static let maxSnoozeCount = 2
    nonisolated static let minBathroomMinutes: Int64 = 5
}
