package com.didahdx.smsgatewaysync.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.didahdx.smsgatewaysync.data.db.entities.IncomingMessages
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.utilities.MESSAGE_DATABASE

@Database(
    entities = [MpesaMessageInfo::class],exportSchema = false,
    version = 1
)
abstract class MessagesDatabase:RoomDatabase() {

    abstract fun getIncomingMessageDao(): IncomingMessagesDao

    companion object{
        @Volatile private var instance: MessagesDatabase?=null
        private val LOCK=Any()

        operator fun invoke(context: Context)= instance
            ?: synchronized(LOCK){
            instance
                ?: buildDatabase(
                    context
                ).also {
                instance =it
            }
        }

        private fun buildDatabase(context: Context)= Room.databaseBuilder(
            context.applicationContext,
            MessagesDatabase::class.java, MESSAGE_DATABASE
        ).build()
    }

}