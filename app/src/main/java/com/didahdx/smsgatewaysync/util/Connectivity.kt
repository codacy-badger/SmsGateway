package com.didahdx.smsgatewaysync.util

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.didahdx.smsgatewaysync.R
import timber.log.Timber

object Connectivity {

    fun getConnectionType(context: Context): String {
        val connectionManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var connectionType = context.getString(R.string.connection_lost)
        val isConnected = false
        var isWifiConn = false
        var isMobileConn = false
        connectionManager.allNetworks.forEach { network ->
            connectionManager.getNetworkInfo(network)?.apply {
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> {
                        isWifiConn = isWifiConn or isConnected
                        connectionType = context.getString(R.string.Wifi)
                    }

                    ConnectivityManager.TYPE_MOBILE -> {
                        isMobileConn = isMobileConn or isConnected
                        connectionType =context.getString(R.string.mobile_data)
                    }
                }
                val activeNetwork = connectionManager.activeNetworkInfo
                if (!(activeNetwork != null && activeNetwork.isConnectedOrConnecting)) {
                    connectionType = context.getString(R.string.connection_lost)
                }
            }
        }

        val isWifiOnly =
            SpUtil.getPreferenceBoolean(context, context.getString(R.string.preference_wifi_only))
        if (isWifiOnly) {
            val networkManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifi = networkManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (wifi?.isAvailable!!  && wifi.isConnected) {
                connectionType=context.getString(R.string.Wifi)
            } else {
                connectionType=context.getString(R.string.connection_lost)
            }
        }

        Timber.d("Wifi connected: $isWifiConn")
        Timber.d("Mobile connected: $isMobileConn")
        Timber.d("Connection type: $connectionType")

        return connectionType
    }
}