package de.familienwecker.famwake

import android.app.Application
import de.familienwecker.famwake.BuildConfig
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

    companion object {
        lateinit var instance: FamWakeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

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
        try {
            com.revenuecat.purchases.Purchases.debugLogsEnabled = BuildConfig.DEBUG
            val currentLang = appSettings.language.value
            val fullLocale = when (currentLang) {
                "de" -> "de-DE"
                "en" -> "en-US"
                "es" -> "es-ES"
                "fr" -> "fr-FR"
                "it" -> "it-IT"
                else -> currentLang
            }
            com.revenuecat.purchases.Purchases.configure(
                com.revenuecat.purchases.PurchasesConfiguration.Builder(
                    this,
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
