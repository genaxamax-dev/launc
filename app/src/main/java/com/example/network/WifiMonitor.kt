package com.example.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.example.model.WifiConnectionStatus
import com.example.model.WifiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _wifiState = MutableStateFlow(readCurrentState())
    val wifiState: StateFlow<WifiState> = _wifiState.asStateFlow()

    private var started = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refresh()
        }

        override fun onLost(network: Network) {
            refresh()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            refresh()
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    fun start() {
        if (started) return
        started = true
        refresh()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        runCatching {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(context, wifiReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        runCatching { context.unregisterReceiver(wifiReceiver) }
    }

    fun refresh() {
        _wifiState.value = readCurrentState()
    }

    @Suppress("DEPRECATION")
    private fun readCurrentState(): WifiState {
        val isEnabled = runCatching { wifiManager.isWifiEnabled }.getOrDefault(false)
        if (!isEnabled) {
            return WifiState(status = WifiConnectionStatus.DISABLED, signalLevel = 0, linkSpeedMbps = 0)
        }

        val hasWifiTransport = runCatching {
            val active = connectivityManager.activeNetwork
            val caps = active?.let { connectivityManager.getNetworkCapabilities(it) }
            if (caps != null) {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                val info = connectivityManager.activeNetworkInfo
                info != null && info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
            }
        }.getOrDefault(false)

        if (!hasWifiTransport) {
            return WifiState(
                status = WifiConnectionStatus.ENABLED_NO_CONNECTION,
                signalLevel = 0,
                linkSpeedMbps = 0
            )
        }

        var level = 3
        var linkSpeed = 0
        runCatching {
            val connInfo = wifiManager.connectionInfo
            if (connInfo != null) {
                level = WifiManager.calculateSignalLevel(connInfo.rssi, 5).coerceIn(0, 4)
                linkSpeed = connInfo.linkSpeed.coerceAtLeast(0)
            }
        }

        return WifiState(
            status = WifiConnectionStatus.CONNECTED,
            signalLevel = level,
            linkSpeedMbps = linkSpeed
        )
    }
}
