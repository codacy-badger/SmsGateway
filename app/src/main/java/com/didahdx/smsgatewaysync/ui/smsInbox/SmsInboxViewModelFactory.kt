package com.didahdx.smsgatewaysync.ui.smsInbox

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao

class SmsInboxViewModelFactory(
    private val dataSource: IncomingMessagesDao,
    private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmsInboxViewModel::class.java)) {
            return SmsInboxViewModel(
                dataSource,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}