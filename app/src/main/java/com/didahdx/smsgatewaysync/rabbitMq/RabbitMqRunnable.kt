package com.didahdx.smsgatewaysync.rabbitMq

import android.content.Context
import androidx.work.*
import com.didahdx.smsgatewaysync.domain.LogFormat
import com.didahdx.smsgatewaysync.presentation.UiUpdaterInterface
import com.didahdx.smsgatewaysync.util.*
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.metrics.AddTrace
import com.google.gson.Gson
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class RabbitMqRunnable(context: Context, email: String, uiUpdaterInterface: UiUpdaterInterface) :
    Runnable {
    private val mContext = context
    private val mEmail = email
    private val updaterInterface = uiUpdaterInterface

    @AddTrace(name="RabbitMqRunnable_run")
    override fun run() {
        AppLog.logMessage("Sms Service started", mContext,true)

        Timber.d(" ${Thread.currentThread().name} ")
        val rabbitMqClient = RabbitmqClient(updaterInterface, mEmail)
        val urlEnabled = SpUtil.getPreferenceBoolean(mContext, PREF_HOST_URL_ENABLED)
        val isServiceOn = SpUtil.getPreferenceBoolean(mContext, PREF_SERVICES_KEY)
        if (isServiceOn && !urlEnabled) {
            setServiceState(mContext, ServiceState.STARTING)
            rabbitMqClient.connection(mContext)
            Timber.d("The service has been created".toUpperCase(Locale.getDefault()))
        }
        cancelPing()
        setupRabbitMqPing()
    }


    @AddTrace(name="AppServicesSetupRabbitMqPing")
    private fun setupRabbitMqPing() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
        val logFormat = LogFormat(
            date = Date().toString(),
            type = "logs",
            client_gateway_type = ANDROID_PHONE,
            log = "ping",
            client_sender = email,
            isUserVisible = true,
            isUploaded =true
        )

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, Gson().toJson(logFormat))
            .putString(KEY_EMAIL, mEmail)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<SendRabbitMqWorker>(18, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        Timber.d("WorkManager: Periodic Work request for sync is scheduled")
        val isServiceOn = SpUtil.getPreferenceBoolean(mContext, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(mContext).enqueueUniquePeriodicWork(
                SendRabbitMqWorker.PING_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest)
    }

    private fun cancelPing() {
        WorkManager.getInstance(mContext).cancelAllWorkByTag(SendRabbitMqWorker.PING_WORK_NAME)
    }

}