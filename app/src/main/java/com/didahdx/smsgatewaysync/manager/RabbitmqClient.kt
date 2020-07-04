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
    private  var connection: Connection?=null
    @Volatile
    private var channel: Channel? = null
    var count = 0

    fun connection(context: Context) {
        try {
            connection = RabbitMqConnector.connection
            channel = RabbitMqConnector.channel

            (connection as RecoverableConnection).addRecoveryListener(this)
            (channel as RecoverableChannel).addRecoveryListener(this)

            channel?.queueDeclare(
                email, false, false,
                false, null
            )

            channel?.queueDeclare(
                NOTIFICATION, false, false,
                false, null
            )

            channel?.queueDeclare(
                PUBLISH_FROM_CLIENT, false, false,
                false, null
            )

            connection?.addShutdownListener(this)
            channel?.addShutdownListener(this)

            channel?.confirmSelect()
            channel?.addConfirmListener(
                { deliveryTag, multiple ->
                    uiUpdater?.toasterMessage(" Delivery ack $deliveryTag   multiple   $multiple")
                },
                { deliveryTag, multiple ->
                    uiUpdater?.toasterMessage(" Delivery Not ack $deliveryTag   multiple $multiple")
                })

            channel?.basicRecover()
            consumeMessages()
            setServiceState(context, ServiceState.RUNNING)
            uiUpdater?.updateStatusViewWith("$APP_NAME is running", GREEN_COLOR)
        } catch (e: Exception) {
            e.printStackTrace()
            uiUpdater?.updateStatusViewWith(
                "Error connecting to server ${e.localizedMessage}",
                RED_COLOR
            )
            Timber.d("$e  ${e.localizedMessage}")
            uiUpdater?.logMessage("$e \n ${e.localizedMessage}")
            disconnect()
            uiUpdater?.updateStatusViewWith(
                "Retrying to connect to server \nNumber of retries $count",
                RED_COLOR
            )
            channel=null
            connection=null
            val isServiceOn = SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY)
            if (count <= 10 && isServiceOn && ServiceState.STOPPED!= getServiceState(context)) {
                CoroutineScope(IO).launch {
                    delay(30000)
                    connection(context)
                }
            } else {
                uiUpdater?.updateStatusViewWith(
                    "Number of retries have reached" +
                            " $count \n check the your network ", RED_COLOR
                )
                setServiceState(context, ServiceState.STOPPED)
            }
            count++
        }
    }


    private fun consumeMessages() {
        channel?.basicConsume(
            email,
            true,
            { consumerTag: String?, delivery: Delivery ->
                val m = String(delivery.body, StandardCharsets.UTF_8)
                var phoneNumber = ""
                var message = ""

                try {
                    val baseJsonResponse: JSONObject = JSONObject(m)
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
            channel?.close()
        } catch (e: Exception) {
            Timber.d("$e ${e.localizedMessage}")
        }
        try {
            connection?.close()
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
            "Error connection lost \nCheck your internet connection ",
            RED_COLOR
        )
        uiUpdater?.logMessage(" $cause ${cause?.localizedMessage}")

    }


}