package com.didahdx.smsgatewaysync.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class LogInfo(
    val date: String,
    val type: String,
    val log: String,
    val client_gateway_type: String,
    val client_sender: String,
    val isUserVisible: Boolean,
    val isUploaded:Boolean
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    override fun toString(): String {
        return "\n$date\n$log\n"
    }
}