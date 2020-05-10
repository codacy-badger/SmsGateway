package com.didahdx.smsgatewaysync.ui.viewmodels

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SmsInboxViewModelFactory( private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmsInboxViewModel::class.java)) {
            return SmsInboxViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}