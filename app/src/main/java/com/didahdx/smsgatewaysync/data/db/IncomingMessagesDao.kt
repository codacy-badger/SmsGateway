package com.didahdx.smsgatewaysync.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo

@Dao
interface IncomingMessagesDao {

    @Insert
    suspend fun addMessage(incomingMessages: MpesaMessageInfo)

    @Update
    suspend fun updateMessage(incomingMessages: MpesaMessageInfo)

    @Delete
    suspend fun deleteMessage(incomingMessages: MpesaMessageInfo)

    @Query("SELECT * FROM mpesaMessageInfo ORDER BY id DESC")
    fun getAllMessages(): LiveData<List<MpesaMessageInfo>>

    @Query("SELECT * FROM mpesaMessageInfo WHERE date=:time")
    fun getMessage(time: Long): LiveData<List<MpesaMessageInfo>>

    @Query("SELECT * FROM mpesaMessageInfo WHERE status=:status ORDER BY id DESC")
    suspend fun getPeddingMessages(status:Boolean):List<MpesaMessageInfo>

}