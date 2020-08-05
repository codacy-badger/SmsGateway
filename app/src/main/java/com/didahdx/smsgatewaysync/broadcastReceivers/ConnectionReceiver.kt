package com.didahdx.smsgatewaysync.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.didahdx.smsgatewaysync.util.*


/**
 * used to check the network connection
 * **/
class ConnectionReceiver : BroadcastReceiver() {
    private val newIntent = Intent(CONNECTION_LOCAL_BROADCAST_RECEIVER)
    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connection=Connectivity.getConnectionType(context)
            AppLog.logMessage(connection,context)
            context.toast(connection)
            newIntent.putExtra("con", connection)
        }
    }
}
