package com.didahdx.smsgatewaysync.receiver

import android.app.ApplicationErrorReport.TYPE_NONE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.didahdx.smsgatewaysync.App
import com.didahdx.smsgatewaysync.utilities.AppLog
import com.didahdx.smsgatewaysync.utilities.toast
import java.util.*

/**
 * used to check the network connection
 * **/
class ConnectionReceiver : BroadcastReceiver() {
    private val DEBUG_TAG = "NetworkStatusExample"
    val appLog = AppLog()
    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {

            val connectionManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var isConnect: Boolean = false
            var isWifiConn: Boolean = false
            var isMobileConn: Boolean = false
            connectionManager.allNetworks.forEach { network ->
                connectionManager.getNetworkInfo(network).apply {
                    context.toast(" $type ")
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            isWifiConn = isWifiConn or isConnected
                            val time = Date()
                            appLog.writeToLog(context, "\n $time \n Connected to Wifi \n")
                            context.toast("Connected to Wifi")
                        }
                        ConnectivityManager.TYPE_MOBILE -> {
                            val time = Date()
                            appLog.writeToLog(context, "\n $time \n Connected to Mobile data \n")
                            context.toast(" Connected to Mobile data")
                            isMobileConn = isMobileConn or isConnected
                        }
                    }

                    val activeNetwork = connectionManager.activeNetworkInfo
                    if (!(activeNetwork != null && activeNetwork.isConnectedOrConnecting)) {
                        context.toast("Connection lost")
                        val time = Date()
                        appLog.writeToLog(context, "\n $time \n Connection lost\n")
                    }
                }
            }
            Log.d(DEBUG_TAG, "Wifi connected: $isWifiConn")
            Log.d(DEBUG_TAG, "Mobile connected: $isMobileConn")

        }
    }
}
