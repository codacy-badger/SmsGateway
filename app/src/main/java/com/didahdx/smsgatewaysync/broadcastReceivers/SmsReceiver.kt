package com.didahdx.smsgatewaysync.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.printerlib.IPrintToPrinter
import com.didahdx.smsgatewaysync.printerlib.WoosimPrnMng
import com.didahdx.smsgatewaysync.printerlib.utils.PrefMng
import com.didahdx.smsgatewaysync.printerlib.utils.Tools
import com.didahdx.smsgatewaysync.printerlib.utils.printerFactory
import com.didahdx.smsgatewaysync.util.*
import com.didahdx.smsgatewaysync.work.WorkerUtil.sendToRabbitMQ
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.util.*


class SmsReceiver : BroadcastReceiver() {
    private val newIntent = Intent(SMS_LOCAL_BROADCAST_RECEIVER)
    var phoneNumber: String = " "
    var messageText: String = ""
    var time: Long? = null
    private var mPrnMng: WoosimPrnMng? = null
    private var userLatitude = ""
    private var userLongitude = ""
    private val user = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE

    override fun onReceive(context: Context, intent: Intent) {
        val printingReference = SpUtil.getPreferenceString(context, PREF_MPESA_TYPE, DIRECT_MPESA)
        val autoPrint = SpUtil.getPreferenceBoolean(context, PREF_AUTO_PRINT)
        val maskedPhoneNumber = SpUtil.getPreferenceBoolean(context, PREF_MASKED_NUMBER)
        userLatitude = SpUtil.getPreferenceString(context, PREF_LATITUDE, " ")
        userLongitude = SpUtil.getPreferenceString(context, PREF_LONGITUDE, " ")

        if (SMS_RECEIVED_INTENT == intent.action) {
            Timber.d("action original ${intent.action}")
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
                    phoneNumber = smsMessage.originatingAddress.toString() ?: " "
                    time = smsMessage.timestampMillis
                    messageBuilder.append(smsMessage.messageBody.toString())

                    println("$phoneNumber \n sms : \t $sms  \n  messageText :\t $messageText ")
                }

                messageText = messageBuilder.toString()

                newIntent.putExtra("phoneNumber", phoneNumber)
                newIntent.putExtra("messageText", messageText)
                newIntent.putExtra("date", time)
                val printType = SpUtil.getPreferenceString(context, PREF_PRINT_TYPE, "")
                val obj: JSONObject? = JSONObject()
                val smsFilter = SmsFilter(messageText, maskedPhoneNumber)
                val printMessage =
                    smsFilter.checkSmsType(messageText.trim(), maskedPhoneNumber)
                val importantSmsType =
                    SpUtil.getPreferenceString(context, PREF_IMPORTANT_SMS_NOTIFICATION, " ")
                if (smsFilter.mpesaType == importantSmsType || importantSmsType == "All") {
                    Timber.i(" $messageText \n $phoneNumber ")
                    NotificationUtil.notificationMessage(
                        messageText, phoneNumber,
                        context, userLatitude, userLongitude
                    )
                }

                obj?.put("type", "message")
                obj?.put("message_body", messageText)
                obj?.put("receipt_date", time?.let { Date(it) }?.let { Conversion.getFormattedDate(it) })
                obj?.put("sender_id", phoneNumber)
                obj?.put("longitude", userLongitude)
                obj?.put("latitude", userLatitude)
                obj?.put("client_sender", user)
                obj?.put("client_gateway_type", "android_phone")


                if (smsFilter.mpesaType != NOT_AVAILABLE) {
                    obj?.put("message_type", "mpesa")
                    obj?.put("voucher_number", smsFilter.mpesaId)
                    obj?.put("transaction_type", smsFilter.mpesaType)
                    obj?.put("phone_number", smsFilter.phoneNumber)
                    obj?.put("name", smsFilter.name)
                    if (smsFilter.time != NOT_AVAILABLE && smsFilter.date != NOT_AVAILABLE) {
                        obj?.put(
                            "transaction_date",
                            "${smsFilter.date} ${smsFilter.time}"
                        )
                    } else if (smsFilter.date != NOT_AVAILABLE) {
                        obj?.put("transaction_date", smsFilter.date)
                    } else if (smsFilter.time !=
                        NOT_AVAILABLE
                    ) {
                        obj?.put("transaction_date", smsFilter.time)
                    }
                    obj?.put("amount", smsFilter.amount)

                } else {
                    obj?.put("message_type", "recieved_sms")
                }

                obj?.toString()?.let {
                    var status = false
                    val urlEnabled = SpUtil.getPreferenceBoolean(context, PREF_HOST_URL_ENABLED)
                    if (!urlEnabled) {
                        val data = Data.Builder()
                            .putString(KEY_TASK_MESSAGE, it)
                            .putString(KEY_EMAIL, user)
                            .build()
                        sendToRabbitMQ(context, data)
                        status = true
                    }

                    val message2: MpesaMessageInfo?

                    if (time != null) {
                        val maskedPhoneNumber =
                            SpUtil.getPreferenceBoolean(context, PREF_MASKED_NUMBER)
                        val smsFilter = SmsFilter(messageText, maskedPhoneNumber)

                        Timber.i("Otp code ${smsFilter.otpCode} website ${smsFilter.otpWebsite}")

                        val smspost = SmsInboxInfo(
                            0, messageText.trim(),
                            Conversion.getFormattedDate(Date(time!!)),
                            phoneNumber,
                            smsFilter.mpesaId,
                            smsFilter.phoneNumber,
                            smsFilter.amount,
                            smsFilter.accountNumber,
                            smsFilter.name,
                            time!!,
                            true, userLongitude, userLatitude
                        )
//                       postMessage(smspost)
                    }
                }


                CoroutineScope(IO).launch {
                    val message2: MpesaMessageInfo?

                    if (time != null) {
                        val smsFilter = SmsFilter(messageText, false)
                        message2 = MpesaMessageInfo(
                            messageText.trim(),
                           Conversion.getFormattedDate(Date(time!!)),
                            phoneNumber,
                            smsFilter.mpesaId,
                            smsFilter.phoneNumber,
                            smsFilter.amount,
                            smsFilter.accountNumber,
                            smsFilter.name,
                            time!!,
                            false, userLongitude, userLatitude
                        )

                        context.let { tex ->
                            MessagesDatabase(tex).getIncomingMessageDao()
                                .addMessage(message2)
                        }

                    }
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

//                    if ("MPESA" == phoneNumber) {
                if (printingReference == smsFilter.mpesaType && autoPrint) {
                    //Check if the Bluetooth is available and on.
                    Tools.isBlueToothOn(context)
                    val address: String = PrefMng.getDeviceAddr(context)
                    if (address.isNotEmpty() && Tools.isBlueToothOn(context)) {
                        val testPrinter: IPrintToPrinter =
                            BluetoothPrinter(context, printMessage)
                        //Connect to the printer and after successful connection issue the print command.
                        mPrnMng = printerFactory.createPrnMng(context, address, testPrinter)
                    } else {
                        context.toast("Printer not connected ")
                    }
                }
//                    }
            }
        }
    }

}