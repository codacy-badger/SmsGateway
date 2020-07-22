package com.didahdx.smsgatewaysync.presentation.log

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.data.db.LogInfoDao
import com.didahdx.smsgatewaysync.utilities.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.lang.StringBuilder

class LogViewModel(application: Application, dbLogs: LogInfoDao) : ViewModel() {

    val app = application
    private val logs = dbLogs.getAllLogsByUserVisibility(true)

   val presentlog= Transformations.map(logs) {
       val allLogs=StringBuilder()
       for (i in it.indices){
          allLogs.append(it[i].toString())
       }
       return@map allLogs.toString()
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(IO).cancel()
    }

}