import Foundation

/// Shared Firebase error mapper for Firestore/Functions errors.
/// Auth-specific errors use AuthViewModel's own mapper (uses AuthErrorCode).
enum FirebaseErrorMapper {
    static func map(_ error: Error) -> String {
        let msg = error.localizedDescription.lowercased()
        if msg.contains("family_not_found") || msg.contains("not-found") || msg.contains("not found") {
            return L.errorFamilyNotFound
        }
        if msg.contains("invalid-argument") || msg.contains("invalid_code") {
            return L.errorInvalidCode
        }
        if msg.contains("network") || msg.contains("offline") || msg.contains("internet") {
            return L.errorNetwork
        }
        return "\(L.errorGeneric) (\(error.localizedDescription))"
    }
}
