import Foundation
import Security

/// Einfacher Keychain-Helper für Alarm-UUIDs.
/// UUIDs müssen Reinstalls überleben, damit AlarmKit-Alarme nach Neuinstallation
/// gecancelt werden können (UserDefaults werden bei Reinstall gelöscht, Keychain nicht).
struct KeychainHelper {
    
    private static let service = "de.familienwecker.famwake.alarms"
    
    /// Speichert einen Wert in der Keychain.
    static func save(key: String, value: String) {
        let data = value.data(using: .utf8)!
        
        // Erst löschen (falls vorhanden), dann neu anlegen
        let deleteQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(deleteQuery as CFDictionary)
        
        let addQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        SecItemAdd(addQuery as CFDictionary, nil)
    }
    
    /// Liest einen Wert aus der Keychain.
    static func read(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
    
    /// Löscht einen Wert aus der Keychain.
    static func delete(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)
    }
    
    /// Liest alle gespeicherten Alarm-UUIDs aus der Keychain.
    /// Wird von cancelAll() genutzt um nach Reinstall Geister-Alarme zu canceln.
    static func readAllAlarmUUIDs() -> [UUID] {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecReturnData as String: true,
            kSecReturnAttributes as String: true,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess, let items = result as? [[String: Any]] else { return [] }
        
        return items.compactMap { item in
            guard let account = item[kSecAttrAccount as String] as? String,
                  account.hasPrefix("alarm_uuid_"),
                  let data = item[kSecValueData as String] as? Data,
                  let uuidStr = String(data: data, encoding: .utf8),
                  let uuid = UUID(uuidString: uuidStr)
            else { return nil }
            return uuid
        }
    }
}
