package com.didahdx.smsgatewaysync.broadcastReceivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Data
import com.didahdx.smsgatewaysync.domain.CallStatus
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.work.WorkerUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.perf.metrics.AddTrace
import com.google.gson.Gson
import timber.log.Timber
import java.lang.reflect.Method
import java.util.*


class PhoneCallReceiver : BroadcastReceiver() {
//The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations

    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var callStartTime: Date? = null
    private var isIncoming = false
    private var savedNumber //because the passed incoming is only valid in ringing
            : String? = null
    val newIntent = Intent(CALL_LOCAL_BROADCAST_RECEIVER)

    @AddTrace(name = "PhoneCallOnReceive", enabled = true /* optional */)
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
            context.toast("phone receiver called $number")
            onCallStateChanged(context, state, number)

        }
    }

    //Deals with actual events
    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    private fun onCallStateChanged(context: Context?, state: Int, number: String?) {

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
            AppLog.logMessage("$callType from $phoneNumber automatic call hangUp at time $startTime",context)
        }
        val phone = phoneNumber ?: " "
        val email = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE

        val callStatus = CallStatus(
            type = "calls",
            longitude = SpUtil.getPreferenceString(context, PREF_LONGITUDE, " "),
            latitude = SpUtil.getPreferenceString(context, PREF_LATITUDE, " "),
            client_sender = email,
            call_type = callType,
            phone_number = phone,
            start_time = startTime,
            end_time = endTime
        )

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, Gson().toJson(callStatus))
            .putString(KEY_EMAIL, FirebaseAuth.getInstance().currentUser?.email)
            .build()
        WorkerUtil.sendToRabbitMQ(context, data)

    }

    private fun hangUpCall(context: Context) {
        val hangup = SpUtil.getPreferenceBoolean(context, PREF_HANG_UP)
        if (hangup) {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tm.endCall()
            } else {
                disconnectCall(context)
            }
        }
    }


    @SuppressLint("PrivateApi")
    private fun disconnectCall(context: Context) {
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
            AppLog.logMessage("disconnect call method $e  ${e.localizedMessage}", context)
        }
    }


}
