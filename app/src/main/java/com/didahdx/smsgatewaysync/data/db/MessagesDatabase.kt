package com.didahdx.smsgatewaysync.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.didahdx.smsgatewaysync.data.db.entities.LogInfo
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo

@Database(
    entities = [MpesaMessageInfo::class,LogInfo::class], exportSchema = false,
    version = 1
)
abstract class MessagesDatabase : RoomDatabase() {

    abstract fun getIncomingMessageDao(): IncomingMessagesDao
    abstract fun getLogInfoDao():LogInfoDao

    companion object {
        @Volatile
        private var instance: MessagesDatabase? = null
        private val LOCK = Any()
        private const val MESSAGE_DATABASE = "messageDatabase"
        operator fun invoke(context: Context) = instance
            ?: synchronized(LOCK) {
                instance
                    ?: buildDatabase(
                        context
                    ).also {
                        instance = it
                    }
            }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            MessagesDatabase::class.java, MESSAGE_DATABASE)
//            .addMigrations(MIGRATION_1_2)
            .build()


        private val MIGRATION_1_2 = object : Migration(1, 2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `LogInfo` RENAME COLUMN date TO dateString ")
                database.execSQL("ALTER TABLE `LogInfo` ADD COLUMN date INTEGER NOT NULL ")
            }
        }
    }





}