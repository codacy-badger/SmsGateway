package com.didahdx.smsgatewaysync.utilities

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_LIGHTS
import androidx.core.app.NotificationCompat.DEFAULT_VIBRATE
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.presentation.activities.MainActivity
import java.util.*

object NotificationUtil {
    private var importantSmsNotification = 2

   private fun getNotificationManager(context: Context):NotificationManagerCompat{
        return NotificationManagerCompat.from(context)
    }

    fun notificationMessage(
        message: String,
        phoneNumber: String,
        context: Context,
        userLatitude: String,
        userLongitude: String
    ) {
        val smsInfo = SmsInfo(
            message,
            Date().toString(),
            phoneNumber,
            NOT_AVAILABLE,
            userLongitude,
            userLatitude
        )
        val bundle = bundleOf("SmsInfo" to smsInfo)
        val pendingIntent = NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph2)
            .setDestination(R.id.smsDetailsFragment)
            .setArguments(bundle)
            .createPendingIntent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_3)
            .setContentTitle(phoneNumber)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_message)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
//                    .setSummaryText(phoneNumber)
                    .setBigContentTitle(phoneNumber)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setDefaults(DEFAULT_LIGHTS and DEFAULT_VIBRATE)
            .build()

        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
        notification.flags = Notification.FLAG_AUTO_CANCEL

        getNotificationManager(context).notify(importantSmsNotification, notification)
        importantSmsNotification++
    }


    fun cancel(context: Context) {
        val nm = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        nm.cancel()
    }
}