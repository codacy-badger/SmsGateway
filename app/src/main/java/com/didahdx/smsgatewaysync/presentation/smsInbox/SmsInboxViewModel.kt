package com.didahdx.smsgatewaysync.presentation.smsInbox

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class SmsInboxViewModel(dataSource: IncomingMessagesDao, application: Application) : ViewModel() {

    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
    val app = application
    val database = dataSource

    //data to be passed to next screen
    private val _eventMessageClicked = MutableLiveData<SmsInboxInfo>()
    val eventMessageClicked: LiveData<SmsInboxInfo>
        get() = _eventMessageClicked

    //data to be passed to next screen
    private val _messageCount = MutableLiveData<Int>()
    val messageCount: LiveData<Int>
        get() = _messageCount

    init {
        _messageCount.value = 0
    getDbSmsMessages()
    }


    fun onMessageDetailClicked(id: SmsInboxInfo) {
        _eventMessageClicked.value = id
    }

    fun onMessageDetailNavigated() {
        _eventMessageClicked.value = null
    }

    private val _messageArrayList = MutableLiveData<List<SmsInboxInfo>>()
    val messageArrayList: LiveData<List<SmsInboxInfo>>
        get() = _messageArrayList


    private fun setInputMessage(message: List<SmsInboxInfo>) {
        _messageArrayList.postValue(message)
    }


    fun getDbSmsMessages() {
        CoroutineScope(IO).launch {
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

    private fun sortCursor(rowNumbers: ArrayList<String>) {
        CoroutineScope(IO).launch {
        val smsInbox = ArrayList<SmsInboxInfo>()
//        Debug.startMethodTracing("cursor.trace")
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
                                    sdf.format(Date(dateString.toLong()))
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

        setInputMessage(smsInbox)}
//            cursor?.let { setInputMessage(it) }
//        Debug.stopMethodTracing()
    }


    private fun setCount(count: Int) {
        _messageCount.postValue(count)
    }

    fun setUpSmsInfo(info: SmsInboxInfo): SmsInfo {
        var smsStatus = NOT_AVAILABLE
        try {
            smsStatus = if (info.status) {
                "Uploaded"
            } else {
                "pending"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        return SmsInfo(
            info.messageBody,
            info.time,
            info.sender,
            smsStatus,
            info.longitude,
            info.latitude
        )
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(IO).cancel()
    }

    fun getAllDbSms() {
     val importAllMessage=ImportAllDbMessageRunnable(database,app)
     Thread(importAllMessage).start()
    }

    class ImportAllDbMessageRunnable(dataSource: IncomingMessagesDao, application: Application):Runnable{
        val app=application
        val database=dataSource
        var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
        override fun run() {
            val threadHandler = Handler(Looper.getMainLooper())
            threadHandler.post {
                app.toast("Importing Messages started")
            }

            CoroutineScope(IO).launch {
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
                            sdf.format(Date(dateString.toLong())),
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

}