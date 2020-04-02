package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast
import com.didahdx.smsgatewaysync.utilities.SMS_RECEIVED


class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
//        if (SMS_RECEIVED==intent.action) {
//            val extras = intent.extras
//            if (extras != null) {
//                val sms = extras.get("pdus") as Array<*>
//
//                for (i in sms.indices) {
//                    val format = extras.getString("format")
//                    var smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                        SmsMessage.createFromPdu(sms[0] as ByteArray, format)
//                    } else {
//                        SmsMessage.createFromPdu(sms[0] as ByteArray)
//                    }
//                    val phoneNumber = smsMessage.originatingAddress
//                    val messageText = smsMessage.messageBody.toString()
//                    val sms=smsMessage.displayMessageBody
//
//                    val newIntent = Intent(SMS_RECEIVED)
//                    newIntent.putExtra("phoneNumber", phoneNumber)
//                    newIntent.putExtra("messageText", messageText)
//
//                    Toast.makeText(context,"display $sms",Toast.LENGTH_LONG).show()
//                    Log.d("tpoiuytr"," $sms   $phoneNumber ")
//                    context?.toast(" $sms   $phoneNumber ")
//                    print(" $sms   $phoneNumber ")
//                    val printer= BluetoothPrinter()
//                    val smsFilter=SmsFilter()
//                    if (Printooth.hasPairedPrinter()){
//                        val printMessage=smsFilter.checkSmsType(messageText)
//                        printer.printText(printMessage,context, APP_NAME)
//                    }else{
//                        context?.toast("Printer not connected")
//                    }
//                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

//
//                }
//            }


        if (intent.action.equals(SMS_RECEIVED)) {
            val bundle = intent.extras
            if (bundle != null) {
                // get sms objects
                val pdus =
                    bundle["pdus"] as Array<*>?
                if (pdus!!.size == 0) {
                    return
                }
                // large message might be broken into many
                val messages =
                    arrayOfNulls<SmsMessage>(pdus.size)
                val sb = StringBuilder()
                for (i in pdus.indices) {
                    messages[i] =
                        SmsMessage.createFromPdu(pdus[i] as ByteArray)
                    sb.append(messages[i]?.messageBody)
                }
                val sender = messages[0]!!.originatingAddress
                val message = sb.toString()
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                // prevent any other broadcast receivers from receiving broadcast
                // abortBroadcast();
            }
        }

        }
    }