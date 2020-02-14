package com.didahdx.smsgatewaysync.services

import android.app.IntentService
import android.content.Intent

class SmsIntentServices(name: String) : IntentService(name) {


    val TAG = SmsIntentServices::class.java.simpleName

    override fun onHandleIntent(intent: Intent?) {

    }
}