package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth


class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (SMS_RECEIVED_INTENT == intent.action) {
            Log.d("sms_rece","action original ${intent.action}")
            val extras = intent.extras
            if (extras != null) {
                val sms = extras.get("pdus") as Array<*>
                var messageBuilder=StringBuilder()

                for (i in sms.indices) {
                    val format = extras.getString("format")
                    var smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(sms[0] as ByteArray, format)
                    } else {
                        SmsMessage.createFromPdu(sms[0] as ByteArray)
                    }
                    val phoneNumber = smsMessage.originatingAddress
                    var messageText = smsMessage.messageBody.toString()
                    val time = smsMessage.timestampMillis
                    val sms = smsMessage.displayMessageBody.toString()
                    messageBuilder.append(smsMessage.displayMessageBody.toString())


                    val newIntent = Intent(SMS_LOCAL_BROADCAST_RECEIVER)
                    newIntent.putExtra("phoneNumber", phoneNumber)
                    newIntent.putExtra("messageText", messageText)
                    newIntent.putExtra("date",time)

                    Log.d("tpoiuytr", "   $phoneNumber sms $sms messageText $messageText ")


                    val printer = BluetoothPrinter()
                    val smsFilter = SmsFilter()
                    if (Printooth.hasPairedPrinter()) {
                        val printMessage = smsFilter.checkSmsType(messageText)
                        printer.printText(printMessage, context, APP_NAME)
                    } else {
                        context?.toast("Printer not connected  ")
                    }

                    println("$phoneNumber \n sms : \t $sms  \n  messageText :\t $messageText ")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
                }

            }


        }
    }
}