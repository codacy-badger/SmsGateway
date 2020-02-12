package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.didahdx.smsgatewaysync.App

/**
 * used to check the network connection
 * **/
class ConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isConnected = checkConnection(context)
        if (connectionReceiverListener != null) {
            connectionReceiverListener?.onNetworkConnectionChanged(isConnected)
        }
    }

    interface ConnectionReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }

    companion object {

        var connectionReceiverListener: ConnectionReceiverListener? = null
        val isConnected: Boolean
            get() {
                val connectionManager = App.instance.
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                val activeNetwork = connectionManager.activeNetworkInfo
                return (activeNetwork != null && activeNetwork.isConnectedOrConnecting)
            }
    }


    private fun checkConnection(context: Context): Boolean {
        val connectionManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectionManager.activeNetworkInfo
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting)
    }
}
