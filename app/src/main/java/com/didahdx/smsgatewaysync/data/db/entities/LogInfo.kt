package com.didahdx.smsgatewaysync.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class LogInfo(
    val date: Long,
    val dateString: String,
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
        return "\n$dateString\n$log\n"
//        return "<br>$dateString<br>$log<br>"
    }
}