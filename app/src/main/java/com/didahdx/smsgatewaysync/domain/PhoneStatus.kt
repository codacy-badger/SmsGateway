package com.didahdx.smsgatewaysync.domain

data class PhoneStatus(
    val FreeRam: String,
    val FreeStorage: String,
    val PhoneBrand: String,
    val PhoneManufacturer: String,
    val PhoneModel: String,
    val TotalRam: String,
    val TotalStorage: String,
    val batteryChargingStatus: String,
    val batteryCondition: String,
    val batteryPercentage: String,
    val batteryPowerSource: String,
    val batteryTechnology: String,
    val batteryTemperature: String,
    val batteryVoltage: String,
    val client_gateway_type: String,
    val client_sender: String,
    val date: String,
    val imei: String,
    val imsi: String,
    val latitude: String,
    val longitude: String,
    val networkName: String,
    val simSerialNumber: String,
    val type: String
)