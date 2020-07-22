package com.didahdx.smsgatewaysync.presentation.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.didahdx.smsgatewaysync.data.db.LogInfoDao
import com.didahdx.smsgatewaysync.data.db.entities.LogInfo
import com.didahdx.smsgatewaysync.utilities.AppLog

@Suppress("UNCHECKED_CAST")
class LogViewModelFactory(
    private val application: Application,
    private val Logs: LogInfoDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            return LogViewModel(
                application,Logs
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }


}