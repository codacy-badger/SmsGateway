package com.didahdx.smsgatewaysync.manager

import android.util.Log
import com.didahdx.smsgatewaysync.ui.UiUpdaterInterface
import com.didahdx.smsgatewaysync.utilities.*
import com.rabbitmq.client.*
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeoutException


class RabbitmqClient(val uiUpdater: UiUpdaterInterface?, private val email: String) {
    private val connectionFactory = ConnectionFactory()

    private val queue = LinkedBlockingDeque<String>()
    var connection: Connection? = null
    var channel: Channel? = null


    fun connection() {

        try {
            connectionFactory.host = "128.199.174.204"
            connectionFactory.username = "didahdx"
            connectionFactory.password = "test"


            if (connection == null && channel == null) {

                connection = connectionFactory.newConnection()
                channel = connection?.createChannel()

                channel?.queueDeclare(
                    email, false, false,
                    false, null)

                channel?.queueDeclare(
                    NOTIFICATION, false, false,
                    false, null)

                channel?.queueDeclare(
                    PUBLISH_FROM_CLIENT, false, false,
                    false, null
                )
                consumeMessages()
                uiUpdater?.updateStatusViewWith("$APP_NAME is running", GREEN_COLOR)
                uiUpdater?.isConnected(true)
            }

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

        val props = AMQP.BasicProperties.Builder()
            .correlationId(email)
            .replyTo(email)
            .deliveryMode(2)
            .build()


        channel?.basicPublish(
            "", PUBLISH_FROM_CLIENT, false,
            props, message.toByteArray()
        )

        Log.d("RabbitMQ", "message sent!!!")
    }


    fun  consumeMessages() {
        channel?.basicConsume(
            email,
            true,
            { consumerTag: String?, delivery: Delivery ->
                val m = String(delivery.body, StandardCharsets.UTF_8)
                println("I have received a message  $m")

                uiUpdater?.toasterMessage(m)
                val baseJsonResponse:JSONObject = JSONObject(m)

               val key= baseJsonResponse.getString("message_type")
                checkMessageType(key,baseJsonResponse)

            }
        ) { consumerTag: String? -> }

        consumeNotification()
    }

    private fun checkMessageType(key: String, baseJsonResponse: JSONObject) {
        when(key){
            "send_sms"->{
                val phoneNumber=baseJsonResponse.getString("phone_number")
                val message=baseJsonResponse.getString("message_body")
                uiUpdater?.sendSms(phoneNumber,message)
            }
        }
    }



    private fun consumeNotification() {
        channel?.basicConsume(
            NOTIFICATION,
            true,
            { consumerTag: String?, delivery: Delivery ->
                val m = String(delivery.body, StandardCharsets.UTF_8)
                println("I have received a message  $m")

                uiUpdater?.notificationMessage(m)
            }
        ) { consumerTag: String? -> }


        channel?.basicConsume(
            PUBLISH_FROM_CLIENT,
            true,
            { consumerTag: String?, delivery: Delivery ->
                val m = String(delivery.body, StandardCharsets.UTF_8)
                println("I have received a message  $m")

                uiUpdater?.notificationMessage(m)
            }
        ) { consumerTag: String? -> }

    }

     fun disconnect(){
        channel?.close()
        connection?.close()
        uiUpdater?.isConnected(false)
    }

}