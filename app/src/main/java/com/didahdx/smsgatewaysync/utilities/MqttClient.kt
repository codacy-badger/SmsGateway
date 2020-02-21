package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.widget.Toast
import com.microsoft.appcenter.utils.HandlerUtils.runOnUiThread
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MqttClient {

    var client: MqttAndroidClient? = null

    fun connect(context: Context) {
        Toast.makeText(context, "Connecting to Server", Toast.LENGTH_LONG).show()
        val connectOptions = MqttConnectOptions()
        connectOptions.isAutomaticReconnect = true
        client = MqttAndroidClient(context, serverURI, clientId)
        try {
            client?.connect(connectOptions, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    subscribe(context)

                    Toast.makeText(context, "Established a connection to Server", Toast.LENGTH_LONG)
                        .show()

                }

                override fun onFailure(asyncActionToken: IMqttToken, e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error connecting $e", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(context: Context) {
        try {
            client?.subscribe(subscribeTopic, 0,
                IMqttMessageListener { topic, message ->
                    runOnUiThread(Runnable {
                        Toast.makeText(
                            context,
                            message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    fun publishMessage(context: Context, message: String) {
        val publishTopic = "outbox"
        val msg = MqttMessage()
        client = MqttAndroidClient(context, serverURI, clientId)
        val connectOptions = MqttConnectOptions()

        try {
            client?.connect(connectOptions, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    msg.payload = message.toByteArray()
                    client?.publish(publishTopic, msg)
                    Toast.makeText(context, "Established a connection to Server", Toast.LENGTH_LONG)
                        .show()

                }

                override fun onFailure(asyncActionToken: IMqttToken, e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error connecting $e", Toast.LENGTH_LONG).show()

                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            Toast.makeText(context, "Error connecting $e", Toast.LENGTH_LONG).show()
        }

    }
}