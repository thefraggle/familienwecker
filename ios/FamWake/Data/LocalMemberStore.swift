import Foundation

/// Lokaler JSON-basierter Member-Cache.
/// Persistiert Members auf Disk, damit sie App-Kills überleben.
/// Bewusst kein CoreData/SwiftData – max ~10 Members, JSON reicht.
final class LocalMemberStore {
    static let shared = LocalMemberStore()
    
    private let fileManager = FileManager.default
    
    private var storeDirectory: URL {
        let docs = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        return docs.appendingPathComponent("FamWakeCache", isDirectory: true)
    }
    
    private init() {
        try? fileManager.createDirectory(at: storeDirectory, withIntermediateDirectories: true)
    }
    
    private func fileURL(familyId: String) -> URL {
        storeDirectory.appendingPathComponent("members_\(familyId).json")
    }
    
    func save(members: [FamilyMember], familyId: String) {
        do {
            let data = try JSONEncoder().encode(members)
            try data.write(to: fileURL(familyId: familyId), options: .atomic)
        } catch {
            print("LocalMemberStore: save failed – \(error.localizedDescription)")
        }
    }
    
    func load(familyId: String) -> [FamilyMember] {
        let url = fileURL(familyId: familyId)
        guard fileManager.fileExists(atPath: url.path) else { return [] }
        do {
            let data = try Data(contentsOf: url)
            return try JSONDecoder().decode([FamilyMember].self, from: data)
        } catch {
            print("LocalMemberStore: load failed – \(error.localizedDescription)")
            return []
        }
    }
    
    func delete(familyId: String) {
        try? fileManager.removeItem(at: fileURL(familyId: familyId))
    }
    
    /// Migriert Member-Cache von temporärer ID auf echte Firestore-ID.
    func migrateFamilyId(from oldId: String, to newId: String) {
        let oldURL = fileURL(familyId: oldId)
        let newURL = fileURL(familyId: newId)
        guard fileManager.fileExists(atPath: oldURL.path) else { return }
        try? fileManager.moveItem(at: oldURL, to: newURL)
    }
}
