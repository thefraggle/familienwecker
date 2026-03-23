package de.familienwecker.famwake

import android.app.Application
import de.familienwecker.famwake.data.PreferencesRepository
import de.familienwecker.famwake.BuildConfig

/**
 * Application-Klasse, die Singletons für Repositories bereitstellt.
 * Stellt sicher, dass [PreferencesRepository] nur einmal instanziiert wird,
 * da es einen SharedPreferences-Listener hält.
 */
class FamWakeApplication : Application() {

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(this)
    }

    companion object {
        lateinit var instance: FamWakeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize RevenueCat
        android.util.Log.d("FamWakeDonation", "Initializing RevenueCat...")
        try {
            com.revenuecat.purchases.Purchases.debugLogsEnabled = true
            com.revenuecat.purchases.Purchases.configure(
                com.revenuecat.purchases.PurchasesConfiguration.Builder(
                    this,
                    BuildConfig.REVENUECAT_PUBLIC_API_KEY
                ).build()
            )
            android.util.Log.d("FamWakeDonation", "RevenueCat initialized with key from BuildConfig")
        } catch (e: Exception) {
            android.util.Log.e("FamWakeDonation", "RevenueCat initialization failed: ${e.message}")
        }
    }
}
