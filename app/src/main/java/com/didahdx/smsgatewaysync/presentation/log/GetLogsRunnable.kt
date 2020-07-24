package com.didahdx.smsgatewaysync.presentation.log

import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.didahdx.smsgatewaysync.data.db.entities.LogInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.lang.ref.WeakReference

class GetLogsRunnable(
    mainThreadHandler: Handler,
    logInfo: List<LogInfo>
) : Runnable {

    companion object {
        const val PROGRESS_COUNT_INT = 120
        const val PROGRESS_COUNT_STRING = "PROGRESS_COUNT_STRING"
        const val FILTERED_DATA = 210
        const val FILTERED_DATA_STRING = "FILTERED_DATA_STRING"
    }

    private var mMainThreadHandler: WeakReference<Handler>? = null
    private var logsData = ArrayList<LogInfo>()

    init {
        mMainThreadHandler = WeakReference(mainThreadHandler)
        logsData.addAll(logInfo)
    }

    override fun run() {
        logsData.let {
            CoroutineScope(Dispatchers.IO).launch {
                val allLogs = StringBuilder()
                for (i in it.indices) {
                    allLogs.append(it[i].toString())
                }
                val message = Message.obtain(null, FILTERED_DATA)
                val bundle = Bundle()
                bundle.putString(FILTERED_DATA_STRING,allLogs.toString())
                message.data = bundle
                mMainThreadHandler?.get()?.sendMessage(message)
            }
        }

    }


}