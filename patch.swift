extension FamilyViewModel {
    func checkAndResetMembers() {
        let cal = Calendar.current
        let now = Date()
        
        let weekdayRaw = cal.component(.weekday, from: now)
        let todayDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1
        
        // Let's reset isAwakeTodayLocal if it's past the reset threshold (latestWakeUp + 2h)
        guard let myId = myMemberId, let myMember = members.first(where: { $0.id == myId }) else { return }
        
        if let profile = myMember.dayProfiles?[todayDow], profile.isActive {
            if let latestDate = cal.date(bySettingHour: profile.latestWakeUp.hour ?? 0, minute: profile.latestWakeUp.minute ?? 0, second: 0, of: now),
               let resetThreshold = cal.date(byAdding: .hour, value: 2, to: latestDate) {
                if now >= resetThreshold {
                    if self.isAwakeTodayLocal {
                        self.setAwakeTodayLocal(false)
                    }
                }
            }
        } else {
            // If there's no active alarm today, and it's late in the day (e.g. past 12:00), just reset it to be safe
            if cal.component(.hour, from: now) >= 12 && self.isAwakeTodayLocal {
                self.setAwakeTodayLocal(false)
            }
        }
    }
}
