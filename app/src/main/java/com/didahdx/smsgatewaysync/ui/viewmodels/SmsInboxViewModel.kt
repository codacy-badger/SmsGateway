package com.didahdx.smsgatewaysync.ui.viewmodels

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.model.SmsInboxInfo
import com.didahdx.smsgatewaysync.model.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.collections.ArrayList

class SmsInboxViewModel(application: Application) : ViewModel() {


    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)



    val app = application
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
        _messageCount.value=0
    }

    fun onMessageDetailClicked(id: SmsInboxInfo) {
        _eventMessageClicked.value = id
    }

    fun onMessageDetailNavigated() {
        _eventMessageClicked.value = null
    }

    private val _messageArrayList = MutableLiveData<ArrayList<SmsInboxInfo>>()
    val messageArrayList: LiveData<ArrayList<SmsInboxInfo>>
        get() = _messageArrayList


   private suspend fun setInputMessage(message: ArrayList<SmsInboxInfo>) {
       withContext(Main) {
           _messageArrayList.value=message
       }
    }

    suspend fun getDbSmsMessages() {
        val messageList = ArrayList<SmsInboxInfo>()
        val cursor = app.contentResolver?.query(
            Uri.parse("content://sms/inbox"),
            null,
            null,
            null,
            null
        )


        if (cursor != null && cursor.moveToNext()) {
            val nameId = cursor.getColumnIndex("address")
            val messageId = cursor.getColumnIndex("body")
            val dateId = cursor.getColumnIndex("date")
            val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

            var count=0
            Timber.i("mpesa sms $mpesaType ")

            do {
                val dateString = cursor.getString(dateId)

//                if (cursor.getString(nameId) == "MPESA") {

                var mpesaId: String =
                    cursor.getString(messageId).split("\\s".toRegex()).first().trim()
                if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
                    mpesaId = NOT_AVAILABLE
                }

                val maskedPhoneNumber=sharedPreferences.getBoolean(PREF_MASKED_NUMBER,false)
                val smsFilter = SmsFilter(cursor.getString(messageId),maskedPhoneNumber)

                when (mpesaType) {
                    PAY_BILL -> {
                        if (smsFilter.mpesaType == PAY_BILL) {
                            messageList.add(
                                SmsInboxInfo(
                                    messageId,
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    dateString.toLong(), true, "", ""
                                )
                            )
                            count++
                        }
                    }
                    DIRECT_MPESA -> {
                        if (smsFilter.mpesaType == DIRECT_MPESA) {
                            messageList.add(
                                SmsInboxInfo(
                                    messageId,
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    dateString.toLong(), true, "", ""
                                )
                            )
                            count++
                        }
                    }

                    BUY_GOODS_AND_SERVICES -> {
                        if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                            messageList.add(
                                SmsInboxInfo(
                                    messageId,
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    dateString.toLong(), true, "", ""
                                )
                            )
                            count++
                        }
                    }

                    else -> {
                        messageList.add(
                            SmsInboxInfo(
                                messageId,
                                cursor.getString(messageId),
                                sdf.format(Date(dateString.toLong())).toString(),
                                cursor.getString(nameId),
                                mpesaId, smsFilter.phoneNumber, smsFilter.amount,
                                smsFilter.accountNumber, smsFilter.name, dateString.toLong(),
                                true, "", ""
                            )
                        )
                        count++
                    }
                }

                setCount(count)

//                }
            } while (cursor.moveToNext())
            cursor.close()
        }

        setInputMessage(messageList)
    }

    private suspend fun setCount(count: Int) {
        withContext(Main){
            _messageCount.value=count
        }
    }

    fun setUpSmsInfo(it: SmsInboxInfo): SmsInfo {
        val smsStatus = if (it.status) {
            "Uploaded"
        } else {
            "pending"
        }

        return SmsInfo(it.messageBody, it.time, it.sender, smsStatus, it.longitude, it.latitude)
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(Dispatchers.IO).cancel()
    }
}