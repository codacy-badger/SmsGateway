package com.didahdx.smsgatewaysync.utilities

import android.util.Log
import com.rabbitmq.client.ConnectionFactory
import java.io.IOException
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeoutException

class RabbitmqClient {

    val queue = LinkedBlockingDeque<String>()

    fun connection(message: String) {
        val connectionFactory = ConnectionFactory()

        try {

            connectionFactory.host = "192.168.43.182"
            connectionFactory.username="guest"
            connectionFactory.port=15672
            connectionFactory.password="guest"
            val connection = connectionFactory.newConnection()

            val channel = connection.createChannel()
            channel.queueDeclare("android-mq", false, false,
                false, null)
            channel.basicPublish("","android-mq",false,
                null,message.toByteArray())

            Log.d("RabbitMQ","message sent!!!")

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("RabbitMQ","$e")
        } catch (e: TimeoutException) {
            e.printStackTrace()
            Log.d("RabbitMQ","$e")
        }catch (e:Exception){
            Log.d("RabbitMQ","$e")
        }
    }

    fun publishMessage(message: String) {
        connection(message)
        try {
            queue.putLast(message)
            Log.d("RabbitMQClient", "[q]  $message")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}