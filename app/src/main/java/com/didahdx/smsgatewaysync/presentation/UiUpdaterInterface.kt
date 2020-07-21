package com.didahdx.smsgatewaysync.presentation

interface UiUpdaterInterface {
    fun notificationMessage(message: String)
    fun toasterMessage(message: String)
    fun updateStatusViewWith(status: String, color: String)
    fun sendSms(phoneNumber: String, message: String)
    fun updateSettings(preferenceType:String,key:String,value:String)
    fun logMessage(message: String)
}