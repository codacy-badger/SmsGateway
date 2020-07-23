package com.didahdx.smsgatewaysync.data.db.entities

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
@Entity
data class MpesaMessageInfo(
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
) : Serializable,Parcelable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}