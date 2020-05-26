package com.didahdx.smsgatewaysync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.ui.home.HomeViewModelFactory
import com.didahdx.smsgatewaysync.ui.smsInbox.SmsInboxViewModelFactory
import com.didahdx.smsgatewaysync.utilities.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import timber.log.Timber

class App : MultiDexApplication(), KodeinAware {


    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)
        MultiDex.install(this)
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        Timber.plant(Timber.DebugTree())


    }

    companion object {
        @get:Synchronized
        lateinit var instance: App
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_SMS_SERVICE_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            val updateNotificationChannel = NotificationChannel(
                CHANNEL_ID_2,
                CHANNEL_CLIENT_NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            val smsNotificationChannel = NotificationChannel(
                CHANNEL_ID_3,
                CHANNEL_IMPORTANT_SMS_NOTIFICATION,
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager: NotificationManager? = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notificationChannel)
            manager?.createNotificationChannel(updateNotificationChannel)
            manager?.createNotificationChannel(smsNotificationChannel)
        }
    }
    override val kodein = Kodein.lazy {
        import(androidXModule(this@App))

        bind() from singleton { MessagesDatabase(instance()) }
        bind() from provider {
            HomeViewModelFactory(
                instance(),
                instance()
            )
        }
        bind() from provider {
            SmsInboxViewModelFactory(
                instance()
            )
        }
    }
}