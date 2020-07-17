package com.didahdx.smsgatewaysync.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.domain.LogFormat
import com.didahdx.smsgatewaysync.manager.RabbitmqClient
import com.didahdx.smsgatewaysync.presentation.UiUpdaterInterface
import com.didahdx.smsgatewaysync.presentation.activities.MainActivity
import com.didahdx.smsgatewaysync.printerlib.WoosimPrnMng
import com.didahdx.smsgatewaysync.receiver.ConnectionReceiver
import com.didahdx.smsgatewaysync.receiver.SmsReceiver
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.work.SendApiWorker
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.didahdx.smsgatewaysync.work.SendSmsWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.metrics.AddTrace
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.TimeUnit

class AppServices : Service(), UiUpdaterInterface {
    private lateinit var notificationManager: NotificationManagerCompat

    private var userLatitude = ""
    private var userLongitude = ""
    private val user = FirebaseAuth.getInstance().currentUser

    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var callStartTime: Date? = null
    private var isIncoming = false
    private var savedNumber: String? = null //because the passed incoming is only valid in ringing
    private val newIntent = Intent(CALL_LOCAL_BROADCAST_RECEIVER)
    private val statusIntent = Intent(STATUS_INTENT_BROADCAST_RECEIVER)
    private var mPrnMng: WoosimPrnMng? = null
    private var wakeLock: PowerManager.WakeLock? = null
    lateinit var notification: Notification

    @Volatile
    var stopThread = false
//    lateinit var rabbitmqClient: RabbitmqClient

