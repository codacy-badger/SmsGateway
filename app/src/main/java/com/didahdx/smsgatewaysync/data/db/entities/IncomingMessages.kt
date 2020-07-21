package com.didahdx.smsgatewaysync.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class IncomingMessages(
    val messageBody: String,
    val date: Long,
    val sender: String,
    val status: Boolean,
    val longitude: String,
    val latitude: String
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}