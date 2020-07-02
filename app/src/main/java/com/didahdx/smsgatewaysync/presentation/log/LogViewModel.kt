package com.didahdx.smsgatewaysync.presentation.log

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.utilities.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

class LogViewModel(application: Application, Logs: AppLog) : ViewModel() {

    val app = application
    private val logs = Logs
    private val _appLogs = MutableLiveData<String>()
    val appLogs: LiveData<String>
        get() = _appLogs

    init {
        CoroutineScope(IO).launch {
            getLogs()
        }
    }

    private fun getLogs() {
        CoroutineScope(IO).launch {
            val log = logs.readLog(app)
            _appLogs.postValue(log)

        }
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(IO).cancel()
    }

}