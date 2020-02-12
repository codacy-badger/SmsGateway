package com.didahdx.smsgatewaysync

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.multidex.MultiDexApplication
import androidx.multidex.MultiDex
import androidx.core.content.ContextCompat.getSystemService



class App : MultiDexApplication() {
    val channel_id="SmsServiceChannel"

     override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
            var notificationChannel=NotificationChannel(channel_id,
                "Sms service channel",
                NotificationManager.IMPORTANCE_DEFAULT)

            val manager : NotificationManager?= getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notificationChannel)

        }
    }
}