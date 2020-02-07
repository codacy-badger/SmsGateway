package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import java.text.SimpleDateFormat
import java.util.*


class SmsReceiver : BroadcastReceiver() {

    val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == SMS_RECEIVED) {

            val extras = intent.extras


            if (extras != null) {
                val sms = extras.get("pdus") as Array<*>

                for (i in sms.indices) {
                    val format = extras.getString("format")

                    var smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(sms[0] as ByteArray, format)
                    } else {
                        SmsMessage.createFromPdu(sms[0] as ByteArray)
                    }

                    val phoneNumber = smsMessage.originatingAddress
                    val messageText = smsMessage.messageBody.toString()


                    val newIntent = Intent(SMS_RECEIVED)
                    newIntent.putExtra("phoneNumber", phoneNumber)
                    newIntent.putExtra("messageText", messageText)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)


                }
            }
        }
    }


}
