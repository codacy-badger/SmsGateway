package com.didahdx.smsgatewaysync.presentation.smsInbox

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class ImportAllDbMessageRunnable(dataSource: IncomingMessagesDao, application: Application):Runnable{
    val app=application
    val database=dataSource
    override fun run() {
        val threadHandler = Handler(Looper.getMainLooper())
        threadHandler.post {
            app.toast("Importing Messages started")
        }

        CoroutineScope(Dispatchers.IO).launch {
            val cursor = app.contentResolver?.query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                null
            )
            Timber.i("get all Db message ")

            if (cursor != null && cursor.moveToNext()) {
//                val message_id = cursor.getColumnIndex("_id")
                val nameId = cursor.getColumnIndex("address")
                val messageId = cursor.getColumnIndex("body")
                val dateId = cursor.getColumnIndex("date")
                val mpesaType = SpUtil.getPreferenceString(app, PREF_MPESA_TYPE, DIRECT_MPESA)

                Timber.i("mpesa sms $mpesaType ")
                do {
                    val dateString = cursor.getString(dateId)
                    val messageBody = cursor.getString(messageId)
                    val sender = cursor.getString(nameId)
//                if (cursor.getString(nameId) == "MPESA") {

                    val maskedPhoneNumber = SpUtil.getPreferenceBoolean(app, PREF_MASKED_NUMBER)
                    val smsFilter = SmsFilter(cursor.getString(messageId), maskedPhoneNumber)
                    val data = database.getMessage(smsFilter.mpesaId)

                    val message = MpesaMessageInfo(
                        messageBody,
                        Conversion.getFormattedDate(Date(dateString.toLong())),
                        sender,
                        smsFilter.mpesaId,
                        smsFilter.phoneNumber,
                        smsFilter.amount,
                        smsFilter.accountNumber,
                        smsFilter.name,
                        dateString.toLong(),
                        false,
                        "",
                        ""
                    )

                    when (mpesaType) {
                        PAY_BILL -> {
                            if (smsFilter.mpesaType == PAY_BILL) {
                                if (data.isNullOrEmpty()) {
                                    database.addMessage(message)
                                }
                            }
                        }
                        DIRECT_MPESA -> {
                            if (smsFilter.mpesaType == DIRECT_MPESA) {
                                if (data.isNullOrEmpty()) {
                                    database.addMessage(message)
                                }
                            }
                        }

                        BUY_GOODS_AND_SERVICES -> {
                            if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                                if (data.isNullOrEmpty()) {
                                    database.addMessage(message)
                                }
                            }
                        }
                    }
//                }

                } while (cursor.moveToNext())
                cursor.close()
                Timber.i("Importing Messages finished")
            }

            CoroutineScope(Main).launch {
                threadHandler.post {
                    app.toast("Importing Messages finished")
                }
            }
        }

    }

}
