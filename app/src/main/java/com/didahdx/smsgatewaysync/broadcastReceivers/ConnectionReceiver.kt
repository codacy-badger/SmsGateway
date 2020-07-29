package com.didahdx.smsgatewaysync.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.didahdx.smsgatewaysync.util.AppLog
import com.didahdx.smsgatewaysync.util.Connectivity
import com.didahdx.smsgatewaysync.util.toast


/**
 * used to check the network connection
 * **/
class ConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connection=Connectivity.getConnectionType(context)
            AppLog.logMessage(connection,context)
            context.toast(connection)

        }
    }
}
