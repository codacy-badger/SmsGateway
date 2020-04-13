package com.didahdx.smsgatewaysync.repository.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface IncomingMessagesDao {

 @Insert
 suspend fun addMessage(incomingMessages:IncomingMessages)

 @Update
 suspend fun updateMessage(incomingMessages: IncomingMessages)

 @Delete
 suspend fun deleteMessage(incomingMessages: IncomingMessages)

 @Query("SELECT * FROM incomingMessages ORDER BY id DESC")
 suspend fun getAllMessages():List<IncomingMessages>

 @Query("SELECT * FROM incomingMessages WHERE date=:time")
  suspend fun getMessage(time:Long):List<IncomingMessages>

 }