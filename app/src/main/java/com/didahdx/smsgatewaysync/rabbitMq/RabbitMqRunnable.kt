package com.didahdx.smsgatewaysync.rabbitMq

import android.content.Context
import androidx.work.*
import com.didahdx.smsgatewaysync.R
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

class RabbitMqRunnable(private  var rabbitMqClient:RabbitmqClient,
    private val context: Context, private val email: String,
    private val uiUpdaterInterface: UiUpdaterInterface, private val connect: Boolean
) :
    Runnable {

    @AddTrace(name = "RabbitMqRunnable_run")
    override fun run() {
        Timber.d(" ${Thread.currentThread().name} ")
        when (connect) {
            true -> {
                val isWifiOnly =
                    SpUtil.getPreferenceBoolean(
                        context,
                        context.getString(R.string.preference_wifi_only)
                    )
                if (isWifiOnly) {
                    if (Connectivity.getConnectionType(context) == context.getString(R.string.Wifi)) {
                        setUpConnection()
                    }
                } else {
                    setUpConnection()
                }
            }
            false -> {
                rabbitMqClient.disconnect()
            }
        }
    }

    private fun setUpConnection() {
        AppLog.logMessage("Sms Service started", context, true)
        val urlEnabled = SpUtil.getPreferenceBoolean(context, PREF_HOST_URL_ENABLED)
        val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
        if (isServiceOn && !urlEnabled) {
            setServiceState(context, ServiceState.STARTING)
            rabbitMqClient.connection(context)
            Timber.d("The service has been created".toUpperCase(Locale.getDefault()))
        }
        cancelPing()
        setupRabbitMqPing()
    }


    @AddTrace(name = "AppServicesSetupRabbitMqPing")
    private fun setupRabbitMqPing() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
        val logFormat = LogFormat(
            date = Date().toString(),
            type = "logs",
            client_gateway_type = ANDROID_PHONE,
            log = "ping",
            client_sender = email,
            isUserVisible = true,
            isUploaded = true
        )

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, Gson().toJson(logFormat))
            .putString(KEY_EMAIL, email)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<SendRabbitMqWorker>(18, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        Timber.d("WorkManager: Periodic Work request for sync is scheduled")
        val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SendRabbitMqWorker.PING_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
            )
    }

    private fun cancelPing() {
        WorkManager.getInstance(context).cancelAllWorkByTag(SendRabbitMqWorker.PING_WORK_NAME)
    }

}