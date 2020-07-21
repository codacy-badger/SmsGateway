package com.didahdx.smsgatewaysync.manager

import android.content.Context
import com.didahdx.smsgatewaysync.presentation.UiUpdaterInterface
import com.didahdx.smsgatewaysync.utilities.*
import timber.log.Timber
import java.util.*

class RabbitMqRunnable(context: Context, email: String, uiUpdaterInterface: UiUpdaterInterface) :
    Runnable {
    private val mContext = context
    private val mEmail = email
    private val updaterInterface = uiUpdaterInterface
    override fun run() {
        Timber.d(" ${Thread.currentThread().name} ")
        val rabbitMqClient = RabbitmqClient(updaterInterface, mEmail)
        val urlEnabled = SpUtil.getPreferenceBoolean(mContext, PREF_HOST_URL_ENABLED)
        val isServiceOn = SpUtil.getPreferenceBoolean(mContext, PREF_SERVICES_KEY)
        if (isServiceOn && !urlEnabled) {
            setServiceState(mContext, ServiceState.STARTING)
            rabbitMqClient.connection(mContext)
            Timber.d("The service has been created".toUpperCase(Locale.getDefault()))
        }
    }

}