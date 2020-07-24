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
import com.didahdx.smsgatewaysync.util.*
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class HomeViewModel(
    dataSource: IncomingMessagesDao,
    application: Application
) : ViewModel() {

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


    @AddTrace(name = "HomeViewModelGetFilteredData")
    fun getFilteredData(): LiveData<List<MpesaMessageInfo>> {

        return Transformations.map(incomingMessages) {
            val messagesFilledNew = ArrayList<MpesaMessageInfo>()
            CoroutineScope(IO).launch {
                val messagesFilled = ArrayList<MpesaMessageInfo>()
                it?.let {
                    val mpesaType = SpUtil.getPreferenceString(app, PREF_MPESA_TYPE, DIRECT_MPESA)
                    var count = 0
                    val maskedPhoneNumber = SpUtil.getPreferenceBoolean(app, PREF_MASKED_NUMBER)
                    for (i in it.indices) {
                        val smsFilter = SmsFilter(it[i].messageBody, maskedPhoneNumber)
                        setCount(count)
                        when (mpesaType) {
                            PAY_BILL -> {
                                if (smsFilter.mpesaType == PAY_BILL) {
                                    messagesFilled.add(it[i])
                                    count++
                                }
                            }

                            DIRECT_MPESA -> {
                                if (smsFilter.mpesaType == DIRECT_MPESA) {
                                    messagesFilled.add(it[i])
                                    count++
                                }
                            }

                            BUY_GOODS_AND_SERVICES -> {
                                if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                                    messagesFilled.add(it[i])
                                    count++
                                }
                            }
                            else -> {
                                messagesFilled.add(it[i])
                                count++
                            }
                        }
                    }
                }
                _messageList.postValue(messagesFilled.toList())
                messagesFilledNew.addAll(messagesFilled)
            }
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
    }


}