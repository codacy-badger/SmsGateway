package com.didahdx.smsgatewaysync.model

data class MqttConnectionParam(val clientId:String, val host: String, val topic: String, val username: String, val password: String) {
}