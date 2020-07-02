package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.didahdx.smsgatewaysync.domain.LogFormat
import com.didahdx.smsgatewaysync.utilities.ANDROID_PHONE
import com.didahdx.smsgatewaysync.utilities.AppLog
import com.didahdx.smsgatewaysync.utilities.NOT_AVAILABLE
import com.didahdx.smsgatewaysync.utilities.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import timber.log.Timber
import java.util.*

/**
 * used to check the network connection
 * **/
class ConnectionReceiver : BroadcastReceiver() {
    private val DEBUG_TAG = "NetworkStatusExample"

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {

            val connectionManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            var isWifiConn = false
            var isMobileConn = false
            connectionManager.allNetworks.forEach { network ->
                connectionManager.getNetworkInfo(network)?.apply {
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            isWifiConn = isWifiConn or isConnected
                            AppLog.logMessage("Connected to Wifi",context)
                            context.toast("Connected to Wifi")
                        }
                        ConnectivityManager.TYPE_MOBILE -> {
                            AppLog.logMessage("Connected to Mobile data",context)
                            context.toast(" Connected to Mobile data")
                            isMobileConn = isMobileConn or isConnected
                        }
                    }
                    val activeNetwork = connectionManager.activeNetworkInfo
                    if (!(activeNetwork != null && activeNetwork.isConnectedOrConnecting)) {
                        AppLog.logMessage("Connection lost",context)
                        context.toast("Connection lost")
                    }
                }
            }
            Timber.d("Wifi connected: $isWifiConn")
            Timber.d("Mobile connected: $isMobileConn")

        }
    }
}
