package com.didahdx.smsgatewaysync.presentation.smsInbox

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.util.NOT_AVAILABLE
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import timber.log.Timber

class SmsInboxViewModel(dataSource: IncomingMessagesDao, application: Application) : ViewModel(),
    Handler.Callback {

    val app = application
    val database = dataSource

    private var mHandlerThread: HandlerThread = HandlerThread("SmsInboxViewModel HandlerThread")
    private var mMainThreadHandler: Handler = Handler(this)

    //data to be passed to next screen
    private val _eventMessageClicked = MutableLiveData<SmsInboxInfo>()
    val eventMessageClicked: LiveData<SmsInboxInfo>
        get() = _eventMessageClicked

    //data to be passed to next screen
    private val _messageCount = MutableLiveData<Int>()
    val messageCount: LiveData<Int>
        get() = _messageCount

    init {
        mHandlerThread.start()
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

    @AddTrace(name = "SmsInboxViewModel_GetDbSmsMessages")
    fun getDbSmsMessages() {
        val backgroundHandler = Handler(mHandlerThread.looper)
        backgroundHandler.post(FilterDataSmsInboxRunnable(app, mMainThreadHandler))
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
        mHandlerThread.quitSafely()
    }

    fun getAllDbSms() {
        val importAllMessage = ImportAllDbMessageRunnable(database, app)
//        Thread(importAllMessage).start()
        val backgroundHandler = Handler(mHandlerThread.looper)
        backgroundHandler.post(importAllMessage)
    }


    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {

            FilterDataSmsInboxRunnable.FILTERED_DATA -> {
                Timber.d("Filtered data called")
                val mpesaMessages: ArrayList<SmsInboxInfo> = ArrayList<SmsInboxInfo>(
                    msg.data.getParcelableArrayList(FilterDataSmsInboxRunnable.FILTERED_DATA_STRING)!!
                )
                setInputMessage(mpesaMessages)

            }

            FilterDataSmsInboxRunnable.PROGRESS_COUNT_INT -> {
                Timber.d("Filtered data count called")
                setCount(msg.data.getInt(FilterDataSmsInboxRunnable.PROGRESS_COUNT_STRING))
            }
        }
        return true
    }


}