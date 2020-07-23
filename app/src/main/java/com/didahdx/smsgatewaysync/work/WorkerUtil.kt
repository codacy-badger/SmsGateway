package com.didahdx.smsgatewaysync.work

import android.content.Context
import androidx.work.*
import com.didahdx.smsgatewaysync.util.PREF_SERVICES_KEY
import com.didahdx.smsgatewaysync.util.SpUtil
import java.util.*

object WorkerUtil {

     fun sendToRabbitMQ(context: Context, data: Data): UUID {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendRabbitMqWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(context).enqueue(request)

         return request.id
    }

     fun sendSms(data: Data,context: Context) {
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendSmsWorker>()
            .setInputData(data)
            .build()
        val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(context).enqueue(request)
    }


     fun sendToApi(data: Data,context: Context) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendApiWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()
        val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
        if (isServiceOn)
            WorkManager.getInstance(context).enqueue(request)
    }

}