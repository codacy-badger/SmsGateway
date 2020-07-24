package com.didahdx.smsgatewaysync.presentation.home

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.util.toast
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import timber.log.Timber


class HomeViewModel(
    dataSource: IncomingMessagesDao,
    application: Application
) : ViewModel(), Handler.Callback {

    private var mHandlerThread: HandlerThread = HandlerThread("HomeViewModel HandlerThread")
    private var mMainThreadHandler: Handler = Handler(this)

    //data to be passed to next screen
    private val _messageCount = MutableLiveData<Int>()
    val messageCount: LiveData<Int>
        get() = _messageCount

    //data to be passed to next screen
    private val _messageList = MutableLiveData<List<MpesaMessageInfo>>()
    val messageList: LiveData<List<MpesaMessageInfo>>
        get() = _messageList

    // the Coroutine runs using the Main (UI) dispatcher
//    private val coroutineScope = CoroutineScope(viewModelJob + Main)
    private val database = dataSource
    private val app = application
    private var incomingMessages = database.getAllMessages()

    init {
        mHandlerThread.start()
    }

    @AddTrace(name = "HomeViewModelGetFilteredData")
    fun getFilteredData(): LiveData<List<MpesaMessageInfo>> {

        return Transformations.map(incomingMessages) {
            val messagesFilledNew = ArrayList<MpesaMessageInfo>()
            val backgroundHandler = Handler(mHandlerThread.looper)
            backgroundHandler.post(
                FilterDataRunnable(
                    app, mMainThreadHandler,
                    it as ArrayList<MpesaMessageInfo>
                )
            )

            return@map messagesFilledNew.toList()
        }
    }


    private fun setCount(count: Int) {
        _messageCount.postValue(count)
    }

    //data to be passed to next screen
    private val _eventMessageClicked = MutableLiveData<MpesaMessageInfo>()
    val eventMessageClicked: LiveData<MpesaMessageInfo>
        get() = _eventMessageClicked

    fun onMessageDetailClicked(id: MpesaMessageInfo) {
        _eventMessageClicked.value = id
    }

    fun onMessageDetailNavigated() {
        _eventMessageClicked.value = null
    }

    fun setUpSmsInfo(it: MpesaMessageInfo): SmsInfo {
        val smsStatus = if (it.status) {
            "Uploaded"
        } else {
            "pending"
        }
        return SmsInfo(it.messageBody, it.time, it.sender, smsStatus, it.longitude, it.latitude)
    }


    fun refreshIncomingDatabase() {
        incomingMessages = database.getAllMessages()
    }


    override fun onCleared() {
        super.onCleared()
        CoroutineScope(IO).cancel()
        mHandlerThread.quitSafely()
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {

            FilterDataRunnable.FILTERED_DATA -> {
                app.toast("Filtered data called")
                Timber.d("Filtered data called")
                val mpesaMessages: ArrayList<MpesaMessageInfo> = ArrayList<MpesaMessageInfo>(
                    msg.data.getParcelableArrayList(FilterDataRunnable.FILTERED_DATA_STRING)!!
                )
                _messageList.postValue(mpesaMessages.toList())
            }

            FilterDataRunnable.PROGRESS_COUNT_INT -> {
                Timber.d("Filtered data count called")
                setCount(msg.data.getInt(FilterDataRunnable.PROGRESS_COUNT_STRING))
            }
        }

        return true
    }


}