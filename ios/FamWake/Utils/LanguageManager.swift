import Foundation

/// Verwaltet die App-Sprache zur Laufzeit.
/// Wenn der Nutzer eine andere Sprache wählt, wird ein neues Bundle geladen
/// und L.bundle zeigt darauf – alle L.xxx-Properties liefern dann sofort die neue Sprache.
final class LanguageManager {
    static let shared = LanguageManager()

    private(set) var bundle: Bundle = .main

    var currentLocale: Locale {
        let saved = UserDefaults.standard.string(forKey: "language") ?? "system"
        let code = resolvedCode(saved)
        if code == "gsw" {
            return Locale(identifier: "gsw_CH")
        } else if code == "swg" || code == "ksh" {
            return Locale(identifier: "de_DE")
        }
        return Locale(identifier: code)
    }

    private init() {
        let saved = UserDefaults.standard.string(forKey: "language") ?? "system"
        apply(saved)
    }

    func apply(_ langCode: String) {
        let code = resolvedCode(langCode)
        // Primär: path-basierter Lookup
        if let path = Bundle.main.path(forResource: code, ofType: "lproj"),
           let b = Bundle(path: path) {
            bundle = b
            return
        }
        // Fallback: URL-basierter Lookup (robuster auf neueren iOS-Versionen)
        if let url = Bundle.main.url(forResource: code, withExtension: "lproj"),
           let b = Bundle(url: url) {
            bundle = b
            return
        }
        bundle = .main
    }

    // Löst "system", Sprachvarianten und Dialekte in den tatsächlichen Bundle-Code auf
    private func resolvedCode(_ code: String) -> String {
        var raw = code
        if raw == "system" {
            let preferred = Locale.preferredLanguages.first ?? "en"
            raw = String(preferred.prefix(2)).lowercased()
        }
        if raw == "no" || raw == "nb" { return "nb" }
        if raw == "in" || raw == "id" { return "id" }
        if raw.hasPrefix("zh") { return "zh-Hans" }
        return supported.contains(raw) ? raw : "en"
    }

    private let baseCodes    = ["en", "da", "de", "es", "fr", "it", "ja", "ko", "nl", "no", "nb", "pl", "pt", "ru", "sv", "tr", "uk", "zh", "id", "vi", "bn", "mr", "hi"]
    private let dialectCodes = ["gsw", "swg", "ksh"]
    private let supported    = ["en", "da", "de", "es", "fr", "it", "ja", "ko", "nl", "no", "nb", "pl", "pt", "ru", "sv", "tr", "uk", "zh", "id", "vi", "bn", "mr", "hi", "gsw", "swg", "ksh"]
}
