import Foundation
import FirebaseMessaging
import FirebaseFirestore
import FirebaseAuth
import CryptoKit

class MessagingService {
    static let shared = MessagingService()
    
    private init() {}
    
    // MARK: - Token Management
    
    /// Holt den aktuellen FCM Token und speichert ihn in Firestore – NUR wenn Push aktiviert ist.
    /// Bei deaktiviertem Push wird ein evtl. vorhandener Token aus Firestore gelöscht.
    func refreshAndSaveToken() {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        
        let pushEnabled = UserDefaults.standard.object(forKey: "push_notifications_enabled") as? Bool ?? true
        
        Messaging.messaging().token { token, error in
            if let error = error {
                print("FCM Token anfordern fehlgeschlagen: \(error.localizedDescription)")
            } else if let token = token {
                if pushEnabled {
                    print("FCM Token erhalten: speichere in Firestore")
                    self.saveTokenToFirestore(uid: uid, token: token)
                } else {
                    print("FCM Token erhalten, aber Push deaktiviert – lösche aus Firestore")
                    self.deleteTokenFromFirestore(uid: uid, token: token)
                }
            }
        }
    }
    
    /// Löscht den FCM Token aus Firestore (Push-Toggle OFF).
    /// Der lokale Token bleibt bestehen, damit er bei Toggle-ON sofort wieder registriert werden kann.
    func deleteToken() {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        
        Messaging.messaging().token { token, error in
            if let token = token {
                self.deleteTokenFromFirestore(uid: uid, token: token)
            }
        }
    }
    
    /// Löscht den FCM Token aus Firestore UND lokal (Logout).
    func deleteTokenOnLogout() {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        
        Messaging.messaging().token { token, error in
            if let token = token {
                self.deleteTokenFromFirestore(uid: uid, token: token)
            }
            // FCM Token lokal löschen, damit ein neuer generiert wird beim nächsten Login
            Messaging.messaging().deleteToken { _ in }
        }
    }
    
    private func deleteTokenFromFirestore(uid: String, token: String) {
        let docId = sha256(token)
        Firestore.firestore().collection("users").document(uid).collection("fcmTokens").document(docId).delete { error in
            if let error = error {
                print("FCM Token löschen fehlgeschlagen: \(error.localizedDescription)")
            } else {
                print("FCM Token aus Firestore gelöscht")
            }
        }
    }
    
    func saveTokenToFirestore(uid: String, token: String) {
        let docId = sha256(token)
        let tokenData: [String: Any] = [
            "token": token,
            "platform": "ios",
            "lastRefresh": FieldValue.serverTimestamp()
        ]
        
        Firestore.firestore().collection("users").document(uid).collection("fcmTokens").document(docId).setData(tokenData, merge: true) { error in
            if let error = error {
                print("FCM Token speichern fehlgeschlagen: \(error.localizedDescription)")
            }
        }
    }
    
    private func sha256(_ input: String) -> String {
        let data = Data(input.utf8)
        let hash = SHA256.hash(data: data)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }
}
