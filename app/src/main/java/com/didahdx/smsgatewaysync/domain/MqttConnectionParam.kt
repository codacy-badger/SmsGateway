package com.didahdx.smsgatewaysync.domain

data class MqttConnectionParam(
    val clientId: String,
    val host: String,
    val topic: String,
    val username: String,
    val password: String
) {
}