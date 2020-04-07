package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.utilities.APP_NAME
import com.didahdx.smsgatewaysync.utilities.SMS_RECEIVED_INTENT
import com.didahdx.smsgatewaysync.utilities.SmsFilter
import com.didahdx.smsgatewaysync.utilities.toast
import com.didahdx.smsgatewaysync.utilities.BluetoothPrinter
import com.mazenrashed.printooth.Printooth


class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (SMS_RECEIVED_INTENT==intent.action) {
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
                    val sms=smsMessage.displayMessageBody

                    val newIntent = Intent(SMS_RECEIVED_INTENT)
                    newIntent.putExtra("phoneNumber", phoneNumber)
                    newIntent.putExtra("messageText", messageText)

                    Toast.makeText(context,"display $sms",Toast.LENGTH_LONG).show()
                    Log.d("tpoiuytr"," $sms   $phoneNumber ")
                    context?.toast(" $sms   $phoneNumber ")
                    print(" $sms   $phoneNumber ")
                    val printer= BluetoothPrinter()
                    val smsFilter= SmsFilter()
                    if (Printooth.hasPairedPrinter()){
                        val printMessage=smsFilter.checkSmsType(messageText)
                        printer.printText(printMessage,context, APP_NAME)
                    }else{
                        context?.toast("Printer not connected")
                    }
                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)


                }
            }


        }
    }
}