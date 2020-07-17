package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.utilities.ServiceState.*
import com.google.firebase.auth.FirebaseAuth

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val user = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
            val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
            if (user.isNotEmpty() && user != NOT_AVAILABLE && getRestartServiceState(context)
                && isServiceOn) {
                Intent(context, AppServices::class.java).also {
                    it.action = AppServiceActions.START.name
                    it.putExtra(INPUT_EXTRAS, "Service is starting")
                    ContextCompat.startForegroundService(context, it)
                }
            }
        }
    }

}
