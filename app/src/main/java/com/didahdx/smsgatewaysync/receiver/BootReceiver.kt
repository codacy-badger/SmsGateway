package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.utilities.ServiceState.*

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
//            Intent(context, AppServices::class.java).also {
//                it.action = AppServiceActions.START.name
//                it.putExtra(INPUT_EXTRAS, "$APP_NAME is Running")
//                ContextCompat.startForegroundService(context, it)
//            }
        }
    }

}
