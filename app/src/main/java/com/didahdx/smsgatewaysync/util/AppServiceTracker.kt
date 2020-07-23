package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.content.SharedPreferences

enum class ServiceState {
    STARTING,
    RUNNING,
    STOPPED,
}


fun setServiceState(context: Context, state: ServiceState) {
    SpUtil.setPreferenceString(context, APP_SERVICE_STATE, state.name)
}

fun getServiceState(context: Context): ServiceState {
    val value = SpUtil.getPreferenceString(context, APP_SERVICE_STATE, ServiceState.STOPPED.name)
    return ServiceState.valueOf(value)
}

fun setRestartServiceState(context: Context, value: Boolean) {
    SpUtil.setPreferenceBoolean(context, RESTART_SERVICE_STATE, value)
}

fun getRestartServiceState(context: Context): Boolean {
    return SpUtil.getPreferenceBoolean(context, RESTART_SERVICE_STATE)
}
