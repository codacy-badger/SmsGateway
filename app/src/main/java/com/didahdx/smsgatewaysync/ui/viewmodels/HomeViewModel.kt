package com.didahdx.smsgatewaysync.ui.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.model.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.launch

class HomeViewModel(
    dataSource: IncomingMessagesDao,
    application: Application
) : ViewModel() {

    init {
    }


    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val database = dataSource

    private val incomingMessages = database.getAllMessages()

    val filteredMessages = Transformations.map(incomingMessages) {
        val messagesFilled = ArrayList<MpesaMessageInfo>()
        viewModelScope.launch {
            it?.let {
                val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)
                for (i in it.indices) {
                    val smsFilter = SmsFilter(it[i].messageBody)
                    when (mpesaType) {
                        PAY_BILL -> {
                            if (smsFilter.mpesaType == PAY_BILL) {
                                messagesFilled.add(it[i])
                            }
                        }

                        DIRECT_MPESA -> {
                            if (smsFilter.mpesaType == DIRECT_MPESA) {
                                messagesFilled.add(it[i])
                            }
                        }

                        BUY_GOODS_AND_SERVICES -> {
                            if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                                messagesFilled.add(it[i])
                            }
                        }
                        else -> {
                            messagesFilled.add(it[i])
                        }
                    }
                }
            }
        }
        return@map messagesFilled.toList()
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

}
