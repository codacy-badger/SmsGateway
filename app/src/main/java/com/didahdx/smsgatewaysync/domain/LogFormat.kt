package com.didahdx.smsgatewaysync.domain

data class LogFormat(
    val date: String,
    val type: String,
    val log: String,
    val client_gateway_type: String,
    val client_sender: String,
    val isUserVisible: Boolean,
    val isUploaded:Boolean
)