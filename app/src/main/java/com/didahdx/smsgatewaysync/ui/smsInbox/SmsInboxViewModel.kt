package com.didahdx.smsgatewaysync.ui.smsInbox

import android.app.Application
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.model.SmsInboxInfo
import com.didahdx.smsgatewaysync.model.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import timber.log.Timber
import java.text.SimpleDateFormat

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


    private suspend fun setInputMessage(message: Cursor) {
        withContext(Main) {
            app.toast(" Db Cursot ${message.count}")
            _messageArrayList.value = message
        }
    }


    suspend fun getDbSmsMessages() {
        val cursor = app.contentResolver?.query(
            Uri.parse("content://sms/inbox"),
            null,
            null,
            null,
            null
        )

        val smsItem = ArrayList<String>()

        if (cursor != null && cursor.moveToNext()) {
            val nameId = cursor.getColumnIndex("address")
            val messageId = cursor.getColumnIndex("body")
            val dateId = cursor.getColumnIndex("date")
            val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

            Timber.i("mpesa sms $mpesaType ")

            do {
                val dateString = cursor.getString(dateId)

//                if (cursor.getString(nameId) == "MPESA") {

                var mpesaId: String =
                    cursor.getString(messageId).split("\\s".toRegex()).first().trim()
                if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
                    mpesaId = NOT_AVAILABLE
                }

                val maskedPhoneNumber = sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)
                val smsFilter = SmsFilter(cursor.getString(messageId), maskedPhoneNumber)

                when (mpesaType) {
                    PAY_BILL -> {
                        if (smsFilter.mpesaType == PAY_BILL) {
                            cursor?.position?.let { smsItem.add(dateString) }
                        }
                    }
                    DIRECT_MPESA -> {
                        if (smsFilter.mpesaType == DIRECT_MPESA) {
                            cursor?.position?.let { smsItem.add(dateString) }
                        }
                    }

                    BUY_GOODS_AND_SERVICES -> {
                        if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                            cursor?.position?.let { smsItem.add(dateString) }
                        }
                    }

                    else -> {
                        cursor?.position?.let { smsItem.add(dateString) }
                    }
                }
//                }
            } while (cursor.moveToNext())
            cursor.close()
        }

        sortCursor(smsItem)
    }


    private suspend fun sortCursor(rowNumbers: ArrayList<String>) {
        val projections = arrayOf("address", "body", "date")
        var inClause: String = rowNumbers.toString()
        inClause = inClause.replace('[', '(');
        inClause = inClause.replace(']', ')');
        val selection = "date IN  $inClause"

        val cursor = app.contentResolver?.query(
            Uri.parse("content://sms/inbox"),
            projections,
            selection,
            null,
            null
        )

        cursor?.let { setInputMessage(it) }
    }


    private suspend fun setCount(count: Int) {
        withContext(Main) {
            _messageCount.value = count
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

    fun getCursor(): Cursor? {
        var mCursor: Cursor? = null
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
                val nameId = cursor.getColumnIndex("address")
                val messageId = cursor.getColumnIndex("body")
                val dateId = cursor.getColumnIndex("date")
                val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

                Timber.i("mpesa sms $mpesaType ")

                do {
                    val dateString = cursor.getString(dateId)

//                if (cursor.getString(nameId) == "MPESA") {

                    var mpesaId: String =
                        cursor.getString(messageId).split("\\s".toRegex()).first().trim()
                    if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
                        mpesaId = NOT_AVAILABLE
                    }

                    val maskedPhoneNumber = sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)
                    val smsFilter = SmsFilter(cursor.getString(messageId), maskedPhoneNumber)

                    when (mpesaType) {
                        PAY_BILL -> {
                            if (smsFilter.mpesaType == PAY_BILL) {
                                cursor?.position?.let { smsItem.add(dateString) }
                            }
                        }
                        DIRECT_MPESA -> {
                            if (smsFilter.mpesaType == DIRECT_MPESA) {
                                cursor?.position?.let { smsItem.add(dateString) }
                            }
                        }

                        BUY_GOODS_AND_SERVICES -> {
                            if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                                cursor?.position?.let { smsItem.add(dateString) }
                            }
                        }

                        else -> {
                            cursor?.position?.let { smsItem.add(dateString) }
                        }
                    }
//                }
                } while (cursor.moveToNext())
                cursor.close()
            }

            val projections = arrayOf("address", "body", "date")
            var inClause: String = smsItem.toString()
            inClause = inClause.replace('[', '(');
            inClause = inClause.replace(']', ')');
            val selection = "date IN  $inClause"
            val selectionArgs: String = smsItem.toArray().toString()

           val valueCursor = app.contentResolver?.query(
                Uri.parse("content://sms/inbox"),
                projections,
                selection,
                null,
                null
            )

            withContext(Main){
                mCursor=valueCursor
            }

        }
        return mCursor
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(Dispatchers.IO).cancel()
    }
}