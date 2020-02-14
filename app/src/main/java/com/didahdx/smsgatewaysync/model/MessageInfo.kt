package com.didahdx.smsgatewaysync.model

data class MessageInfo (
    val messageBody:String,
    val time:String,
    val sender:String,
    val mpesaId:String
)