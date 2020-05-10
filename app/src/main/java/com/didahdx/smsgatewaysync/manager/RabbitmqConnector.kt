package com.didahdx.smsgatewaysync.manager

import com.rabbitmq.client.Address
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

object RabbitmqConnector {

    private val connectionFactory = ConnectionFactory()

    init {
        connectionFactory.host = "128.199.174.204"
        connectionFactory.username = "didahdx"
        connectionFactory.password = "test"
//        connectionFactory.networkRecoveryInterval=10000
//        connectionFactory.connectionTimeout=10000
        connectionFactory.isAutomaticRecoveryEnabled = true
//        connectionFactory.isTopologyRecoveryEnabled = false
    }

    val connection: Connection by lazy {
        connectionFactory.newConnection()
    }


    val channel: Channel by lazy {
        connection.createChannel()
    }
}