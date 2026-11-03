import Foundation
import SwiftUI
import FirebaseFunctions
import Aptabase

// MARK: - FamilyViewModel Vacation, Notification & Feedback Extension

extension FamilyViewModel {

    // MARK: - Vacation Mode
    func setVacationUntil(_ date: String?) {
        vacationUntil = date
        if let date = date {
            UserDefaults.standard.set(date, forKey: "vacation_until")
        } else {
            UserDefaults.standard.removeObject(forKey: "vacation_until")
        }
        if isEveningReminderEnabled {
            AlarmService.shared.scheduleEveningReminder()
        }
        recalculateSchedule()

        guard let fid = familyId, !isLocalOnlyFamily else { return }
        Task {
            do {
                try await FamilyFirestoreService.shared.updateVacationUntil(familyId: fid, vacationUntil: date)
            } catch {
                await MainActor.run { errorMessage = error.localizedDescription }
            }
        }
    }

    func clearVacation() {
        setVacationUntil(nil)
    }

    func setVacationPreset(daysToAdd: Int) {
        let cal = Calendar.current
        guard let target = cal.date(byAdding: .day, value: daysToAdd, to: Date()) else { return }
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        setVacationUntil(f.string(from: target))
    }

    func setVacationPresetEndOfMonth() {
        let cal = Calendar.current
        let now = Date()
        guard let range = cal.range(of: .day, in: .month, for: now),
              let lastDay = range.last,
              let target = cal.date(bySetting: .day, value: lastDay, of: now) else { return }
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        setVacationUntil(f.string(from: target))
    }

    func formatVacationDate(_ dateStr: String?) -> String {
        guard let dateStr = dateStr, !dateStr.isEmpty else { return "" }
        let fIn = DateFormatter()
        fIn.calendar = Calendar(identifier: .gregorian)
        fIn.locale = Locale(identifier: "en_US_POSIX")
        fIn.dateFormat = "yyyy-MM-dd"
        guard let d = fIn.date(from: dateStr) else { return dateStr }

        let fOut = DateFormatter()
        fOut.setLocalizedDateFormatFromTemplate("EEEdMMM")
        return fOut.string(from: d)
    }

    func getFirstAlarmDateAfterVacation(_ vacationUntilStr: String?) -> String? {
        guard let vacationUntilStr = vacationUntilStr, !vacationUntilStr.isEmpty else { return nil }
        let fIn = DateFormatter()
        fIn.calendar = Calendar(identifier: .gregorian)
        fIn.locale = Locale(identifier: "en_US_POSIX")
        fIn.dateFormat = "yyyy-MM-dd"
        guard let vacEndDate = fIn.date(from: vacationUntilStr) else { return nil }

        let cal = Calendar.current
        guard let startSearchDate = cal.date(byAdding: .day, value: 1, to: vacEndDate) else { return nil }
        let currentMyId = self.myMemberId
        let myMember = members.first(where: { $0.id == currentMyId })

        for offset in 0..<7 {
            if let checkDate = cal.date(byAdding: .day, value: offset, to: startSearchDate) {
                let weekdayRaw = cal.component(.weekday, from: checkDate)
                let checkIsoDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1
                let hasAlarm: Bool
                if let my = myMember {
                    let profile = my.dayProfiles?[checkIsoDow]
                    hasAlarm = !my.isPaused && (profile?.isActive ?? false)
                } else {
                    hasAlarm = members.contains { m in
                        let profile = m.dayProfiles?[checkIsoDow]
                        return !m.isPaused && (profile?.isActive ?? false)
                    }
                }
                if hasAlarm {
                    let fOut = DateFormatter()
                    fOut.setLocalizedDateFormatFromTemplate("EEEdMMM")
                    return fOut.string(from: checkDate)
                }
            }
        }
        return nil
    }

    // MARK: - Bathroom Free Notification
    func notifyBathroomFree(completion: @escaping (Bool) -> Void = { _ in }) {
        guard let fid = familyId, let myId = myMemberId else {
            completion(false)
            return
        }
        let myMember = members.first(where: { $0.id == myId })
        let memberName = myMember?.name ?? "jemand"

        bathroomFreeSending = true
        Task {
            do {
                try await FamilyFirestoreService.shared.notifyBathroomFree(familyId: fid, memberId: myId, memberName: memberName)
                await MainActor.run {
                    self.bathroomFreeSending = false
                    self.bathroomFreeSent = true
                    completion(true)
                }
                try? await Task.sleep(nanoseconds: 3_000_000_000)
                await MainActor.run {
                    self.bathroomFreeSent = false
                }
            } catch {
                await MainActor.run {
                    self.bathroomFreeSending = false
                    self.errorMessage = error.localizedDescription
                    completion(false)
                }
            }
        }
    }

    // MARK: - Error Mapping
    func mapFirebaseError(_ error: Error) -> String {
        FirebaseErrorMapper.map(error)
    }

    // MARK: - Feedback
    func sendFeedback(category: String, message: String, email: String, appVersion: String, device: String) {
        isSendingFeedback = true
        feedbackError = nil
        let data: [String: Any] = [
            "category": category,
            "message": message.trimmingCharacters(in: .whitespacesAndNewlines),
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines),
            "appVersion": appVersion,
            "device": device
        ]
        Task {
            do {
                try await functions.httpsCallable("sendFeedbackEmail").call(data)
                feedbackSubmitted = true
                Aptabase.shared.trackEvent("feedback_submitted", with: ["category": category])
            } catch {
                feedbackError = mapFirebaseError(error)
            }
            isSendingFeedback = false
        }
    }

    func resetFeedbackState() {
        feedbackSubmitted = false
        feedbackError = nil
    }
}
