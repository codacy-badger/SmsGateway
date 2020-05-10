package com.didahdx.smsgatewaysync.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.didahdx.smsgatewaysync.data.db.IncomingMessagesDao
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase

class HomeViewModelFactory(
    private val dataSource: IncomingMessagesDao,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }


}