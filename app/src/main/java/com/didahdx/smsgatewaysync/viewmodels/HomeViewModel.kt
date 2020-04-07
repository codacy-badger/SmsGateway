package com.didahdx.smsgatewaysync.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.didahdx.smsgatewaysync.model.MpesaMessageInfo

class HomeViewModel :ViewModel(){
    private val mMessageInfo=MutableLiveData<ArrayList<MpesaMessageInfo>>()

    fun getMessages():LiveData<ArrayList<MpesaMessageInfo>>{
        return mMessageInfo
    }
}