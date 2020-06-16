package com.didahdx.smsgatewaysync.model

data class SmsInboxInfo(
    val id: Int,
    val messageBody: String,
    val time: String,
    val sender: String,
    val mpesaId: String,
    val receiver: String,
    val amount: String,
    val accountNumber: String,
    val name: String,
    val date: Long,
    val status: Boolean,
    val longitude: String,
    val latitude: String
)