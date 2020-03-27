package com.didahdx.smsgatewaysync.manager

import android.util.Log
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.io.IOException
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeoutException


class RabbitmqClient {

    val queue = LinkedBlockingDeque<String>()

    fun connection(message: String) {
        val connectionFactory = ConnectionFactory()
        var connection: Connection?=null
        var channel:Channel?=null
        try {

            connectionFactory.host = "128.199.174.204"
            connectionFactory.username="didahdx"
//            connectionFactory.port=15672
            connectionFactory.password="test"
//            connectionFactory.connectionTimeout=6000

           if(connection== null) {

               connection = connectionFactory.newConnection()
                channel = connection?.createChannel()
               channel?.queueDeclare("android-mq", false, false,
                   false, null)
           }


            channel?.basicPublish("","android-mq",false,
                null,message.toByteArray())

            Log.d("RabbitMQ","message sent!!!")

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("RabbitMQ","$e  ${e.localizedMessage}")
        } catch (e: TimeoutException) {
            Log.d("RabbitMQ","$e  ${e.localizedMessage}")
            e.printStackTrace()
            Log.d("RabbitMQ","$e")
        }catch (e:Exception){
            Log.d("RabbitMQ","$e  ${e.localizedMessage}")
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