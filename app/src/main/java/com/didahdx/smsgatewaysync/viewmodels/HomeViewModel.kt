package com.didahdx.smsgatewaysync.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.model.MessageInfo

class HomeViewModel :ViewModel(){
    private val mMessageInfo=MutableLiveData<ArrayList<MessageInfo>>()

    fun getMessages():LiveData<ArrayList<MessageInfo>>{
        return mMessageInfo
    }
}