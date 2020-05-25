package com.didahdx.smsgatewaysync.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.data.network.PostSms
import com.didahdx.smsgatewaysync.data.network.SmsApi
import com.didahdx.smsgatewaysync.model.SmsInboxInfo
import com.didahdx.smsgatewaysync.model.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.lang.reflect.Method
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

    // Create a Coroutine scope using a job to be able to cancel when needed
    private var viewModelJob = Job()
    val sdf = SimpleDateFormat(DATE_FORMAT)

    // the Coroutine runs using the Main (UI) dispatcher
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val database = dataSource
    private val app = application
    private var incomingMessages = database.getAllMessages()

    val filteredMessages = Transformations.map(incomingMessages) {
        val messagesFilled = ArrayList<MpesaMessageInfo>()
        viewModelScope.launch {
            it?.let {
                val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)
                var count = 0
                val maskedPhoneNumber = sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)
                for (i in it.indices) {
                    val smsFilter = SmsFilter(it[i].messageBody, maskedPhoneNumber)
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

    //used to hangUpCall
    fun hangUpCall() {
        val hangup = sharedPreferences.getBoolean(PREF_HANG_UP, false)
        if (hangup) {
            val tm = app.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tm.endCall()
            } else {
                disconnectCall()
            }
        }
    }


    @SuppressLint("PrivateApi")
    private fun disconnectCall() {
        try {
            val serviceManagerName = "android.os.ServiceManager"
            val serviceManagerNativeName = "android.os.ServiceManagerNative"
            val telephonyName = "com.android.internal.telephony.ITelephony"
            val telephonyClass: Class<*>
            val telephonyStubClass: Class<*>
            val serviceManagerClass: Class<*>
            val serviceManagerNativeClass: Class<*>
            val telephonyEndCall: Method
            val telephonyObject: Any
            val serviceManagerObject: Any
            telephonyClass = Class.forName(telephonyName)
            telephonyStubClass = telephonyClass.classes[0]
            serviceManagerClass = Class.forName(serviceManagerName)
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName)
            val getService =  // getDefaults[29];
                serviceManagerClass.getMethod("getService", String::class.java)
            val tempInterfaceMethod = serviceManagerNativeClass.getMethod(
                "asInterface",
                IBinder::class.java
            )
            val tmpBinder = Binder()
            tmpBinder.attachInterface(null, "fake")
            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder)
            val retbinder = getService.invoke(serviceManagerObject, "phone") as IBinder
            val serviceMethod = telephonyStubClass.getMethod(
                "asInterface",
                IBinder::class.java
            )
            telephonyObject = serviceMethod.invoke(null, retbinder)
            telephonyEndCall = telephonyClass.getMethod("endCall")
            telephonyEndCall.invoke(telephonyObject)
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.i(
                "FATAL ERROR: could not connect to telephony subsystem"
            )
            Timber.i("Exception object: $e  ${e.localizedMessage}")
        }
    }


    /**
     * When the [ViewModel] is finished, we cancel our coroutine [viewModelJob], which tells the
     * Retrofit service to stop.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}