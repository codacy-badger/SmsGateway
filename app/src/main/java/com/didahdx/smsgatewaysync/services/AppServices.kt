package com.didahdx.smsgatewaysync.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.domain.LogFormat
import com.didahdx.smsgatewaysync.manager.RabbitMqRunnable
import com.didahdx.smsgatewaysync.presentation.UiUpdaterInterface
import com.didahdx.smsgatewaysync.presentation.activities.MainActivity
import com.didahdx.smsgatewaysync.printerlib.WoosimPrnMng
import com.didahdx.smsgatewaysync.broadcastReceivers.ConnectionReceiver
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.didahdx.smsgatewaysync.work.WorkerUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.metrics.AddTrace
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class AppServices : Service(), UiUpdaterInterface {
    private lateinit var notificationManager: NotificationManagerCompat

    private var userLatitude = ""
    private var userLongitude = ""
    private val user = FirebaseAuth.getInstance().currentUser

    private val statusIntent = Intent(STATUS_INTENT_BROADCAST_RECEIVER)
    private var mPrnMng: WoosimPrnMng? = null
    private var wakeLock: PowerManager.WakeLock? = null
    lateinit var notification: Notification

    //    lateinit var rabbitmqClient: RabbitmqClient
    @AddTrace(name = "AppServicesOnCreate", enabled = true /* optional */)
    override fun onCreate() {
        super.onCreate()
        toast("Service started")
        val notification = NotificationUtil.notificationStatus(this, "Service starting", false)
        startForeground(1, notification)
        AppLog.logMessage("Sms Service started", this,true)


        val rabbitMqRunnable = user?.email?.let { RabbitMqRunnable(this, it, this) }
        Thread(rabbitMqRunnable).start()

        CoroutineScope(IO).launch {
            cancelPing()
            setupRabbitMqPing()
        }

        registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        registerReceiver(locationBroadcastReceiver, IntentFilter(LOCATION_UPDATE_INTENT))

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                AppServices::class.java.simpleName
            ).apply {
                acquire(10 * 60 * 1000L /*4 minutes*/)
            }
        }
    }

    @AddTrace(name = "AppServiceStartCommand", enabled = true /* optional */)
    override fun onStartCommand(intent: Intent?, flagings: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                AppServiceActions.START.name -> startAppService(intent)
                AppServiceActions.STOP.name -> stopAppService(intent)
                else -> {
                    Timber.d("This should never happen. No action in the received intent")
                }
            }
        } else {
            Timber.d(" with a null intent. It has been probably restarted by the system.")
        }

        return START_REDELIVER_INTENT
    }

    private fun stopAppService(intent: Intent) {
        setServiceState(this, ServiceState.STOPPED)
        // Stop foreground service and remove the notification.
        stopForeground(true)

        // Stop the foreground service.
        stopSelf()
    }

    private fun startAppService(intent: Intent?) {
        val input = intent?.getStringExtra(INPUT_EXTRAS) ?: " "
        setRestartServiceState(this, true)
        setServiceState(this, ServiceState.STARTING)
//        notificationStatus(input, false)
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )

        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_home)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
