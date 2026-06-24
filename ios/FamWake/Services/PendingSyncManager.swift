import Foundation

/// Verwaltet den Pending-Sync-Status für offline erstellte Familien.
/// Speichert in UserDefaults, da nur ein einziger Pending-State möglich ist.
final class PendingSyncManager {
    static let shared = PendingSyncManager()
    private let defaults = UserDefaults.standard
    
    private init() {}
    
    var isLocalOnlyFamily: Bool {
        get { defaults.bool(forKey: "is_local_only_family") }
        set { defaults.set(newValue, forKey: "is_local_only_family") }
    }
    
    func clear() {
        isLocalOnlyFamily = false
    }
}
