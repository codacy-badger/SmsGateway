package com.didahdx.smsgatewaysync.presentation.smsInbox

import android.annotation.SuppressLint
import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.os.Debug
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
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
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

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
    }

    fun onMessageDetailClicked(id: SmsInboxInfo) {
        _eventMessageClicked.value = id
    }

    fun onMessageDetailNavigated() {
        _eventMessageClicked.value = null
    }

    private val _messageArrayList = MutableLiveData<Cursor>()
    val messageArrayList: LiveData<Cursor>
        get() = _messageArrayList


    private fun setInputMessage(message: Cursor) {
        _messageArrayList.postValue(message)
    }


    fun getDbSmsMessages() {
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
            val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

            Timber.i("mpesa sms $mpesaType ")
            var count = 0

            do {
                val dateString = cursor.getString(dateId)
//                if (cursor.getString(nameId) == "MPESA") {
                val maskedPhoneNumber = sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)
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

    private fun sortCursor(rowNumbers: ArrayList<String>) {
        Debug.startMethodTracing("cursor.trace")
        val projections = arrayOf("address", "body", "date")
        var inClause: String = rowNumbers.toString()
        inClause = inClause.replace('[', '(').replace(']', ')')
        val selection = "date IN  $inClause"
        val cursor = app.contentResolver?.query(Uri.parse("content://sms/inbox"),
            projections, selection, null, null)
        cursor?.let { setInputMessage(it) }
        Debug.stopMethodTracing()
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
        CoroutineScope(IO).launch {
            val cursor = app.contentResolver?.query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                null
            )
            Timber.i("get all Db message calledv")

            if (cursor != null && cursor.moveToNext()) {
//                val message_id = cursor.getColumnIndex("_id")
                val nameId = cursor.getColumnIndex("address")
                val messageId = cursor.getColumnIndex("body")
                val dateId = cursor.getColumnIndex("date")
                val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

                Timber.i("mpesa sms $mpesaType ")
                do {
                    val dateString = cursor.getString(dateId)
                    val messageBody = cursor.getString(messageId)
                    val sender = cursor.getString(nameId)
//                if (cursor.getString(nameId) == "MPESA") {

                    val maskedPhoneNumber = sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)
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
                Timber.i("done ")
            }
        }
    }
}