    @AddTrace(name = "AppServicesOnCreate", enabled = true /* optional */)
    override fun onCreate() {
        super.onCreate()
        toast("Service started")
        stopThread = false
        AppLog.logMessage("Sms Service started", this)
        val notification = NotificationUtil.notificationStatus(this, "Service starting", false)
        startForeground(1, notification)

        val rabbitMqRunnable = user?.email?.let { RabbitMqRunnable(this, it, this) }
        Thread(rabbitMqRunnable).start()

        CoroutineScope(IO).launch {
            setupRabbitmqPingingWork()
        }

        registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
        registerReceiver(SmsReceiver(), IntentFilter(SMS_RECEIVED_INTENT))
        registerReceiver(locationBroadcastReceiver, IntentFilter(LOCATION_UPDATE_INTENT))

        val phoneCallFilter = IntentFilter(PHONE_STATE)
        phoneCallFilter.addAction(NEW_OUTGOING_CALL)
        registerReceiver(phoneCallReceiver, phoneCallFilter)


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
        stopForeground(true);

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


    private val phoneCallReceiver = object : BroadcastReceiver() {
        //The receiver will be recreated whenever android feels like it.
        // We need a static variable to remember data between instantiations
        @AddTrace(name = "AppServicePhoneCallOnReceive", enabled = true /* optional */)
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
                savedNumber = intent.extras!!.getString("android.intent.extra.PHONE_NUMBER")
            } else {

                val stateStr =
                    intent.extras!!.getString(TelephonyManager.EXTRA_STATE)
                val number =
                    intent.extras!!.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
                var state = 0
                if (stateStr == TelephonyManager.EXTRA_STATE_IDLE) {
                    state = TelephonyManager.CALL_STATE_IDLE
                } else if (stateStr == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    state = TelephonyManager.CALL_STATE_OFFHOOK
                } else if (stateStr == TelephonyManager.EXTRA_STATE_RINGING) {
                    state = TelephonyManager.CALL_STATE_RINGING
                }

                Timber.d("phone receiver called $number")
                onCallStateChanged(context, state, number)
            }
        }
    }

    //Deals with actual events
    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    private fun onCallStateChanged(context: Context?, state: Int, number: String?) {

        Timber.d("caall called")
        if (lastState == state) {
            //No change, debounce extras
            return
        }
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                callStartTime = Date()
                savedNumber = number
                if (context != null) {
                    newIntent.putExtra(CALL_TYPE_EXTRA, "incomingCallReceived")
                    newIntent.putExtra(PHONE_NUMBER_EXTRA, number)
                    newIntent.putExtra(START_TIME_EXTRA, Date().toString())
                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
                    checkCall(context, "incomingCallReceived", number, Date().toString(), "")

                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK ->                 //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false
                    callStartTime = Date()
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onOutgoingCallStarted")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, Date().toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

                        checkCall(
                            context, "onOutgoingCallStarted",
                            savedNumber, Date().toString(), ""
                        )

                    }
                } else {
                    isIncoming = true
                    callStartTime = Date()
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onIncomingCallAnswered")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, Date().toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

                        checkCall(
                            context, "onIncomingCallAnswered",
                            savedNumber, Date().toString(), ""
                        )

                    }
                }
            TelephonyManager.CALL_STATE_IDLE ->                 //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onMissedCall")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, callStartTime.toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

                        checkCall(
                            context, "onMissedCall",
                            savedNumber, callStartTime.toString(), ""
                        )
                    }
                } else if (isIncoming) {
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onIncomingCallEnded")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, callStartTime.toString())
                        newIntent.putExtra(END_TIME_EXTRA, Date().toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

                        checkCall(
                            context, "onIncomingCallEnded",
                            savedNumber, callStartTime.toString(), Date().toString()
                        )

                    }
                } else {
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onOutgoingCallEnded")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, callStartTime.toString())
                        newIntent.putExtra(END_TIME_EXTRA, Date().toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

                        checkCall(
                            context, "onOutgoingCallEnded",
                            savedNumber, callStartTime.toString(), Date().toString()
                        )

                    }
                }
        }
        lastState = state
    }

    private fun checkCall(
        context: Context, callType: String, phoneNumber: String?,
        startTime: String, endTime: String
    ) {

        if (callType == "incomingCallReceived") {
            hangUpCall(context)
        }

        val obj: JSONObject? = JSONObject()
        obj?.put("type", "calls")
        obj?.put("longitude", userLongitude)
        obj?.put("latitude", userLatitude)
        obj?.put("client_sender", user?.email)
        obj?.put("client_gateway_type", "android_phone")
        obj?.put("call_type", callType)
        obj?.put("phone_number", phoneNumber)
        obj?.put("start_time", startTime)
        obj?.put("end_time", endTime)

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, obj.toString())
            .putString(KEY_EMAIL, user?.email)
            .build()
        sendToRabbitMQ(context, data)

    }


    //used to hang Up Phone Call
    private fun hangUpCall(context: Context) {
        val hangup = SpUtil.getPreferenceBoolean(this, PREF_HANG_UP) ?: false
        if (hangup) {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tm.endCall()
            } else {
                disconnectCall()
            }
        }
    }


    @SuppressLint("PrivateApi")
    private fun disconnectCall() {
        try {
            val serviceManagerName = "android.os.ServiceManager"
            val serviceManagerNativeName = "android.os.ServiceManagerNative"
            val telephonyName = "com.android.internal.telephony.ITelephony"
            val telephonyClass: Class<*>
            val telephonyStubClass: Class<*>
            val serviceManagerClass: Class<*>
            val serviceManagerNativeClass: Class<*>
            val telephonyEndCall: Method
            val telephonyObject: Any
            val serviceManagerObject: Any
            telephonyClass = Class.forName(telephonyName)
            telephonyStubClass = telephonyClass.classes[0]
            serviceManagerClass = Class.forName(serviceManagerName)
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName)
            val getService =  // getDefaults[29];
                serviceManagerClass.getMethod("getService", String::class.java)
            val tempInterfaceMethod = serviceManagerNativeClass.getMethod(
                "asInterface",
                IBinder::class.java
            )
            val tmpBinder = Binder()
            tmpBinder.attachInterface(null, "fake")
            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder)
            val retbinder = getService.invoke(serviceManagerObject, "phone") as IBinder
            val serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder::class.java)
            telephonyObject = serviceMethod.invoke(null, retbinder)
            telephonyEndCall = telephonyClass.getMethod("endCall")
            telephonyEndCall.invoke(telephonyObject)
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.i("FATAL ERROR: could not connect to telephony subsystem")
            Timber.i("Exception object: $e  ${e.localizedMessage}")
            AppLog.logMessage(" $e  ${e.localizedMessage}", this)
        }
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


    //used to show to notification
    private fun sendToRabbitMQ(context: Context, data: Data) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendRabbitMqWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(context).enqueue(request)
    }


    private fun sendSms(data: Data) {
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendSmsWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(this).enqueue(request)
    }

    private fun sendToApi(data: Data) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendApiWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(this).enqueue(request)
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
            AppLog.logMessage(status, context)
            NotificationUtil.updateNotificationStatus(this, status, false)
        }

    }

    override fun sendSms(phoneNumber: String, message: String) {
        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, message)
            .putString(KEY_PHONE_NUMBER, phoneNumber)
            .build()

        sendSms(data)
    }

    override fun updateSettings(preferenceType: String, key: String, value: String) {
        SpUtil.updateSettingsRemotely(this, preferenceType, key, value)
    }

    override fun logMessage(message: String) {
        AppLog.logMessage(message, this)
    }


    override fun onDestroy() {
        mPrnMng?.releaseAllocatoins()
        super.onDestroy()
        setServiceState(this, ServiceState.STOPPED)
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        stopThread = true
        cancelPinging()
        AppLog.logMessage("Sms Service stopped", this)
//        CoroutineScope(IO).launch {
//            rabbitmqClient.disconnect()
//        }

//        unregisterReceiver(ConnectionReceiver())
//        unregisterReceiver(SmsReceiver())
//        unregisterReceiver(locationBroadcastReceiver)
//        unregisterReceiver(phoneCallReceiver)

        toast("Service destroyed")
        CoroutineScope(IO).cancel()
    }

    @AddTrace(name = "AppServiceSetupPingingWork", enabled = true /* optional */)
    private fun setupRabbitmqPingingWork() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
        val logFormat = LogFormat(
            date = Date().toString(),
            type = "logs",
            client_gateway_type = ANDROID_PHONE,
            log = "pinging server",
            client_sender = email
        )

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, Gson().toJson(logFormat))
            .putString(KEY_EMAIL, user?.email)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<SendRabbitMqWorker>(20, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        Timber.d("WorkManager: Periodic Work request for sync is scheduled")
        val isServiceOn = SpUtil.getPreferenceBoolean(this, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SendRabbitMqWorker.PING_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
            )
    }

    fun cancelPinging() {
        WorkManager.getInstance(this).cancelAllWorkByTag(SendRabbitMqWorker.PING_WORK_NAME)
    }


    class RabbitMqRunnable(
        context: Context,
        email: String,
        uiUpdaterInterface: UiUpdaterInterface
    ) :
        Runnable {
        private val mContext = context
        private val mEmail = email
        private val updaterInterface = uiUpdaterInterface

        override fun run() {
            Timber.d(" ${Thread.currentThread().name} ")
            val rabbitMqClient = RabbitmqClient(updaterInterface, mEmail)
            val urlEnabled = SpUtil.getPreferenceBoolean(mContext, PREF_HOST_URL_ENABLED)
            val isServiceOn = SpUtil.getPreferenceBoolean(mContext, PREF_SERVICES_KEY)
            if (isServiceOn && !urlEnabled) {
                setServiceState(mContext, ServiceState.STARTING)
                rabbitMqClient.connection(mContext)
                Timber.d("The service has been created".toUpperCase(Locale.getDefault()))
            }
        }
    }
}