import SwiftUI
import FirebaseCore
import FirebaseAuth
import GoogleSignIn
import FirebaseMessaging
import TelemetryClient
import UserNotifications
import FirebaseFirestore

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()
        if let clientID = FirebaseApp.app()?.options.clientID {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        }
        
        let configuration = TelemetryManagerConfiguration(appID: "65B4CF62-F147-42B5-9B7A-14CF0ADF949D")
        TelemetryManager.initialize(with: configuration)
        TelemetryManager.send("app.launched")
        
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        
        application.registerForRemoteNotifications()
        
        // Registrierung der Wecker-Kategorie und Actions
        let stopAction = UNNotificationAction(
            identifier: "STOP_ACTION",
            title: L.ringingStop,
            options: []
        )
        let snoozeAction = UNNotificationAction(
            identifier: "SNOOZE_ACTION",
            title: L.ringingSnooze,
            options: []
        )
        let alarmCategory = UNNotificationCategory(
            identifier: "ALARM",
            actions: [stopAction, snoozeAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        UNUserNotificationCenter.current().setNotificationCategories([alarmCategory])
        
        // Geister-Alarm-Schutz: Wenn Switch OFF → alle AlarmKit-Alarme sofort canceln
        // (z.B. nach Reinstall, wenn AlarmKit-Alarme noch im System stecken)
        let isAlarmEnabled = UserDefaults.standard.bool(forKey: "alarm_enabled")
        if !isAlarmEnabled {
            Task { await AlarmService.shared.cancelAll() }
        }
        
        return true
    }
    
    // MARK: - APNs / FCM Tokens
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        TelemetryManager.send("auth.apnsRegistrationFailed", with: ["error": error.localizedDescription])
    }
    
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        // Token-Speicherung erfolgt bereits über AuthViewModel.listenToAuthState()
        // und AppState.load() via refreshAndSaveToken() – inklusive Push-Toggle-Check.
        // Kein separater Save hier nötig: vermeidet doppelte Firestore-Writes und
        // verhindert, dass bei deaktiviertem Push der Token trotzdem gespeichert wird.
    }
    
    // MARK: - Silent Push / Data Messages
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        // FCM Data Message empfangen
        if let type = userInfo["type"] as? String {
            let hasAlert = (userInfo["aps"] as? [AnyHashable: Any])?["alert"] != nil
            if !hasAlert {
                handleIncomingDataMessage(type: type)
            }
            completionHandler(.newData)
            return
        }
        completionHandler(.noData)
    }
    
    // Client-Debounce: doppelte Pushes desselben Typs innerhalb 10s ignorieren
    private var lastNotifTimestamps: [String: Date] = [:]
    
    private func handleIncomingDataMessage(type: String) {
        // Push in App deaktiviert? → keine lokale Notification erzeugen
        let isEnabled = UserDefaults.standard.bool(forKey: "push_notifications_enabled")
        guard isEnabled else { return }
        
        // Debounce: gleicher Typ innerhalb 10s → ignorieren
        let now = Date()
        if let lastTime = lastNotifTimestamps[type], now.timeIntervalSince(lastTime) < 10 {
            return
        }
        lastNotifTimestamps[type] = now
        
        let title: String
        let body: String
        
        switch type {
        case "schedule_change":
            title = L.notifScheduleChangedTitle
            body = L.notifScheduleChangedBody
        case "family_joined":
            title = L.notifMemberJoinedTitle
            body = L.notifMemberJoinedBody
        case "family_left":
            title = L.notifMemberLeftTitle
            body = L.notifMemberLeftBody
        default:
            return
        }
        
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        // Kein Sound – stille Notification im Tray (analog Android IMPORTANCE_LOW)
        
        // Feste IDs pro Typ wie in Android
        let reqId = "famwake_push_\(type)"
        let request = UNNotificationRequest(identifier: reqId, content: content, trigger: nil)
        
        UNUserNotificationCenter.current().add(request)
    }
    
    // MARK: - Local Notifications (Alarms)
    
    // Erlaubt das Anzeigen (und Klingeln) von Benachrichtigungen, wenn die App im Vordergrund ist!
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        if let memberId = notification.request.content.userInfo["memberId"] as? String,
           let memberName = notification.request.content.userInfo["memberName"] as? String {
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .showRingingView, object: nil, userInfo: ["memberId": memberId, "memberName": memberName])
            }
        } else {
            // Normale Push-Nachrichten still im Notification Center anzeigen (kein Popup)
            completionHandler([.list])
            return
        }
        
        // Wir zeigen den Fullscreen-RingingView für Alarme, also brauchen wir keinen zusätzlichen iOS Banner
        completionHandler([])
    }
    
    // Wird aufgerufen, wenn der Benutzer auf die Notification tippt oder eine Action ausführt
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        guard let memberId = userInfo["memberId"] as? String,
              let memberName = userInfo["memberName"] as? String else {
            completionHandler()
            return
        }
        
        if response.actionIdentifier == "STOP_ACTION" {
            TelemetryManager.send("alarm.dismissed_background")
            AlarmService.shared.cancelWakeUp(memberId: memberId)
            UserDefaults.standard.removeObject(forKey: "snooze_until")
            
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .stopAlarmFromNotification, object: nil, userInfo: ["memberId": memberId])
            }
        } else if response.actionIdentifier == "SNOOZE_ACTION" {
            TelemetryManager.send("alarm.snoozed_background")
            AlarmService.shared.cancelWakeUp(memberId: memberId)
            
            let snoozeTime = Date().addingTimeInterval(5 * 60)
            UserDefaults.standard.set(snoozeTime.timeIntervalSince1970, forKey: "snooze_until")
            
            let alarmSoundUri = UserDefaults.standard.string(forKey: "alarm_sound_uri")
            AlarmService.shared.scheduleWakeUp(
                wakeUpTime: snoozeTime,
                memberId: memberId,
                memberName: memberName,
                soundUri: alarmSoundUri,
                isSnooze: true
            )
            
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .snoozeAlarmFromNotification, object: nil, userInfo: ["memberId": memberId, "memberName": memberName, "snoozeTime": snoozeTime])
            }
        } else {
            // Klick auf die Benachrichtigung selbst
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .showRingingView, object: nil, userInfo: ["memberId": memberId, "memberName": memberName])
            }
        }
        
        completionHandler()
    }
}

