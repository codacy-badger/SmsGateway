package com.didahdx.smsgatewaysync.presentation.log

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.data.db.LogInfoDao
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.lang.StringBuilder

class LogViewModel(application: Application, dbLogs: LogInfoDao) : ViewModel() {

    val app = application
    private val logs = dbLogs.getAllLogsByUserVisibility(true)

    //data to be passed to next screen
    private val _messageLogs = MutableLiveData<String>()
    val messageLogs: LiveData<String>
        get() = _messageLogs

    @AddTrace(name="LogViewModelGetLogs")
    fun getLogs(): LiveData<String> {
        return Transformations.map(logs) {
            val allLogsempyt = StringBuilder()
             it?.let {
                 CoroutineScope(IO).launch {
                     val allLogs = StringBuilder()
                     for (i in it.indices) {
                         allLogs.append(it[i].toString())
                     }
                     _messageLogs.postValue(allLogs.toString())
                 }
             }

            return@map allLogsempyt.toString()
         }
    }


    override fun onCleared() {
        super.onCleared()
        CoroutineScope(IO).cancel()
    }

}