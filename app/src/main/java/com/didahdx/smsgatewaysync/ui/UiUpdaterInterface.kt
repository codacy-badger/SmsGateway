package com.didahdx.smsgatewaysync.ui

interface UiUpdaterInterface {
    fun toasterMessage(message: String)
    fun updateStatusViewWith(status: String,color:String)
    fun publish(isReadyToPublish:Boolean)
}