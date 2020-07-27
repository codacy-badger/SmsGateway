package com.didahdx.smsgatewaysync.presentation.log

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.data.db.LogInfoDao
import com.didahdx.smsgatewaysync.util.IOExecutor
import com.didahdx.smsgatewaysync.util.NOT_AVAILABLE
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber


class LogViewModel(application: Application, dbLogs: LogInfoDao) : ViewModel(), Handler.Callback {

    val app = application
    private val logs = dbLogs.getAllLogsByUserVisibility(true)
    private var mHandlerThread: HandlerThread = HandlerThread("SmsInboxViewModel HandlerThread")
    private var mMainThreadHandler: Handler = Handler(this)

    init {
        mHandlerThread.start()
    }

    //data to be passed to next screen
    private val _messageLogs = MutableLiveData<String>()
    val messageLogs: LiveData<String>
        get() = _messageLogs

    @AddTrace(name = "LogViewModelGetLogs")
    fun getLogs(): LiveData<String> {
        return Transformations.map(logs) {
            val backgroundHandler = Handler(mHandlerThread.looper)
            backgroundHandler.post(GetLogsRunnable(mMainThreadHandler, it))
            val allLogsempyt = StringBuilder()
            return@map allLogsempyt.toString()
        }
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(IO).cancel()
        mHandlerThread.quitSafely()
    }

    fun setLog(messageLogs: String) {
        _messageLogs.postValue(messageLogs)
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            GetLogsRunnable.FILTERED_DATA -> {
                Timber.d("Filtered data called")
                val messageLogs = msg.data.getString(GetLogsRunnable.FILTERED_DATA_STRING) ?: NOT_AVAILABLE
                CoroutineScope(IO).launch {
                    setLog(messageLogs)
                }
            }

            GetLogsRunnable.PROGRESS_COUNT_INT -> {
                Timber.d("Filtered data count called")
            }
        }
        return true
    }

}