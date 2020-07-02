package com.didahdx.smsgatewaysync.presentation.home

import android.app.Application
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


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

    // Create a Coroutine scope using a job to be able to cancel when needed
    private var viewModelJob = Job()
    val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)

    // the Coroutine runs using the Main (UI) dispatcher
//    private val coroutineScope = CoroutineScope(viewModelJob + Main)
    private val database = dataSource
    private val app = application
    private var incomingMessages = database.getAllMessages()

    fun getFilteredData(): LiveData<List<MpesaMessageInfo>> {
        return Transformations.map(incomingMessages) {
            val messagesFilledNew = ArrayList<MpesaMessageInfo>()
            CoroutineScope(IO).launch {
                val messagesFilled = ArrayList<MpesaMessageInfo>()
                it?.let {
                    val mpesaType = SpUtil.getPreferenceString(app,PREF_MPESA_TYPE, DIRECT_MPESA)
                    var count = 0
                    val maskedPhoneNumber = SpUtil.getPreferenceBoolean(app,PREF_MASKED_NUMBER)
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

                withContext(Main) {
                    messagesFilledNew.addAll(messagesFilled)
                }
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


    /**
     * When the [ViewModel] is finished, we cancel our coroutine [viewModelJob], which tells the
     * Retrofit service to stop.
     */
    override fun onCleared() {
        super.onCleared()
        CoroutineScope(IO).cancel()
        viewModelJob.cancel()
    }

}