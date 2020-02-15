package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.utilities.SMS_RECEIVED

import java.text.SimpleDateFormat
import java.util.*



class SmsReceiver : BroadcastReceiver() {


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
                    Toast.makeText(context,messageText,Toast.LENGTH_LONG).show()
                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)


                }
            }
        }
    }


}