@main
struct FamWakeApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    // Statische Initialisierung, die vor der Zuweisung aller Properties läuft
    private static var sdkInit: Void = {
        let settings = FirestoreSettings()
        settings.isPersistenceEnabled = true
        Firestore.firestore().settings = settings
        RevenueCatService.configure()
        
        if ProcessInfo.processInfo.arguments.contains("-screenshotMode") {
            setupMockDataForScreenshots()
        }
    }()
    
    @StateObject private var appState: AppState = {
        _ = FamWakeApp.sdkInit
        return AppState()
    }()
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var familyViewModel = FamilyViewModel()
    @StateObject private var donationViewModel = DonationViewModel()

    init() {
        // Adjust large title font size to prevent truncation of long translated app names
        UINavigationBar.appearance().largeTitleTextAttributes = [
            .font: UIFont.systemFont(ofSize: 28, weight: .bold)
        ]
    }
    
    private static func setupMockDataForScreenshots() {
        // 1. Lokale Datenbank / UserDefaults leeren & vorbereiten
        UserDefaults.standard.removeObject(forKey: "language")
        UserDefaults.standard.removeObject(forKey: "snooze_until")
        UserDefaults.standard.set(0, forKey: "snooze_count")
        
        UserDefaults.standard.set(true, forKey: "alarm_enabled")
        UserDefaults.standard.set(true, forKey: "onboarding_completed")
        UserDefaults.standard.set(true, forKey: "is_local_only_family")
        UserDefaults.standard.set("MOCK_FAMILY_ID", forKey: "family_id")
        UserDefaults.standard.set("FW-982-XYZ", forKey: "family_join_code")
        UserDefaults.standard.set(false, forKey: "tooltips_enabled")
        
        // 2. Systemsprache aus Locale.preferredLanguages oder CLI-Args (-AppleLanguages) ermitteln
        var lang = "en"
        if let pref = Locale.preferredLanguages.first {
            let code = String(pref.prefix(2)).lowercased()
            if code == "in" || code == "id" { lang = "id" }
            else if code == "zh" { lang = "zh-Hans" }
            else if code == "no" || code == "nb" { lang = "nb" }
            else { lang = code }
        }
        
        let args = ProcessInfo.processInfo.arguments
        for (i, arg) in args.enumerated() {
            if arg == "-AppleLanguages" && i + 1 < args.count {
                let rawVal = args[i + 1]
                    .replacingOccurrences(of: "(", with: "")
                    .replacingOccurrences(of: ")", with: "")
                    .replacingOccurrences(of: "\"", with: "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                let first = rawVal.split(separator: ",").first.map(String.init) ?? rawVal
                var code = String(first.prefix(2)).lowercased()
                if code == "in" || code == "id" { code = "id" }
                if code == "zh" { code = "zh-Hans" }
                if code == "no" || code == "nb" { code = "nb" }
                lang = code
                break
            }
        }
        
        // Force-apply language to LanguageManager & UserDefaults for UI tests
        UserDefaults.standard.set(lang, forKey: "language")
        LanguageManager.shared.apply(lang)
        
        // 3. Lokalisierte Namen & Texte definieren
        var fatherName = "Papa"
        var motherName = "Mama"
        var childName = "Paul"
        var familyName = "Familie Müller"
        
        if lang == "en" {
            fatherName = "Dad"
            motherName = "Mom"
            childName = "Alex"
            familyName = "The Millers"
        } else if lang == "no" || lang == "nb" {
            fatherName = "Pappa"
            motherName = "Mamma"
            childName = "Jonas"
            familyName = "Familien"
        } else if lang == "da" {
            fatherName = "Far"
            motherName = "Mor"
            childName = "Lucas"
            familyName = "Familien"
        } else if lang == "nl" {
            fatherName = "Papa"
            motherName = "Mama"
            childName = "Daan"
            familyName = "Familie"
        } else if lang == "fr" {
            fatherName = "Papa"
            motherName = "Maman"
            childName = "Lucas"
            familyName = "Famille"
        } else if lang == "es" {
            fatherName = "Papá"
            motherName = "Mamá"
            childName = "Mateo"
            familyName = "Familia"
        } else if lang == "it" {
            fatherName = "Papà"
            motherName = "Mamma"
            childName = "Leonardo"
            familyName = "Famiglia"
        } else if lang == "pt" {
            fatherName = "Pai"
            motherName = "Mãe"
            childName = "Lucas"
            familyName = "Família"
        } else if lang == "id" || lang == "in" {
            fatherName = "Ayah"
            motherName = "Ibu"
            childName = "Budi"
            familyName = "Keluarga"
        } else if lang == "hi" {
            fatherName = "पापा"
            motherName = "मम्मी"
            childName = "अर्णव"
            familyName = "परिवार"
        } else if lang == "bn" {
            fatherName = "বাবা"
            motherName = "মা"
            childName = "রাহুল"
            familyName = "পরিবার"
        } else if lang == "mr" {
            fatherName = "बाबा"
            motherName = "आई"
            childName = "रोहन"
            familyName = "कुटुंब"
        } else if lang == "ja" {
            fatherName = "お父さん"
            motherName = "お母さん"
            childName = "蓮"
            familyName = "ファミリー"
        } else if lang == "ko" {
            fatherName = "아빠"
            motherName = "엄마"
            childName = "민준"
            familyName = "가족"
        } else if lang == "zh" {
            fatherName = "爸爸"
            motherName = "妈妈"
            childName = "小明"
            familyName = "家庭"
        } else if lang == "ru" || lang == "uk" {
            fatherName = "Папа"
            motherName = "Мама"
            childName = "Саша"
            familyName = "Семья"
        } else if lang == "tr" {
            fatherName = "Baba"
            motherName = "Anne"
            childName = "Can"
            familyName = "Aile"
        } else if lang == "vi" {
            fatherName = "Bố"
            motherName = "Mẹ"
            childName = "Minh"
            familyName = "Gia đình"
        } else if lang == "pl" {
            fatherName = "Tata"
            motherName = "Mama"
            childName = "Jan"
            familyName = "Rodzina"
        } else if lang == "sv" {
            fatherName = "Pappa"
            motherName = "Mamma"
            childName = "Emil"
            familyName = "Familjen"
        }
        
        UserDefaults.standard.set(familyName, forKey: "family_name")
        
        // Set myMemberId to father so he is the claimed user on this device
        let dadId = "mock_dad"
        UserDefaults.standard.set(dadId, forKey: "my_member_id")
        
        // 4. DayProfiles generieren, damit der Weckzeitplan berechnet werden kann
        func makeDayProfiles(earliest: DateComponents, latest: DateComponents, bathroom: Int, leave: DateComponents?) -> [Int: DayProfile] {
            var profiles: [Int: DayProfile] = [:]
            for day in 1...7 {
                profiles[day] = DayProfile(
                    isActive: true,
                    earliestWakeUp: earliest,
                    latestWakeUp: latest,
                    bathroomDurationMinutes: bathroom,
                    wantsBreakfast: true,
                    leaveHomeTime: leave,
                    isSimpleMode: false
                )
            }
            return profiles
        }
        
        let earliestWakeDad = DateComponents(hour: 6, minute: 0)
        let latestWakeDad = DateComponents(hour: 7, minute: 15)
        let leaveDad = DateComponents(hour: 8, minute: 0)
        let dadProfiles = makeDayProfiles(earliest: earliestWakeDad, latest: latestWakeDad, bathroom: 15, leave: leaveDad)
        
        let earliestWakeMom = DateComponents(hour: 6, minute: 0)
        let latestWakeMom = DateComponents(hour: 7, minute: 30)
        let leaveMom = DateComponents(hour: 8, minute: 15)
        let momProfiles = makeDayProfiles(earliest: earliestWakeMom, latest: latestWakeMom, bathroom: 20, leave: leaveMom)
        
        let earliestWakeChild = DateComponents(hour: 6, minute: 0)
        let latestWakeChild = DateComponents(hour: 7, minute: 45)
        let leaveChild = DateComponents(hour: 8, minute: 15)
        let childProfiles = makeDayProfiles(earliest: earliestWakeChild, latest: latestWakeChild, bathroom: 10, leave: leaveChild)
        
        let dad = FamilyMember(
            id: dadId,
            name: fatherName,
            earliestWakeUp: earliestWakeDad,
            latestWakeUp: latestWakeDad,
            bathroomDurationMinutes: 15,
            wantsBreakfast: true,
            leaveHomeTime: leaveDad,
            isPaused: false,
            isAwakeToday: false,
            claimedByUserId: "mock_user_id",
            claimedByUserName: fatherName,
            sequenceOrder: 0,
            deviceAlarmEnabled: true,
            dayProfiles: dadProfiles
        )
        
        let mom = FamilyMember(
            id: "mock_mom",
            name: motherName,
            earliestWakeUp: earliestWakeMom,
            latestWakeUp: latestWakeMom,
            bathroomDurationMinutes: 20,
            wantsBreakfast: true,
            leaveHomeTime: leaveMom,
            isPaused: false,
            isAwakeToday: false,
            sequenceOrder: 1,
            deviceAlarmEnabled: false,
            dayProfiles: momProfiles
        )
        
        let child = FamilyMember(
            id: "mock_child",
            name: childName,
            earliestWakeUp: earliestWakeChild,
            latestWakeUp: latestWakeChild,
            bathroomDurationMinutes: 10,
            wantsBreakfast: true,
            leaveHomeTime: leaveChild,
            isPaused: false,
            isAwakeToday: false,
            sequenceOrder: 2,
            deviceAlarmEnabled: false,
            dayProfiles: childProfiles
        )
        
        // Save to Cache
        LocalMemberStore.shared.save(members: [dad, mom, child], familyId: "MOCK_FAMILY_ID")
    }

    var body: some Scene {
        WindowGroup {
            AppRouter()
                .environmentObject(appState)
                .environmentObject(authViewModel)
                .environmentObject(familyViewModel)
                .environmentObject(donationViewModel)
                .environment(\.locale, LanguageManager.shared.currentLocale)
                .id(appState.languageId)
                .preferredColorScheme(appState.colorScheme)
                .onOpenURL { url in
                    // Google Sign-In URL handler for OAuth callback
                    GIDSignIn.sharedInstance.handle(url)
                    handleDeepLink(url: url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { userActivity in
                    if let url = userActivity.webpageURL {
                        handleDeepLink(url: url)
                    }
                }
        }
    }
    
    private func handleDeepLink(url: URL) {
        guard let host = url.host, host == "familienwecker.de" || host == "www.familienwecker.de" else { return }
        
        let path = url.path
        if path.hasPrefix("/join/") {
            let code = url.lastPathComponent
            if !code.isEmpty && code != "join" {
                let sanitized = String(code.filter { $0.isLetter || $0.isNumber }.uppercased().prefix(6))
                familyViewModel.pendingJoinCode = sanitized
            }
        } else if path.contains("/verify-email") {
            if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
               let queryItems = components.queryItems,
               let oobCode = queryItems.first(where: { $0.name == "oobCode" })?.value {
                authViewModel.applyActionCode(oobCode)
            }
        }
    }
}
