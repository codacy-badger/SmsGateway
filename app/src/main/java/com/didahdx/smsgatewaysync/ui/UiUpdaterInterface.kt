package com.didahdx.smsgatewaysync.ui

interface UiUpdaterInterface {
     fun isConnected(value:Boolean)
     fun notificationMessage(message: String)
     fun toasterMessage(message: String)
     fun updateStatusViewWith(status: String,color:String)
     fun publish(isReadyToPublish:Boolean)
     fun sendSms(phoneNumber: String, message: String)
}