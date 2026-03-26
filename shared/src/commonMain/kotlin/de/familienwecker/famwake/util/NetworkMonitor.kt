package de.familienwecker.famwake.util

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
}

expect fun createNetworkMonitor(context: Any? = null): NetworkMonitor
