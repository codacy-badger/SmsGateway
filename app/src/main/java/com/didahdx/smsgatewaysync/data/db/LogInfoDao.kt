package com.didahdx.smsgatewaysync.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.didahdx.smsgatewaysync.data.db.entities.LogInfo


@Dao
interface LogInfoDao {

    @Insert
    suspend fun addLogInfo(logInfo: LogInfo)

    @Update
    suspend fun updateLogInfo(logInfo: LogInfo)

    @Delete
    suspend fun deleteLog(logInfo: LogInfo)

    @Query("SELECT * FROM logInfo ORDER BY date DESC")
    fun getAllLogs(): LiveData<List<LogInfo>>

    @Query("SELECT * FROM logInfo WHERE isUserVisible=:isUserVisible ORDER BY date DESC")
    fun getAllLogsByUserVisibility(isUserVisible: Boolean): LiveData<List<LogInfo>>

    @Query("SELECT * FROM logInfo WHERE isUploaded=:isUploaded ORDER BY date DESC")
    fun getAllPendingLogs(isUploaded: Boolean): LiveData<List<LogInfo>>

    @Query("SELECT * FROM logInfo  ORDER BY date DESC ")
    fun getLastDate(): LiveData<List<LogInfo>>

    @Query("SELECT * FROM logInfo WHERE id = ( SELECT MAX( id ) FROM logInfo )")
    fun getHighestId(): List<LogInfo>

    @Query("SELECT * FROM logInfo WHERE id = ( SELECT MIN( id ) FROM logInfo )")
    fun getLowestId(): List<LogInfo>

    @Query("DELETE FROM logInfo WHERE id >=:lowerId and id <=:higherId")
    suspend fun delete(lowerId: Int, higherId: Int)
}