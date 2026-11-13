package de.familienwecker.famwake.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        _isOnline.value = checkCurrentConnectivity()
    }

    private fun checkCurrentConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private var offlineDebounceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            offlineDebounceJob?.cancel()
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            offlineDebounceJob?.cancel()
            offlineDebounceJob = scope.launch {
                delay(3000)
                // Erneut prüfen, ob zwischenzeitlich ein anderes Netzwerk (z.B. Mobilfunk nach WLAN-Verlust) aktiv ist
                if (!checkCurrentConnectivity()) {
                    _isOnline.value = false
                }
            }
        }
    }

    override fun startMonitoring() {
        _isOnline.value = checkCurrentConnectivity()
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            android.util.Log.w("NetworkMonitor", "registerNetworkCallback failed: ${e.message}")
        }
    }

    override fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
            offlineDebounceJob?.cancel()
        } catch (e: Exception) {
            android.util.Log.w("NetworkMonitor", "unregisterNetworkCallback failed: ${e.message}")
        }
    }
}

actual fun createNetworkMonitor(context: Any?): NetworkMonitor {
    val ctx = context as? Context ?: throw IllegalArgumentException("Android Context required for createNetworkMonitor")
    return AndroidNetworkMonitor(ctx)
}
