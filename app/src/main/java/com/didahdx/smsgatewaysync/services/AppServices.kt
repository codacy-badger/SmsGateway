package com.didahdx.smsgatewaysync.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
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
            .setContentTitle(getString(R.string.app_name))
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_home)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()



            startForeground(1,notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
    override fun onLowMemory() { //Send broadcast to the Activity to kill this service and restart it.
        super.onLowMemory()
    }


}