//            .setOnlyAlertOnce(true)
            .build()

        notification.flags = notification.flags or Notification.DEFAULT_LIGHTS
        notification.flags = notification.flags or Notification.DEFAULT_VIBRATE
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE

        startForeground(1, notification)
        toast("startForeground called")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val isServiceOn = SpUtil.getPreferenceBoolean(this, PREF_SERVICES_KEY)
        if (getRestartServiceState(this) && isServiceOn) {
            toast("Restart service state ${getRestartServiceState(this)}")
            setServiceState(this, ServiceState.STARTING)
            Intent(applicationContext, this.javaClass).also {
                it.action = AppServiceActions.START.name
                it.setPackage(packageName)
                ContextCompat.startForegroundService(this, it)
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onLowMemory() { //Send broadcast to the Activity to kill this service and restart it.
        super.onLowMemory()
    }


    private val locationBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (LOCATION_UPDATE_INTENT == intent.action) {
                val longitude = intent.getStringExtra(LONGITUDE_EXTRA)
                val latitude = intent.getStringExtra(LATITUDE_EXTRA)
                val altitude = intent.getStringExtra(ALTITUDE_EXTRA)
                latitude?.let { userLatitude = it }
                longitude?.let { userLongitude = it }

//                context.toast("Gps/Network Location  $userLatitude  $userLongitude $altitude")
                Timber.d("Received Gps/Network Location  $userLatitude  $userLongitude $altitude")
            }
        }
    }

    override fun notificationMessage(message: String) {
        NotificationUtil.updateNotificationStatus(this, message, true)
        statusIntent.putExtra(STATUS_MESSAGE_EXTRA, message)
        statusIntent.putExtra(STATUS_COLOR_EXTRA, GREEN_COLOR)
        sendBroadcast(statusIntent)
    }

    override fun toasterMessage(message: String) {
        Timber.d("called $message")
        Timber.d("thread name ${Thread.currentThread().name}")
        CoroutineScope(Main).launch {
            toast(message)
            Timber.d("thread name ${Thread.currentThread().name}")
        }
    }

    @AddTrace(name = "AppServiceUpdateStatusViewWith")
    override fun updateStatusViewWith(status: String, color: String) {
        val context = this
        SpUtil.setPreferenceString(context, PREF_STATUS_MESSAGE, status)
        SpUtil.setPreferenceString(context, PREF_STATUS_COLOR, color)
        Timber.d("thread name ${Thread.currentThread().name}")

        notificationManager = NotificationManagerCompat.from(context)
        val isServiceOn = SpUtil.getPreferenceBoolean(this@AppServices, PREF_SERVICES_KEY)
        if (isServiceOn) {
            statusIntent.putExtra(STATUS_MESSAGE_EXTRA, status)
            statusIntent.putExtra(STATUS_COLOR_EXTRA, color)
            sendBroadcast(statusIntent)
            AppLog.logMessage(status, context,true)
            NotificationUtil.updateNotificationStatus(this, status, false)
        }

    }

    override fun sendSms(phoneNumber: String, message: String) {
        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, message)
            .putString(KEY_PHONE_NUMBER, phoneNumber)
            .build()

        WorkerUtil.sendSms(data,this)
    }

    override fun updateSettings(preferenceType: String, key: String, value: String) {
        SpUtil.updateSettingsRemotely(this, preferenceType, key, value)
    }

    override fun logMessage(message: String) {
        AppLog.logMessage(message, this)
    }

    @AddTrace(name = "AppServicesOnDestroy")
    override fun onDestroy() {
        mPrnMng?.releaseAllocatoins()
        super.onDestroy()
        setServiceState(this, ServiceState.STOPPED)
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        cancelPing()
        AppLog.logMessage("Sms Service stopped", this)
//        CoroutineScope(IO).launch {
//            rabbitmqClient.disconnect()
//        }

        toast("Service destroyed")
        CoroutineScope(IO).cancel()
    }

    @AddTrace(name="AppServicesSetupRabbitMqPing")
    private fun setupRabbitMqPing() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
        val logFormat = LogFormat(
            date = Date().toString(),
            type = "logs",
            client_gateway_type = ANDROID_PHONE,
            log = "ping",
            client_sender = email,
            isUserVisible = true,
            isUploaded =true
        )

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, Gson().toJson(logFormat))
            .putString(KEY_EMAIL, user?.email)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<SendRabbitMqWorker>(18, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        Timber.d("WorkManager: Periodic Work request for sync is scheduled")
        val isServiceOn = SpUtil.getPreferenceBoolean(this, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(SendRabbitMqWorker.PING_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest)
    }

    private fun cancelPing() {
        WorkManager.getInstance(this).cancelAllWorkByTag(SendRabbitMqWorker.PING_WORK_NAME)
    }
}