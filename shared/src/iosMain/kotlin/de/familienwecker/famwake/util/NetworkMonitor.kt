package de.familienwecker.famwake.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_get_status
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.darwin.DISPATCH_QUEUE_SERIAL

class IOSNetworkMonitor : NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var monitor: nw_path_monitor_t = null
    // Create a serial background queue for the path monitor
    private val monitorQueue: dispatch_queue_t = dispatch_queue_create("de.familienwecker.famwake.NetworkMonitorQueue", null)

    override fun startMonitoring() {
        monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor, monitorQueue)
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            _isOnline.value = (status == nw_path_status_satisfied)
        }
        nw_path_monitor_start(monitor)
    }

    override fun stopMonitoring() {
        monitor?.let {
            nw_path_monitor_cancel(it)
            monitor = null
        }
    }
}

actual fun createNetworkMonitor(context: Any?): NetworkMonitor {
    return IOSNetworkMonitor()
}
