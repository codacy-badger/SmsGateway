package com.didahdx.smsgatewaysync.manager

import android.content.Context
import com.rabbitmq.client.Connection

abstract class RabbitmqConnection : Connection {
    companion object {
        @Volatile
        private var instance: Connection? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: RabbitmqConnector.connection.also {
                instance = it
            }
        }
    }
}