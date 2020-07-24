package com.didahdx.smsgatewaysync.presentation.smsInbox

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.util.*
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class FilterDataSmsInboxRunnable(
    application: Application,
    mainThreadHandler: Handler
) : Runnable {


    companion object {
        const val PROGRESS_COUNT_INT = 120
        const val PROGRESS_COUNT_STRING = "PROGRESS_COUNT_STRING"
        const val FILTERED_DATA = 210
        const val FILTERED_DATA_STRING = "FILTERED_DATA_STRING"
    }

    private var mMainThreadHandler: WeakReference<Handler>? = null
    private val app = application

    init {
        mMainThreadHandler = WeakReference(mainThreadHandler)
    }

    override fun run() {
        getDbSmsMessages()
    }


    @AddTrace(name = "SmsInboxViewModel_GetDbSmsMessages")
    fun getDbSmsMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            val cursor = app.contentResolver?.query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                null
            )

            val smsItem = ArrayList<String>()

            if (cursor != null && cursor.moveToNext()) {
//            val nameId = cursor.getColumnIndex("address")
                val messageId = cursor.getColumnIndex("body")
                val dateId = cursor.getColumnIndex("date")
                val mpesaType = SpUtil.getPreferenceString(app, PREF_MPESA_TYPE, DIRECT_MPESA)

                Timber.i("mpesa sms $mpesaType ")
                var count = 0

                do {
                    val dateString = cursor.getString(dateId)
//                if (cursor.getString(nameId) == "MPESA") {

                    val maskedPhoneNumber = SpUtil.getPreferenceBoolean(app, PREF_MASKED_NUMBER)

                    val smsFilter = SmsFilter(cursor.getString(messageId), maskedPhoneNumber)
                    when (mpesaType) {
                        PAY_BILL -> {
                            if (smsFilter.mpesaType == PAY_BILL) {
                                cursor.position.let { smsItem.add(dateString) }
                                count++
                            }
                        }
                        DIRECT_MPESA -> {
                            if (smsFilter.mpesaType == DIRECT_MPESA) {
                                cursor.position.let { smsItem.add(dateString) }
                                count++
                            }
                        }

                        BUY_GOODS_AND_SERVICES -> {
                            if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                                cursor.position.let { smsItem.add(dateString) }
                                count++
                            }
                        }

                        else -> {
                            cursor.position.let { smsItem.add(dateString) }
                            count++
                        }
                    }
//                }
                    setCount(count)
                } while (cursor.moveToNext())
                cursor.close()
            }
            sortCursor(smsItem)
        }
    }

    @AddTrace(name = "FilterDataSmsInboxRunnable_sortCursor")
    private fun sortCursor(rowNumbers: ArrayList<String>) {
        CoroutineScope(IO).launch {
            val smsInbox = ArrayList<SmsInboxInfo>()
            val projections = arrayOf("address", "body", "date")
            var inClause: String = rowNumbers.toString()
            inClause = inClause.replace('[', '(').replace(']', ')')
            val selection = "date IN  $inClause"

            val mCursor = app.contentResolver?.query(
                Uri.parse("content://sms/inbox"),
                projections,
                selection,
                null,
                null
            )

            if (mCursor != null && mCursor.moveToNext()) {
                do {
                    val nameId = mCursor.getColumnIndex("address")
                    val messageId = mCursor.getColumnIndex("body")
                    val dateId = mCursor.getColumnIndex("date")
                    val dateString = dateId.let { mCursor.getString(it) }
                    val smsFilter =
                        messageId.let { messageId ->
                            mCursor.getString(messageId)?.let { SmsFilter(it, false) }
                        }

                    nameId.let { mCursor.getString(it) }?.let {
                        dateString?.toLong()?.let { it1 ->
                            if (smsFilter != null) {
                                mCursor.getString(messageId)?.let { it2 ->
                                    SmsInboxInfo(
                                        messageId,
                                        it2,
                                        Conversion.getFormattedDate(Date(dateString.toLong()))
                                            .toString(),
                                        it,
                                        smsFilter.mpesaId,
                                        smsFilter.phoneNumber,
                                        smsFilter.amount,
                                        smsFilter.accountNumber,
                                        smsFilter.name,
                                        it1, false, "", ""
                                    )
                                }?.let { it3 -> smsInbox.add(it3) }
                            }
                        }
                    }

                } while (mCursor.moveToNext())
                mCursor.close()
            }

            setInputMessage(smsInbox)
        }
    }

    private fun setInputMessage(smsInbox: ArrayList<SmsInboxInfo>) {
        val message = Message.obtain(null, FILTERED_DATA)
        val bundle = Bundle()
        bundle.putParcelableArrayList(FILTERED_DATA_STRING, smsInbox)
        message.data = bundle
        mMainThreadHandler?.get()?.sendMessage(message)
    }

    private fun setCount(count:Int){
        val message = Message.obtain(null, PROGRESS_COUNT_INT)
        val bundle = Bundle()
        bundle.putInt(PROGRESS_COUNT_STRING, count)
        message.data = bundle
        mMainThreadHandler?.get()?.sendMessage(message)
    }

}