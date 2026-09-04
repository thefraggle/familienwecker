import XCTest
@testable import FamWake

final class FirebaseErrorMapperTests: XCTestCase {

    func testMap_notFoundErrors_returnsFamilyNotFound() {
        struct MockError: LocalizedError {
            var errorDescription: String?
        }

        let err1 = MockError(errorDescription: "Error: family_not_found in Firestore")
        XCTAssertEqual(FirebaseErrorMapper.map(err1), L.errorFamilyNotFound)

        let err2 = MockError(errorDescription: "Requested document was not-found")
        XCTAssertEqual(FirebaseErrorMapper.map(err2), L.errorFamilyNotFound)
    }

    func testMap_invalidCodeErrors_returnsInvalidCode() {
        struct MockError: LocalizedError {
            var errorDescription: String?
        }

        let err1 = MockError(errorDescription: "invalid-argument: Code does not exist")
        XCTAssertEqual(FirebaseErrorMapper.map(err1), L.errorInvalidCode)

        let err2 = MockError(errorDescription: "invalid_code format entered")
        XCTAssertEqual(FirebaseErrorMapper.map(err2), L.errorInvalidCode)
    }

    func testMap_networkErrors_returnsNetworkError() {
        struct MockError: LocalizedError {
            var errorDescription: String?
        }

        let err1 = MockError(errorDescription: "Network connection lost")
        XCTAssertEqual(FirebaseErrorMapper.map(err1), L.errorNetwork)

        let err2 = MockError(errorDescription: "Device is offline")
        XCTAssertEqual(FirebaseErrorMapper.map(err2), L.errorNetwork)
    }

    func testMap_unknownErrors_returnsGenericErrorWithDescription() {
        struct MockError: LocalizedError {
            var errorDescription: String?
        }

        let err = MockError(errorDescription: "Something weird happened")
        let result = FirebaseErrorMapper.map(err)
        XCTAssertTrue(result.contains(L.errorGeneric))
        XCTAssertTrue(result.contains("Something weird happened"))
    }
}
