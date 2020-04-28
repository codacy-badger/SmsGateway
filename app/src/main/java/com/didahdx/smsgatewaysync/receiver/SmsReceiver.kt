package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.data.db.entities.IncomingMessages
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


class SmsReceiver : BroadcastReceiver() {
    private lateinit var sharedPreferences: SharedPreferences
    private val newIntent = Intent(SMS_LOCAL_BROADCAST_RECEIVER)
    var phoneNumber:String?=" "
    var messageText:String?=""
    var time:Long?=null
    private val printer = BluetoothPrinter()
    private val smsFilter = SmsFilter()

    override fun onReceive(context: Context, intent: Intent) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val printingReference = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

        if (SMS_RECEIVED_INTENT == intent.action) {
            Log.d("sms_rece", "action original ${intent.action}")
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
                    phoneNumber = smsMessage.originatingAddress
                     time = smsMessage.timestampMillis
                    messageBuilder.append(smsMessage.messageBody.toString())

                    println("$phoneNumber \n sms : \t $sms  \n  messageText :\t $messageText ")
                }

                messageText=messageBuilder.toString()

                newIntent.putExtra("phoneNumber", phoneNumber)
                newIntent.putExtra("messageText", messageText)
                newIntent.putExtra("date", time)

                CoroutineScope(IO).launch {
                    val message2: IncomingMessages?
                    message2 = IncomingMessages(
                            messageText!!, time!!,
                            phoneNumber!!, true)

                    context.let { tex ->
                        MessagesDatabase(tex).getIncomingMessageDao()
                            .addMessage(message2)
                    }
                }

                LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
                if ("MPESA" == phoneNumber) {
                    val printMessage = smsFilter.checkSmsType(messageText!!)
                    if (printingReference == smsFilter.mpesaType) {
                        if (Printooth.hasPairedPrinter()) {
                            printer.printText(printMessage, context, APP_NAME)
                        } else {
                            context?.toast("Printer not connected  ")
                        }
                    }
                }

            }


        }
    }
}