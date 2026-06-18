package de.familienwecker.famwake

import android.app.Application
import de.familienwecker.famwake.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Application-Klasse, die Singletons für Repositories bereitstellt.
 * Stellt sicher, dass [PreferencesRepository] nur einmal instanziiert wird,
 * da es einen SharedPreferences-Listener hält.
 */
class FamWakeApplication : Application() {

    companion object {
        lateinit var instance: FamWakeApplication
            private set
    }

    val appSettings: de.familienwecker.famwake.data.AppSettings by lazy {
        val settings = de.familienwecker.famwake.data.SettingsFactory(this).createSettings()
        de.familienwecker.famwake.data.AppSettingsImpl(settings)
    }

    val memberRepository: de.familienwecker.famwake.data.MemberRepository by lazy {
        val db = de.familienwecker.famwake.db.getDatabaseBuilder(this)
            .fallbackToDestructiveMigration()
            .build()
        de.familienwecker.famwake.data.MemberRepository(db.memberDao())
    }

    val firebaseRepository: de.familienwecker.famwake.data.FirebaseRepository by lazy {
        de.familienwecker.famwake.data.FirebaseRepository()
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        // O7: Persistente Firestore-Offline-Persistenz konfigurieren – muss VOR dem ersten Firestore-Zugriff sein
        de.familienwecker.famwake.data.FirebaseRepository.configurePersistentCache()
        // S2: Debug-Logging im shared-Modul an BuildConfig koppeln
        de.familienwecker.famwake.data.FirebaseRepository.debugLogging = BuildConfig.DEBUG
        // Push: Notification Channels einmalig registrieren (Android 8+, idempotent)
        NotificationChannels.register(this)

        // Ausstehende Firestore-Writes (z.B. lastModifiedByUid) können nach einem
        // Familienwechsel mit PERMISSION_DENIED abgelehnt werden → dies ist erwartet
        // und darf nicht crashen. Echte Crashes werden weiter an den System-Handler geleitet.
        val systemHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isFirestorePermissionDenied = throwable is com.google.firebase.firestore.FirebaseFirestoreException &&
                throwable.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            if (isFirestorePermissionDenied) {
                if (BuildConfig.DEBUG) android.util.Log.w("FamWakeApp", "Ignoring expected PERMISSION_DENIED after family leave: ${throwable.message}")
            } else {
                systemHandler?.uncaughtException(thread, throwable)
            }
        }
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
        kotlinx.coroutines.MainScope().launch {
            try {
                // N2: logLevel ersetzt das deprecated debugLogsEnabled
                com.revenuecat.purchases.Purchases.logLevel =
                    if (BuildConfig.DEBUG) com.revenuecat.purchases.LogLevel.DEBUG
                    else com.revenuecat.purchases.LogLevel.WARN
                val currentLang = appSettings.language.value
                val fullLocale = when (currentLang) {
                    "da" -> "da-DK"
                    "de" -> "de-DE"
                    "en" -> "en-US"
                    "es" -> "es-ES"
                    "fr" -> "fr-FR"
                    "it" -> "it-IT"
                    "ja" -> "ja-JP"
                    "ko" -> "ko-KR"
                    "nl" -> "nl-NL"
                    "no" -> "nb-NO"
                    "pl" -> "pl-PL"
                    "pt" -> "pt-BR"
                    "ru" -> "ru-RU"
                    "sv" -> "sv-SE"
                    "tr" -> "tr-TR"
                    "uk" -> "uk-UA"
                    "zh" -> "zh-CN"
                    "id" -> "id-ID"
                    "vi" -> "vi-VN"
                    "bn" -> "bn-BD"
                    "mr" -> "mr-IN"
                    "hi" -> "hi-IN"
                    "system", "" -> java.util.Locale.getDefault().let { "${it.language}-${it.country}" }
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
