package com.didahdx.smsgatewaysync.domain

import com.didahdx.smsgatewaysync.util.ANDROID_PHONE

data class CallStatus(
    val call_type: String,
    val client_gateway_type: String= ANDROID_PHONE,
    val client_sender: String,
    val end_time: String,
    val latitude: String,
    val longitude: String,
    val phone_number: String,
    val start_time: String,
    val type: String
)