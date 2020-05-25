package com.didahdx.smsgatewaysync.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings.Global.getString
import android.telephony.SmsMessage
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class SmsReceiver : BroadcastReceiver() {
    private lateinit var sharedPreferences: SharedPreferences
    private val newIntent = Intent(SMS_LOCAL_BROADCAST_RECEIVER)
    var phoneNumber: String? = " "
    var messageText: String? = ""
    var time: Long? = null
    private val printer = BluetoothPrinter()
    private val smsFilter = SmsFilter()

    override fun onReceive(context: Context, intent: Intent) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val printingReference = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)
        val autoPrint = sharedPreferences.getBoolean(PREF_AUTO_PRINT, false)
        val maskedPhoneNumber = sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)

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

                messageText = messageBuilder.toString()

                newIntent.putExtra("phoneNumber", phoneNumber)
                newIntent.putExtra("messageText", messageText)
                newIntent.putExtra("date", time)
                var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
//                CoroutineScope(IO).launch {
//                    val message2: MpesaMessageInfo?
//
//                    if (messageText != null && time != null && phoneNumber != null) {
//                        val smsFilter = SmsFilter(messageText!!)
//                        message2 = MpesaMessageInfo(
//                            messageText!!.trim(),
//                            sdf.format(Date(time!!)).toString(),
//                            phoneNumber!!,
//                            smsFilter.mpesaId,
//                            smsFilter.phoneNumber,
//                            smsFilter.amount,
//                            smsFilter.accountNumber,
//                            smsFilter.name,
//                            time!!,
//                            true, "", ""
//                        )
//
//                        context.let { tex ->
//                            MessagesDatabase(tex).getIncomingMessageDao()
//                                .addMessage(message2)
//                        }
//
//                    }
//                }

                LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
                val printMessage = smsFilter.checkSmsType(messageText!!.trim(), maskedPhoneNumber)


                if ("MPESA" == phoneNumber) {
                    if (printingReference == smsFilter.mpesaType && autoPrint) {
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