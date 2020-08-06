package com.didahdx.smsgatewaysync.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.WorkManager
import com.didahdx.smsgatewaysync.broadcastReceivers.ConnectionReceiver
import com.didahdx.smsgatewaysync.presentation.UiUpdaterInterface
import com.didahdx.smsgatewaysync.printerlib.WoosimPrnMng
import com.didahdx.smsgatewaysync.rabbitMq.RabbitMqRunnable
import com.didahdx.smsgatewaysync.rabbitMq.RabbitmqClient
import com.didahdx.smsgatewaysync.util.*
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.didahdx.smsgatewaysync.work.WorkerUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class AppServices : Service(), UiUpdaterInterface {

    private var userLatitude = ""
    private var userLongitude = ""
    private val user = FirebaseAuth.getInstance().currentUser
    private val statusIntent = Intent(STATUS_INTENT_BROADCAST_RECEIVER)
    private var mPrnMng: WoosimPrnMng? = null

     lateinit var rabbitmqClient: RabbitmqClient
    @AddTrace(name = "AppServicesOnCreate", enabled = true /* optional */)
    override fun onCreate() {
        super.onCreate()
        toast("Service started")
        val email=user?.email ?: NOT_AVAILABLE
       rabbitmqClient=RabbitmqClient(this, email)

        val notification = NotificationUtil.notificationStatus(this, "Service starting", false)
        startForeground(1, notification)
//        val rabbitMqRunnable = user?.email?.let { RabbitMqRunnable(this, it, this,true) }
//        Thread(rabbitMqRunnable).start()
        registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
        registerReceiver(locationBroadcastReceiver, IntentFilter(LOCATION_UPDATE_INTENT))
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
        val input = intent?.getStringExtra(INPUT_EXTRAS) ?:"Initializing service"
        setRestartServiceState(this, true)
        setServiceState(this, ServiceState.STARTING)
        val rabbitMqRunnable = user?.email?.let { RabbitMqRunnable( rabbitmqClient,
            this, it, this,true) }
        Thread(rabbitMqRunnable).start()
        val notification = NotificationUtil.notificationStatus(this, input, false)
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
            val status= SpUtil.getPreferenceString(this, PREF_STATUS_MESSAGE, ERROR_CONNECTING_TO_SERVER)
                ?: ERROR_CONNECTING_TO_SERVER
            Intent(applicationContext, this.javaClass).also {
                it.action = AppServiceActions.START.name
                it.setPackage(packageName)
                it.putExtra(INPUT_EXTRAS, status)
                ContextCompat.startForegroundService(this, it)
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onLowMemory() {
        //Send broadcast to the Activity to kill this service and restart it.
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

        cancelPing()
        AppLog.logMessage("Sms Service stopped", this)
//        val rabbitMqRunnable = user?.email?.let { RabbitMqRunnable( rabbitmqClient,this,
//            it, this,false) }
//        Thread(rabbitMqRunnable).start()
        toast("Service destroyed")
        CoroutineScope(IO).cancel()
    }

    private fun cancelPing() {
        WorkManager.getInstance(this).cancelAllWorkByTag(SendRabbitMqWorker.PING_WORK_NAME)
    }
}