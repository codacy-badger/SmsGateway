package com.didahdx.smsgatewaysync.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

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
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}