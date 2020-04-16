package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.didahdx.smsgatewaysync.utilities.toast

class CallReceiver :BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
//            context?.toast("Call started...");
        }
        else if(intent?.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)){
//            context?.toast("Call ended...");
        }
        else if(intent?.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)){
//            context?.toast("Incoming call...");
        }
    }
}