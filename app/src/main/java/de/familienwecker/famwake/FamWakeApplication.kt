package de.familienwecker.famwake

import android.app.Application
import de.familienwecker.famwake.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.room.RoomDatabase

/**
 * Application-Klasse, die Singletons für Repositories bereitstellt.
 * Stellt sicher, dass [PreferencesRepository] nur einmal instanziiert wird,
 * da es einen SharedPreferences-Listener hält.
 */
class FamWakeApplication : Application() {


    val appSettings: de.familienwecker.famwake.data.AppSettings by lazy {
        val settings = de.familienwecker.famwake.data.SettingsFactory(this).createSettings()
        de.familienwecker.famwake.data.AppSettingsImpl(settings)
    }

    val memberRepository: de.familienwecker.famwake.data.MemberRepository by lazy {
        val db = de.familienwecker.famwake.db.getDatabaseBuilder(this)
            .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
            .build()
        de.familienwecker.famwake.data.MemberRepository(db.memberDao())
    }


    override fun onCreate() {
        super.onCreate()
        // O7: Persistente Firestore-Offline-Persistenz konfigurieren – muss VOR dem ersten Firestore-Zugriff sein
        de.familienwecker.famwake.data.FirebaseRepository.configurePersistentCache()
        // S2: Debug-Logging im shared-Modul an BuildConfig koppeln
        de.familienwecker.famwake.data.FirebaseRepository.debugLogging = BuildConfig.DEBUG
        // Installations-Zeitstempel setzen (nur beim allerersten Start)
        if (appSettings.installTime.value == 0L) {
            appSettings.setInstallTime(System.currentTimeMillis())
        }

        // Ersten Start erkennen: "system"-Sentinel zur Gerätesprache auflösen und persistent speichern.
        // Dialekte (gsw/ksh/swg) können vom Gerät nicht erkannt werden → nur Hauptsprachen prüfen.
        // Alle nicht unterstützten Sprachen fallen auf EN zurück.
        if (appSettings.language.value == "system") {
            val deviceLang = java.util.Locale.getDefault().language
            val supportedMainCodes = de.familienwecker.famwake.data.AppSettingsImpl.SUPPORTED_LANGUAGE_CODES
                .filter { it.length == 2 } // nur ISO 639-1 Codes, keine Dialekte oder "system"
            val resolved = if (deviceLang in supportedMainCodes) deviceLang else "en"
            appSettings.setLanguage(resolved)
        }
        // Initialize RevenueCat
        if (BuildConfig.DEBUG) {
            android.util.Log.d("FamWakeDonation", "Initializing RevenueCat...")
        }
        if (BuildConfig.REVENUECAT_PUBLIC_API_KEY.isEmpty()) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e("FamWakeDonation", "RevenueCat API Key is empty! Please check local.properties or GitHub Secrets.")
        }
            return
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // N2: logLevel ersetzt das deprecated debugLogsEnabled
                com.revenuecat.purchases.Purchases.logLevel =
                    if (BuildConfig.DEBUG) com.revenuecat.purchases.LogLevel.DEBUG
                    else com.revenuecat.purchases.LogLevel.WARN
                val currentLang = appSettings.language.value
                val fullLocale = when (currentLang) {
                    "de" -> "de-DE"
                    "en" -> "en-US"
                    "es" -> "es-ES"
                    "fr" -> "fr-FR"
                    "it" -> "it-IT"
                    "sv" -> "sv-SE"
                    "system", "" -> java.util.Locale.getDefault().let { "${it.language}-${it.country}" }
                    // Unknown codes fall back to English
                    else -> "en-US"
                }
                com.revenuecat.purchases.Purchases.configure(
                    com.revenuecat.purchases.PurchasesConfiguration.Builder(
                        this@FamWakeApplication,
                        BuildConfig.REVENUECAT_PUBLIC_API_KEY
                    )
                    .preferredUILocaleOverride(fullLocale)
                    .build()
                )
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("FamWakeDonation", "RevenueCat initialized with key from BuildConfig")
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.e("FamWakeDonation", "RevenueCat initialization failed: ${e.message}")
                }
            }
        }
    }
}
