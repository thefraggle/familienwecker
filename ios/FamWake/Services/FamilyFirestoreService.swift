import Foundation
import FirebaseFirestore
import FirebaseAuth
import FirebaseFunctions

@MainActor
final class FamilyFirestoreService {
    static let shared = FamilyFirestoreService()
    private let db = Firestore.firestore()
    private let functions = Functions.functions(region: "europe-west3")
    
    private init() {}
    
    // MARK: - Error Mapping
    private func mapFirebaseError(_ error: Error) -> String {
        FirebaseErrorMapper.map(error)
    }

    func createFamily(name: String) async throws -> (familyId: String, familyName: String, joinCode: String) {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw NSError(domain: "FamWake", code: 401, userInfo: [NSLocalizedDescriptionKey: "Not authenticated"])
        }
        do {
            let result = try await functions.httpsCallable("createFamily").call(["familyName": name, "userId": uid])
            guard let data = result.data as? [String: Any],
                  let familyId = data["familyId"] as? String,
                  let joinCode = data["joinCode"] as? String else {
                throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: L.errorGeneric])
            }
            let familyName = data["familyName"] as? String ?? name
            return (familyId, familyName, joinCode)
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func joinFamily(code: String) async throws -> (familyId: String, familyName: String, joinCode: String) {
        do {
            let result = try await functions.httpsCallable("joinFamilyByCode").call(["code": code.uppercased()])
            guard let data = result.data as? [String: Any],
                  let familyId = data["familyId"] as? String,
                  let joinCode = data["joinCode"] as? String else {
                throw NSError(domain: "FamWake", code: 404, userInfo: [NSLocalizedDescriptionKey: L.errorFamilyNotFound])
            }
            let familyName = data["familyName"] as? String ?? "FamWake"
            return (familyId, familyName, joinCode)
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func leaveFamily(familyId: String, memberId: String?) async throws {
        var params: [String: Any] = ["familyId": familyId]
        if let mid = memberId { params["memberId"] = mid }
        try await functions.httpsCallable("leaveFamily").call(params)
    }

    func deleteFamily(familyId: String) async throws {
        do {
            try await functions.httpsCallable("deleteFamily").call(["familyId": familyId])
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func addOrUpdateMember(familyId: String, member: FamilyMember) async throws {
        let data = member.toFirestoreMap()
        do {
            try await db.collection("families").document(familyId)
                .collection("members").document(member.id)
                .setData(data)
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func deleteMember(familyId: String, memberId: String) async throws {
        do {
            try await db.collection("families").document(familyId).collection("members").document(memberId).delete()
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func togglePauseMember(familyId: String, memberId: String, newPausedState: Bool) async throws {
        do {
            try await db.collection("families").document(familyId).collection("members").document(memberId)
                .updateData(["isPaused": newPausedState])
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func setAwake(familyId: String, memberId: String, awake: Bool) async throws {
        do {
            try await db.collection("families").document(familyId).collection("members").document(memberId)
                .updateData(["isAwakeToday": awake])
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func updateDeviceAlarmEnabled(familyId: String, memberId: String, enabled: Bool) async throws {
        do {
            try await db.collection("families").document(familyId).collection("members").document(memberId)
                .updateData(["deviceAlarmEnabled": enabled])
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func setGlobalBufferMinutes(familyId: String, minutes: Int) async throws {
        do {
            try await db.collection("families").document(familyId)
                .updateData(["globalBufferMinutes": minutes])
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func updateVacationUntil(familyId: String, vacationUntil: String?) async throws {
        do {
            if let date = vacationUntil {
                try await db.collection("families").document(familyId)
                    .updateData(["vacationUntil": date])
            } else {
                try await db.collection("families").document(familyId)
                    .updateData(["vacationUntil": FieldValue.delete()])
            }
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func notifyBathroomFree(familyId: String, memberId: String, memberName: String) async throws {
        let data: [String: Any] = [
            "familyId": familyId,
            "memberId": memberId,
            "memberName": memberName
        ]
        do {
            _ = try await functions.httpsCallable("notifyBathroomFree").call(data)
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }

    func sendFeedback(category: String, message: String, email: String, appVersion: String, device: String) async throws {
        let data: [String: Any] = [
            "category": category,
            "message": message.trimmingCharacters(in: .whitespacesAndNewlines),
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines),
            "appVersion": appVersion,
            "device": device
        ]
        do {
            try await functions.httpsCallable("sendFeedbackEmail").call(data)
        } catch {
            throw NSError(domain: "FamWake", code: 500, userInfo: [NSLocalizedDescriptionKey: mapFirebaseError(error)])
        }
    }
    
    func trackUserAction(familyId: String) async {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        do {
            try await db.collection("users").document(uid)
                .collection("pushMeta").document("user_action")
                .setData([
                    "familyId": familyId,
                    "timestamp": FieldValue.serverTimestamp()
                ])
        } catch {
            // Silently ignore tracking errors
        }
    }

    func resetMembersBatch(familyId: String, membersToUpdate: [FamilyMember], todayStr: String) async throws {
        let batch = db.batch()
        for member in membersToUpdate {
            let ref = db.collection("families").document(familyId).collection("members").document(member.id)
            var updates: [String: Any] = [
                "isAwakeToday": false,
                "lastResetDate": todayStr,
                "lastUpdatedAt": FieldValue.serverTimestamp()
            ]
            if member.claimedByUserId == nil {
                updates["isPaused"] = false
            }
            batch.updateData(updates, forDocument: ref)
        }
        try await batch.commit()
    }

    func claimProfile(familyId: String, memberId: String, uid: String, userName: String, deviceId: String, force: Bool) async throws -> Bool {
        let docRef = db.collection("families").document(familyId).collection("members").document(memberId)
        let success = try await db.runTransaction({ (transaction, errorPointer) -> Any? in
            let snapshot: DocumentSnapshot
            do {
                try snapshot = transaction.getDocument(docRef)
            } catch let error as NSError {
                errorPointer?.pointee = error
                return false
            }
            
            let existingClaim = snapshot.data()?["claimedByUserId"] as? String
            if force || existingClaim == nil || existingClaim == uid {
                transaction.updateData([
                    "claimedByUserId": uid,
                    "claimedByUserName": userName,
                    "claimedByDeviceId": deviceId,
                    "deviceAlarmEnabled": true,
                    "lastUpdatedAt": FieldValue.serverTimestamp()
                ], forDocument: docRef)
                return true
            } else {
                let err = NSError(domain: "FamWake", code: 409, userInfo: [NSLocalizedDescriptionKey: "profile_taken"])
                errorPointer?.pointee = err
                return false
            }
        }) as? Bool ?? false
        return success
    }

    func unclaimProfile(familyId: String, memberId: String, uid: String) async throws {
        let docRef = db.collection("families").document(familyId).collection("members").document(memberId)
        _ = try await db.runTransaction({ (transaction, errorPointer) -> Any? in
            let snapshot: DocumentSnapshot
            do {
                try snapshot = transaction.getDocument(docRef)
            } catch let error as NSError {
                errorPointer?.pointee = error
                return false
            }
            let existingClaim = snapshot.data()?["claimedByUserId"] as? String
            if existingClaim == uid {
                transaction.updateData([
                    "claimedByUserId": FieldValue.delete(),
                    "claimedByUserName": FieldValue.delete(),
                    "claimedByDeviceId": FieldValue.delete(),
                    "isAwakeToday": false,
                    "lastUpdatedAt": FieldValue.serverTimestamp()
                ], forDocument: docRef)
                return true
            }
            return false
        })
    }
}
