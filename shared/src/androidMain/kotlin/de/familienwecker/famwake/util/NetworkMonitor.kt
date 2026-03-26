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
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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
                _isOnline.value = false
            }
        }
    }

    override fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}
    }

    override fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
            offlineDebounceJob?.cancel()
        } catch (_: Exception) {}
    }
}

actual fun createNetworkMonitor(context: Any?): NetworkMonitor {
    val ctx = context as? Context ?: throw IllegalArgumentException("Android Context required for createNetworkMonitor")
    return AndroidNetworkMonitor(ctx)
}
