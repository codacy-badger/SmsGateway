package com.didahdx.smsgatewaysync.ui.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.model.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    dataSource: IncomingMessagesDao,
    application: Application
) : ViewModel() {

    //data to be passed to next screen
    private val _messageCount = MutableLiveData<Int>()
    val messageCount: LiveData<Int>
        get() = _messageCount


    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val database = dataSource

    private var incomingMessages = database.getAllMessages()

    val filteredMessages = Transformations.map(incomingMessages) {
        val messagesFilled = ArrayList<MpesaMessageInfo>()
        viewModelScope.launch {
            it?.let {
                val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)
                var count = 0
                for (i in it.indices) {
                    val smsFilter = SmsFilter(it[i].messageBody)
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
                    setCount(count)
                }
            }
        }
        return@map messagesFilled.toList()
    }


    private suspend fun setCount(count: Int) {
        withContext(Dispatchers.Main) {
            _messageCount.value = count
        }
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

}
