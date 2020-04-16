package com.didahdx.smsgatewaysync.manager

import android.content.Context
import android.util.Log
import com.didahdx.smsgatewaysync.ui.UiUpdaterInterface
import com.didahdx.smsgatewaysync.utilities.*
import com.rabbitmq.client.*
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeoutException


class RabbitmqClient(private val uiUpdater: UiUpdaterInterface?, private val email: String){
    private val connectionFactory = ConnectionFactory()

    private val queue = LinkedBlockingDeque<String>()
    @Volatile private lateinit var connection: Connection
    @Volatile private var channel: Channel? = null

    fun connection(context: Context) {

        try {

                connection =RabbitmqConnector.connection
                channel = RabbitmqConnector.channel

            Log.d(
                "connectorasd",
                "connection  ${RabbitmqConnector.connection.hashCode()}   ${connection.hashCode()} " +
                        " channel ${RabbitmqConnector.channel.hashCode()}    ${channel.hashCode()} ")

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
            uiUpdater?.isConnected(true)
            uiUpdater?.updateStatusViewWith("$APP_NAME is running", GREEN_COLOR)


        } catch (e: IOException) {
            uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
            e.printStackTrace()
            Log.d("RabbitMQ", "$e  ${e.localizedMessage}")
        } catch (e: TimeoutException) {
            Log.d("RabbitMQ", "$e  ${e.localizedMessage}")
            uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
            uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
            Log.d("RabbitMQ", "$e  ${e.localizedMessage}")
        }
    }

    fun publishMessage(message: String) {

        val props = AMQP.BasicProperties.Builder()
            .correlationId(email)
            .replyTo(email)
            .deliveryMode(1)
            .build()


        channel?.basicPublish(
            "", PUBLISH_FROM_CLIENT, false,
            props, message.toByteArray()
        )

        Log.d("RabbitMQ", "message sent!!!")
    }


    fun consumeMessages() {
        channel?.basicConsume(
            email,
            true,
            { consumerTag: String?, delivery: Delivery ->
                val m = String(delivery.body, StandardCharsets.UTF_8)
                println("I have received a message  $m")
                var phoneNumber = ""
                var message = ""
//                uiUpdater?.toasterMessage(m)
                val baseJsonResponse: JSONObject = JSONObject(m)

                when (baseJsonResponse.getString("message_type")) {
                    "send_sms" -> {
                        phoneNumber = baseJsonResponse.getString("phone_number")
                        message = baseJsonResponse.getString("message_body")
                    }
                }
                uiUpdater?.toasterMessage("$phoneNumber $message")
                uiUpdater?.sendSms(phoneNumber, message)
                println("$phoneNumber  $message")
                Log.d("teretg", "$phoneNumber  $message")

            }
        )

        { consumerTag: String? -> }

        consumeNotification()
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


//        channel?.basicConsume(
//            PUBLISH_FROM_CLIENT,
//            true,
//            { consumerTag: String?, delivery: Delivery ->
//                val m = String(delivery.body, StandardCharsets.UTF_8)
//                println("I have received a message  $m")
//
//                uiUpdater?.notificationMessage(m)
//            }
//        ) { consumerTag: String? -> }

    }

    fun disconnect() {
        channel?.close()
        connection?.close()
        uiUpdater?.isConnected(false)
    }


}