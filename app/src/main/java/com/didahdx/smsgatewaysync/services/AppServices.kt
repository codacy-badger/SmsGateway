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
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_LIGHTS
import androidx.core.app.NotificationCompat.DEFAULT_VIBRATE
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.*
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.manager.RabbitmqClient
import com.didahdx.smsgatewaysync.presentation.UiUpdaterInterface
import com.didahdx.smsgatewaysync.presentation.activities.MainActivity
import com.didahdx.smsgatewaysync.printerlib.IPrintToPrinter
import com.didahdx.smsgatewaysync.printerlib.WoosimPrnMng
import com.didahdx.smsgatewaysync.printerlib.utils.PrefMng
import com.didahdx.smsgatewaysync.printerlib.utils.Tools
import com.didahdx.smsgatewaysync.printerlib.utils.printerFactory
import com.didahdx.smsgatewaysync.receiver.ConnectionReceiver
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.work.SendApiWorker
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.didahdx.smsgatewaysync.work.SendSmsWorker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

class AppServices : Service(), UiUpdaterInterface {
    private lateinit var notificationManager: NotificationManagerCompat
    private var importantSmsNotification = 2
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
    private var uiUpdaterInterface: UiUpdaterInterface? = null
    private var mPrnMng: WoosimPrnMng? = null
    private var wakeLock: PowerManager.WakeLock? = null
    lateinit var notification: Notification


    @Volatile
    lateinit var rabbitmqClient: RabbitmqClient

