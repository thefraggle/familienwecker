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
        
        return true
    }
    
    // MARK: - APNs / FCM Tokens
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("APNs Registrierung fehlgeschlagen: \(error.localizedDescription)")
        TelemetryManager.send("auth.apnsRegistrationFailed", with: ["error": error.localizedDescription])
    }
    
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        if let uid = Auth.auth().currentUser?.uid {
            MessagingService.shared.saveTokenToFirestore(uid: uid, token: token)
        }
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
    
    private func handleIncomingDataMessage(type: String) {
        // Debounce / Ignorieren, wenn Push in App deaktiviert ist
        let isEnabled = UserDefaults.standard.bool(forKey: "push_notifications_enabled") // TODO: AppState check
        // Da AppState hier schwer erreichbar ist, generieren wir die Notification. Der User kann sie in den iOS Settings deaktivieren.
        
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
        content.sound = .default
        
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
            // Normale Push-Nachrichten im Vordergrund als Banner zeigen
            completionHandler([.banner, .sound])
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
            AlarmService.shared.cancelWakeUp(memberId: memberId, isSnooze: true)
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
        FirebaseApp.configure()
        let settings = FirestoreSettings()
        settings.isPersistenceEnabled = true
        Firestore.firestore().settings = settings
        RevenueCatService.configure()
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

    var body: some Scene {
        WindowGroup {
            AppRouter()
                .environmentObject(appState)
                .environmentObject(authViewModel)
                .environmentObject(familyViewModel)
                .environmentObject(donationViewModel)
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
