package com.didahdx.smsgatewaysync.manager

import android.util.Log
import com.didahdx.smsgatewaysync.ui.UiUpdaterInterface
import com.didahdx.smsgatewaysync.utilities.APP_NAME
import com.didahdx.smsgatewaysync.utilities.GREEN_COLOR
import com.didahdx.smsgatewaysync.utilities.RED_COLOR
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeoutException


class RabbitmqClient(val uiUpdater: UiUpdaterInterface?) {
    private val connectionFactory = ConnectionFactory()
    var connection: Connection? = null
    var channel: Channel? = null
    private val queue = LinkedBlockingDeque<String>()

     fun connection(message: String) {

        try {
            connectionFactory.host = "128.199.174.204"
            connectionFactory.username = "didahdx"
            connectionFactory.password = "test"

            if (connection == null) {

                connection = connectionFactory.newConnection()
                channel = connection?.createChannel()
                channel?.queueDeclare(
                    "android-mq", false, false,
                    false, null
                )
            }



            channel?.basicPublish(
                "", "android-mq", false,
                null, message.toByteArray()
            )

            channel?.basicConsume(
                "android-mq",
                true,
                { consumerTag: String?, delivery: Delivery ->
                    val m = String(delivery.body, StandardCharsets.UTF_8)
                    println("I have received a message  $m")

                    uiUpdater?.toasterMessage(m)
                }
            ) { consumerTag: String? -> }

            Log.d("RabbitMQ", "message sent!!!")

        } catch (e: IOException) {
            uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
            e.printStackTrace()
            Log.d("RabbitMQ", "$e  ${e.localizedMessage}")
        } catch (e: TimeoutException) {
            Log.d("RabbitMQ", "$e  ${e.localizedMessage}")
            uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
            e.printStackTrace()
            Log.d("RabbitMQ", "$e")
        } catch (e: Exception) {
            uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
            Log.d("RabbitMQ", "$e  ${e.localizedMessage}")
            Log.d("RabbitMQ", "$e")
        }
    }

     fun publishMessage(message: String) {
        try {
            queue.putLast(message)
            Log.d("RabbitMQClient", "[q]  $message")
        } catch (e: InterruptedException) {
            uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
            e.printStackTrace()
        }
    }
}