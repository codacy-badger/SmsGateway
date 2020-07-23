package com.didahdx.smsgatewaysync.util

import android.content.Context
import androidx.work.Data
import androidx.work.WorkManager
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.LogInfo
import com.didahdx.smsgatewaysync.domain.LogFormat
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.didahdx.smsgatewaysync.work.WorkerUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.metrics.AddTrace
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*

/**
 * used to log events
 * */

object AppLog {

    //writing to log messages
    @AddTrace(name = "AppLog_logMessage")
    fun logMessage(message: String, context: Context, isUserVisible: Boolean=true) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
        val logFormat = LogFormat(
            date = Date().toString(),
            type = "logs",
            client_gateway_type = ANDROID_PHONE,
            log = message,
            client_sender = email,
            isUploaded = false,
            isUserVisible = isUserVisible
        )

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, Gson().toJson(logFormat))
            .putString(KEY_EMAIL, email)
            .build()

        CoroutineScope(IO).launch {
            MessagesDatabase(context).getLogInfoDao().addLogInfo(
                LogInfo(
                    logFormat.date,
                    logFormat.type,
                    logFormat.log,
                    logFormat.client_gateway_type,
                    logFormat.client_sender,
                    logFormat.isUserVisible,
                    logFormat.isUploaded
                )
            )
            val uuid = WorkerUtil.sendToRabbitMQ(context, data)

        }
    }
}