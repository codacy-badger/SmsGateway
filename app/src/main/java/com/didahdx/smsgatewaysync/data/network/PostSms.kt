package com.didahdx.smsgatewaysync.data.network

data class PostSms(
    val account_number: String,
    val amount: Double,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val name: String,
    val phone_number: String,
    val receipt_date: String,
    val sender_id: String,
    val transaction_date: String,
    val transaction_type: String,
    val voucher_number: String
)