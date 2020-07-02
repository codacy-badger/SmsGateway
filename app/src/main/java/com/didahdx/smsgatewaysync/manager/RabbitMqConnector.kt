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
//        connectionFactory.networkRecoveryInterval = 10000
        connectionFactory.requestedHeartbeat = 20
//        connectionFactory.set
//        connectionFactory.connectionTimeout = 240000
        connectionFactory.isAutomaticRecoveryEnabled = true
    }
    
    val connection: Connection by lazy {
        connectionFactory.newConnection()
    }


    val channel: Channel by lazy {
        connection.createChannel()
    }


}