    override fun onCreate() {
        super.onCreate()
        toast("Service started")
        registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
        registerReceiver(smsReceiver, IntentFilter(SMS_RECEIVED_INTENT))
        registerReceiver(locationBroadcastReceiver, IntentFilter(LOCATION_UPDATE_INTENT))

        val phoneCallFilter = IntentFilter(PHONE_STATE)
        phoneCallFilter.addAction(NEW_OUTGOING_CALL)
        registerReceiver(phoneCallReceiver, phoneCallFilter)
        uiUpdaterInterface = this

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    AppServices::class.java.simpleName
                ).apply {
                    acquire(10 * 60 * 1000L /*10 minutes*/)
                }
            }

        CoroutineScope(IO).launch {
            rabbitmqClient = RabbitmqClient(uiUpdaterInterface, user?.email!!)
            val urlEnabled = SpUtil.getPreferenceBoolean(this@AppServices, PREF_HOST_URL_ENABLED)
            val isServiceOn = SpUtil.getPreferenceBoolean(this@AppServices, PREF_SERVICES_KEY)
            if (isServiceOn && !urlEnabled) {
                setServiceState(this@AppServices, ServiceState.STARTING)
                val notification = notificationStatus("Service starting", false)
                Timber.d("The service has been created".toUpperCase())
                startForeground(1, notification)
                rabbitmqClient.connection(this@AppServices)
            }
        }


    }

    override fun onStartCommand(intent: Intent?, flagings: Int, startId: Int): Int {
        if (intent != null) {
            toast("Serive called ${intent.action}")
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
            .setOnlyAlertOnce(true)
            .build()

        notification.flags = notification.flags or Notification.DEFAULT_LIGHTS
        notification.flags = notification.flags or Notification.DEFAULT_VIBRATE
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val isServiceOn = SpUtil.getPreferenceBoolean(this, PREF_SERVICES_KEY)
        if (getRestartServiceState(this) && isServiceOn) {
            toast("Restart service state ${getRestartServiceState(this)}")
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


    private val smsReceiver = object : BroadcastReceiver() {

        private val newIntent = Intent(SMS_LOCAL_BROADCAST_RECEIVER)
        var phoneNumber: String = " "
        var messageText: String = " "
        var time: Long? = null

        override fun onReceive(context: Context, intent: Intent) {
            val printingReference =
                SpUtil.getPreferenceString(context, PREF_MPESA_TYPE, DIRECT_MPESA)
            val autoPrint = SpUtil.getPreferenceBoolean(context, PREF_AUTO_PRINT)
            val maskedPhoneNumber = SpUtil.getPreferenceBoolean(context, PREF_MASKED_NUMBER)

            if (SMS_RECEIVED_INTENT == intent.action) {
                Timber.d("action original ${intent.action}")
                val extras = intent.extras
                if (extras != null) {
                    val sms = extras.get("pdus") as Array<*>
                    val messageBuilder = StringBuilder()

                    for (i in sms.indices) {
                        val format = extras.getString("format")
                        var smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            SmsMessage.createFromPdu(sms[i] as ByteArray, format)
                        } else {
                            SmsMessage.createFromPdu(sms[i] as ByteArray)
                        }
                        phoneNumber = smsMessage.originatingAddress.toString()
                        time = smsMessage.timestampMillis
                        messageBuilder.append(smsMessage.messageBody.toString())

                        println("$phoneNumber \n sms : \t $sms  \n  messageText :\t $messageText ")
                    }

                    messageText = messageBuilder.toString()

                    newIntent.putExtra("phoneNumber", phoneNumber)
                    newIntent.putExtra("messageText", messageText)
                    newIntent.putExtra("date", time)
                    val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)
                    val printType = SpUtil.getPreferenceString(context, PREF_PRINT_TYPE, "")
                    val obj: JSONObject? = JSONObject()
                    val smsFilter = SmsFilter(messageText, maskedPhoneNumber)
                    val printMessage =
                        smsFilter?.checkSmsType(messageText!!.trim(), maskedPhoneNumber)
                    val importantSmsType =
                        SpUtil.getPreferenceString(context, PREF_IMPORTANT_SMS_NOTIFICATION, " ")
                    if (messageText != null && phoneNumber != null &&
                        (smsFilter.mpesaType == importantSmsType || importantSmsType == "All")
                    ) {
                        Timber.i(" $messageText \n $phoneNumber ")
                        notificationManager = NotificationManagerCompat.from(context)
                        notificationMessage(messageText, phoneNumber, context)
                    }


                    obj?.put("type", "message")
                    obj?.put("message_body", messageText)
                    obj?.put("receipt_date", sdf.format(time?.let { Date(it) }).toString())
                    obj?.put("sender_id", phoneNumber)
                    obj?.put("longitude", userLongitude)
                    obj?.put("latitude", userLatitude)
                    obj?.put("client_sender", user?.email!!)
                    obj?.put("client_gateway_type", "android_phone")


                    if (phoneNumber != null && smsFilter?.mpesaType != NOT_AVAILABLE) {
                        obj?.put("message_type", "mpesa")
                        obj?.put("voucher_number", smsFilter?.mpesaId)
                        obj?.put("transaction_type", smsFilter?.mpesaType)
                        obj?.put("phone_number", smsFilter?.phoneNumber)
                        obj?.put("name", smsFilter?.name)
                        if (smsFilter?.time != NOT_AVAILABLE && smsFilter?.date != NOT_AVAILABLE) {
                            obj?.put(
                                "transaction_date",
                                "${smsFilter?.date} ${smsFilter?.time}"
                            )
                        } else if (smsFilter.date != NOT_AVAILABLE) {
                            obj?.put("transaction_date", smsFilter.date)
                        } else if (smsFilter.time !=
                            NOT_AVAILABLE
                        ) {
                            obj?.put("transaction_date", smsFilter.time)
                        }
                        obj?.put("amount", smsFilter?.amount)

                    } else {
                        obj?.put("message_type", "recieved_sms")
                    }


                    CoroutineScope(IO).launch {
                        obj?.toString()?.let {
                            var status = false
                            val urlEnabled =
                                SpUtil.getPreferenceBoolean(context, PREF_HOST_URL_ENABLED)
                            if (!urlEnabled) {
                                val data = Data.Builder()
                                    .putString(KEY_TASK_MESSAGE, it)
                                    .putString(KEY_EMAIL, user?.email)
                                    .build()
                                sendToRabbitMQ(context, data)
                                status = true
                            }

                            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)
                            val message2: MpesaMessageInfo?

                            if (messageText != null && time != null && phoneNumber != null) {
                                val maskedPhoneNumber =
                                    SpUtil.getPreferenceBoolean(context, PREF_MASKED_NUMBER)
                                val smsFilter = SmsFilter(messageText, maskedPhoneNumber)

                                withContext(Main) {
                                    Timber.i("Otp code ${smsFilter.otpCode} website ${smsFilter.otpWebsite}")

                                    val smspost = SmsInboxInfo(
                                        0, messageText.trim(),
                                        sdf.format(Date(time!!)).toString(),
                                        phoneNumber,
                                        smsFilter.mpesaId,
                                        smsFilter.phoneNumber,
                                        smsFilter.amount,
                                        smsFilter.accountNumber,
                                        smsFilter.name,
                                        time!!,
                                        true, userLongitude, userLatitude
                                    )
//                                    postMessage(smspost)
                                }

                            }
                        }
                    }

                    CoroutineScope(IO).launch {
                        val message2: MpesaMessageInfo?

                        if (messageText != null && time != null && phoneNumber != null) {
                            val smsFilter = SmsFilter(messageText!!, false)
                            message2 = MpesaMessageInfo(
                                messageText!!.trim(),
                                sdf.format(Date(time!!)).toString(),
                                phoneNumber!!,
                                smsFilter.mpesaId,
                                smsFilter.phoneNumber,
                                smsFilter.amount,
                                smsFilter.accountNumber,
                                smsFilter.name,
                                time!!,
                                false, userLongitude, userLatitude
                            )

                            context.let { tex ->
                                MessagesDatabase(tex).getIncomingMessageDao()
                                    .addMessage(message2)
                            }

                        }
                    }


                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)


//                    if ("MPESA" == phoneNumber) {
                    if (printingReference == smsFilter.mpesaType && autoPrint) {

                        //Check if the Bluetooth is available and on.
                        Tools.isBlueToothOn(context)
                        val address: String = PrefMng.getDeviceAddr(context)
                        if (address.isNotEmpty() && Tools.isBlueToothOn(context)) {
                            val testPrinter: IPrintToPrinter =
                                BluetoothPrinter(context, printMessage)
                            //Connect to the printer and after successful connection issue the print command.
                            mPrnMng = printerFactory.createPrnMng(context, address, testPrinter)
                        } else {
                            context.toast("Printer not connected ")
                        }
                    }
//                    }

                }

            }
        }
    }

    private val phoneCallReceiver = object : BroadcastReceiver() {
//The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations


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
        if (context != null) {
            sendToRabbitMQ(context, data)
        }

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
            val serviceMethod = telephonyStubClass.getMethod(
                "asInterface",
                IBinder::class.java
            )
            telephonyObject = serviceMethod.invoke(null, retbinder)
            telephonyEndCall = telephonyClass.getMethod("endCall")
            telephonyEndCall.invoke(telephonyObject)
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.i(
                "FATAL ERROR: could not connect to telephony subsystem"
            )
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
                Timber.d("Gps/Network Location  $userLatitude  $userLongitude $altitude")
            }
        }
    }


    //used to show to notification
    fun notificationMessage(message: String, phoneNumber: String, context: Context) {
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
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()


        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
        notification.flags =
            Notification.FLAG_INSISTENT or Notification.FLAG_AUTO_CANCEL

        notificationManager.notify(importantSmsNotification, notification)
        importantSmsNotification++
    }


    private fun sendToRabbitMQ(context: Context, data: Data) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendRabbitMqWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }


    private fun sendSms(context: Context, data: Data) {
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendSmsWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    private fun sendToApi(context: Context, data: Data) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendApiWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }


    override fun notificationMessage(message: String) {
        notificationManager = NotificationManagerCompat.from(this)
        val notification = notificationStatus(message, true)

        notificationManager.notify(1, notification)
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
        CoroutineScope(Main).launch {

            SpUtil.setPreferenceString(context, PREF_STATUS_MESSAGE, status)
            SpUtil.setPreferenceString(context, PREF_STATUS_COLOR, color)
            statusIntent.putExtra(STATUS_MESSAGE_EXTRA, status)
            statusIntent.putExtra(STATUS_COLOR_EXTRA, color)
            sendBroadcast(statusIntent)
            AppLog.logMessage(status, context)
            notificationManager = NotificationManagerCompat.from(context)
            if (ServiceState.STARTING == getServiceState(this@AppServices) ||
                ServiceState.RUNNING == getServiceState(this@AppServices) ||
                ServiceState.STOPPED != getServiceState(this@AppServices)
            ) {
                val notification = notificationStatus(status, false)
                notificationManager.notify(1, notification)
            }

        }
    }

    override fun sendSms(phoneNumber: String, message: String) {
        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, message)
            .putString(KEY_PHONE_NUMBER, phoneNumber)
            .build()

        sendSms(this, data)
    }

    override fun updateSettings(preferenceType: String, key: String, value: String) {
        SpUtil.updateSettingsRemotely(this, preferenceType, key, value)
    }

    override fun logMessage(message: String) {
        AppLog.logMessage(message, this)
    }


    //used to show to notification
    private fun notificationStatus(message: String, value: Boolean): Notification {

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_home)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(value)
            .setOnlyAlertOnce(true)
            .setDefaults(DEFAULT_LIGHTS or DEFAULT_VIBRATE)
            .build()

        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.flags =
            Notification.FLAG_INSISTENT or Notification.FLAG_AUTO_CANCEL
        return notification

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

        CoroutineScope(IO).launch {
            rabbitmqClient.disconnect()
        }

        toast("Service destroyed")
    }
}
