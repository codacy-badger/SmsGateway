package com.didahdx.smsgatewaysync.model

data class MpesaMessageInfo(
    val messageBody: String,
    val time: String,
    val sender: String,
    val mpesaId: String,
    val receiver: String,
    val amount: String,
    val accountNumber: String,
    val name: String,
    val dateTime:Long
)