package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.utilities.*
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

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.extras!!.getString("android.intent.extra.PHONE_NUMBER")
            context?.toast("outgoing call $savedNumber")

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

            Log.d("cafrghjk", "phone receiver called $number")
            onCallStateChanged(context, state, number)

        }
    }


    //Deals with actual events

    //Deals with actual events
    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    private fun onCallStateChanged(
        context: Context?,
        state: Int,
        number: String?
    ) {

        Log.d("cafrghjk", "caall called")
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
                    }
                } else {
                    isIncoming = true
                    callStartTime = Date()
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onIncomingCallAnswered")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, Date().toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
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
                    }
                } else if (isIncoming) {
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onIncomingCallEnded")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, callStartTime.toString())
                        newIntent.putExtra(END_TIME_EXTRA, Date().toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
                    }
                } else {
                    if (context != null) {
                        newIntent.putExtra(CALL_TYPE_EXTRA, "onOutgoingCallEnded")
                        newIntent.putExtra(PHONE_NUMBER_EXTRA, savedNumber)
                        newIntent.putExtra(START_TIME_EXTRA, callStartTime.toString())
                        newIntent.putExtra(END_TIME_EXTRA, Date().toString())
                        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
                    }
                }
        }
        lastState = state
    }
}
