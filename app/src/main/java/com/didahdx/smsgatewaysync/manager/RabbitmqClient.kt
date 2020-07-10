package com.didahdx.smsgatewaysync.manager

import android.content.Context
import com.didahdx.smsgatewaysync.presentation.UiUpdaterInterface
import com.didahdx.smsgatewaysync.utilities.*
import com.rabbitmq.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets


class RabbitmqClient(private val uiUpdater: UiUpdaterInterface?, private val email: String) :
    ConfirmListener, RecoveryListener, ShutdownListener {

    @Volatile
    private var connection: Connection? = null
    private lateinit var channel: Channel
    var count = 0

    fun connection(context: Context) {
        try {
            connection = RabbitMqConnector.connection

            if (null != connection && connection?.isOpen!!) {

                channel = RabbitMqConnector.channel
                (connection as RecoverableConnection).addRecoveryListener(this)
                (channel as RecoverableChannel).addRecoveryListener(this)

                channel.queueDeclare(
                    email, false, false,
                    false, null
                )

                channel.queueDeclare(
                    NOTIFICATION, false, false,
                    false, null
                )

                channel.queueDeclare(
                    PUBLISH_FROM_CLIENT, false, false,
                    false, null
                )

                connection?.addShutdownListener(this)
                channel.addShutdownListener(this)
                uiUpdater?.updateStatusViewWith("$APP_NAME is running", GREEN_COLOR)

                if (channel.isOpen && channel.consumerCount(email).toInt() == 0) {
                    consumeMessages()
                    uiUpdater?.logMessage("Channel is connected")

                }else{
                    uiUpdater?.logMessage("Channel is closed because ${RabbitMqConnector.channel.closeReason}")
                }
                setServiceState(context, ServiceState.RUNNING)
                uiUpdater?.logMessage("Connected to server")
            } else {
                uiUpdater?.logMessage("Connection is closed because ${RabbitMqConnector.connection.closeReason}")
                setServiceState(context, ServiceState.STOPPED)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uiUpdater?.updateStatusViewWith(
                "Error connecting to server \n Check your network connection",
                RED_COLOR
            )
            Timber.d("$e  ${e.localizedMessage}")
            uiUpdater?.logMessage("$e \n ${e.localizedMessage}")
            val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
            if (count <= 10 && isServiceOn && ServiceState.STOPPED != getServiceState(context)) {
                uiUpdater?.logMessage("Retrying to connect in ${(1000 * count).toLong() / 60000L} Minutes")
                CoroutineScope(IO).launch {
                    delay((1000 * count).toLong())
                    connection(context)
                }
            } else {
                count = 0
                setServiceState(context, ServiceState.STOPPED)
            }
            count++
        }
    }


    private fun consumeMessages() {

        channel.basicConsume(
            email,
            true,
            { consumerTag: String?, delivery: Delivery ->
                val m = String(delivery.body, StandardCharsets.UTF_8)
                var phoneNumber =""
                var message = ""

                try {
                    val baseJsonResponse = JSONObject(m)
                    when (baseJsonResponse.getString("message_type")) {
                        "send_sms" -> {
                            phoneNumber = baseJsonResponse.getString("phone_number")
                            message = baseJsonResponse.getString("message_body")
                            uiUpdater?.sendSms(phoneNumber, message)
                        }
                        "notification_update" -> {
                            message = baseJsonResponse.getString("message_body")
    //                        uiUpdater?.notificationMessage(message)
                            uiUpdater?.updateStatusViewWith(message, GREEN_COLOR)
                        }
                    }
                } catch (e: Exception) {
                    uiUpdater?.toasterMessage(
                        "$e \n" +
                                " ${e.localizedMessage}"
                    )
                    Timber.d("$e \n ${e.localizedMessage}")
                    uiUpdater?.logMessage("$e \n ${e.localizedMessage}")
                }
                Timber.d(" $message")

            }
        )

        { consumerTag: String? -> }

        consumeNotification()
    }


    private fun consumeNotification() {

        val autoAck = false
        channel?.basicConsume(
            NOTIFICATION, autoAck,
            object : DefaultConsumer(channel) {
                @Throws(IOException::class)
                override fun handleDelivery(
                    consumerTag: String?,
                    envelope: Envelope,
                    properties: AMQP.BasicProperties?,
                    body: ByteArray?
                ) {
                    val deliveryTag = envelope.deliveryTag
                    channel.basicAck(deliveryTag, false)
//                    uiUpdater?.toasterMessage("deliveryTag $deliveryTag body ${body?.let { String(it) }} ")
//                    uiUpdater?.sendSms(
//                        "+254719134650",
//                        "deliveryTag $deliveryTag body ${body?.let { String(it) }} "
//                    )
                }
            })

    }

    fun disconnect() {
        try {
            channel.close()
            RabbitMqConnector.channel.close()
        } catch (e: Exception) {
            Timber.d("$e ${e.localizedMessage}")
        }
        try {
            connection?.close()
            RabbitMqConnector.connection.close()
            CoroutineScope(IO).cancel()
        } catch (e: Exception) {
            Timber.d("$e ${e.localizedMessage}")
        }
    }


    override fun handleAck(deliveryTag: Long, multiple: Boolean) {
//        uiUpdater?.toasterMessage("delivery Tag Sender $deliveryTag")
    }

    override fun handleNack(deliveryTag: Long, multiple: Boolean) {
//        uiUpdater?.toasterMessage("delivery Tag Failed Sender $deliveryTag")
    }

    override fun handleRecovery(recoverable: Recoverable?) {
        Timber.d(" automatic connection recovery has completed.")
        uiUpdater?.logMessage("Automatic connection recovery has completed.")
        uiUpdater?.updateStatusViewWith("$APP_NAME is running", GREEN_COLOR)
    }

    override fun handleRecoveryStarted(recoverable: Recoverable?) {
        Timber.d(" automatic connection recovery starts.")
        uiUpdater?.updateStatusViewWith("Retrying connecting to server", RED_COLOR)
        uiUpdater?.logMessage("Automatic connection recovery started")
    }

    override fun shutdownCompleted(cause: ShutdownSignalException?) {
        Timber.d(" $cause \n ${cause?.localizedMessage}")
        uiUpdater?.updateStatusViewWith(
            "Error connection lost \nCheck your internet connection \n cause is" +
                    " ${cause?.localizedMessage}", RED_COLOR
        )
        uiUpdater?.logMessage("Cause of shutdown $cause ${cause?.localizedMessage}")

    }


}