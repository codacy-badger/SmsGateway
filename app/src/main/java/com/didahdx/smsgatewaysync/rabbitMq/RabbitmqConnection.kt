package com.didahdx.smsgatewaysync.rabbitMq

import android.content.Context
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import timber.log.Timber


abstract class RabbitmqConnection : Connection {
    companion object {
        @Volatile
        private var instance: Connection? = null
        private val LOCK = Any()

        operator fun invoke() = instance ?: synchronized(LOCK) {
            instance ?: RabbitMqConnector.connection.also {
                instance = it
            }
        }

        fun close() {
            try {
                instance?.close()
                instance = null
            } catch (e: Exception) {
                Timber.d("$e ${e.localizedMessage}")
            }
        }
    }
}

abstract class RabbitmqChannel : Channel {
    companion object {
        @Volatile
        private var instance: Channel? = null
        private val LOCK = Any()

        operator fun invoke() = instance ?: synchronized(LOCK) {
            instance ?: RabbitMqConnector.channel.also {
                instance = it
            }
        }

        fun close() {
            try {
                instance?.close()
                instance = null
            } catch (e: Exception) {
                Timber.d("$e ${e.localizedMessage}")
            }
        }
    }
}
