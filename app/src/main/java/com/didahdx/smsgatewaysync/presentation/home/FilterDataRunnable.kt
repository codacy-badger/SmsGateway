package com.didahdx.smsgatewaysync.presentation.home

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.util.*
import timber.log.Timber
import java.lang.ref.WeakReference


class FilterDataRunnable(
    application: Application,
    mainThreadHandler: Handler,
    data: ArrayList<MpesaMessageInfo>
) : Runnable {


    companion object {
        const val PROGRESS_COUNT_INT = 100
        const val PROGRESS_COUNT_STRING = "PROGRESS_COUNT_STRING"
        const val FILTERED_DATA = 200
        const val FILTERED_DATA_STRING = "FILTERED_DATA_STRING"
    }


    private var mMainThreadHandler: WeakReference<Handler>? = null
    private val filterData = ArrayList<MpesaMessageInfo>()
    private val app = application

    init {
        filterData.addAll(data)
        mMainThreadHandler = WeakReference(mainThreadHandler)
    }

    override fun run() {
        Timber.d("Filtered Data Runnable Thread name ${Thread.currentThread().name}")
        Timber.d("passed data ${Thread.currentThread().name}")
        val messagesFilled = ArrayList<MpesaMessageInfo>()

        filterData?.let {
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
        val message = Message.obtain(null, FILTERED_DATA)
        val bundle = Bundle()
        bundle.putParcelableArrayList(FILTERED_DATA_STRING, messagesFilled)
        message.data = bundle
        mMainThreadHandler?.get()?.sendMessage(message)
//            _messageList.postValue(messagesFilled.toList())
//
//            messagesFilledNew.addAll(messagesFilled)

    }

    private fun setCount(count: Int) {
        Timber.d("SetCount $count")
        val message = Message.obtain(null, PROGRESS_COUNT_INT)
        val bundle = Bundle()
        bundle.putInt(PROGRESS_COUNT_STRING, count)
        message.data = bundle
        mMainThreadHandler?.get()?.sendMessage(message)
    }
}



