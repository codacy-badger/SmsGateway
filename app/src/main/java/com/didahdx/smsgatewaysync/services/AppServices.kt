package com.didahdx.smsgatewaysync.services

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.didahdx.smsgatewaysync.MainActivity
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.CHANNEL_ID
import com.didahdx.smsgatewaysync.utilities.INPUT_EXTRAS

class AppServices :Service(){


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input=intent?.getStringExtra(INPUT_EXTRAS)

        val notificationIntent=Intent(this, MainActivity::class.java)
        val pendingIntent=PendingIntent.getActivity(this,
            0,notificationIntent,0)

        var notification=NotificationCompat.Builder(this,CHANNEL_ID)
            .setContentTitle("SmsGatewaySync")
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_home)
            .setContentIntent(pendingIntent)
            .build()

            startForeground(1,notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}