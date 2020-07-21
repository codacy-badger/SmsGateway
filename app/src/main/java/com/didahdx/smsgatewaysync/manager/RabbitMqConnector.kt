package com.didahdx.smsgatewaysync.manager

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

object RabbitMqConnector {

    private val connectionFactory = ConnectionFactory()

    init {
        connectionFactory.host = "128.199.174.204"
        connectionFactory.username = "didahdx"
        connectionFactory.password = "test"
        connectionFactory.requestedHeartbeat = 30
//        connectionFactory.handshakeTimeout=60000
//        connectionFactory.shutdownTimeout=60000
//        connectionFactory.connectionTimeout = 120000
        connectionFactory.isAutomaticRecoveryEnabled = true
//        connectionFactory.requestedChannelMax=4
//        connectionFactory.workPoolTimeout
    }
    
    val connection: Connection by lazy {
        connectionFactory.newConnection()
    }


    val channel: Channel by lazy {
        connection.createChannel()
    }

    val publishChannel: Channel by lazy {
        connection.createChannel()
    }

}