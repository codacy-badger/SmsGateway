package com.didahdx.smsgatewaysync.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SmsInfo(
    val messageBody: String, val time: String, val sender: String, val status: String
    , val longitude: String, val latitude: String
) : Parcelable {

}