package com.didahdx.smsgatewaysync.repository.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class IncomingMessages(
    val messageBody: String,
    @ColumnInfo(name = "date")
    val date: Long,
    val sender: String,
    val status: Boolean
):Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}