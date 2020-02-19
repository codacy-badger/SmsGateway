package com.didahdx.smsgatewaysync.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.Global.getString
import android.telephony.SmsMessage
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.data.printable.Printable
import com.mazenrashed.printooth.data.printable.RawPrintable
import com.mazenrashed.printooth.data.printable.TextPrintable
import com.mazenrashed.printooth.data.printer.DefaultPrinter
import com.mazenrashed.printooth.ui.ScanningActivity

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
                    val sms=smsMessage.displayMessageBody


                    val newIntent = Intent(SMS_RECEIVED)
                    newIntent.putExtra("phoneNumber", phoneNumber)
                    newIntent.putExtra("messageText", messageText)
                    Toast.makeText(context,"message $messageText",Toast.LENGTH_LONG).show()
                    Toast.makeText(context,"display $sms",Toast.LENGTH_LONG).show()
                    val printer= bluetoothPrinter()
                    val smsFilter=SmsFilter()
                    if (Printooth.hasPairedPrinter()){
                        printer.printText(smsFilter.checkSmsType(messageText),context, APP_NAME)
                    }else{
                        Toast.makeText(context,"Connect a printer",Toast.LENGTH_LONG).show()
                    }
                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)


                }
            }
        }
    }



}
