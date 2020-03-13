package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.microsoft.appcenter.utils.HandlerUtils.runOnUiThread
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.MqttClient.generateClientId


class MqttClient {

    var client: MqttAndroidClient? = null
    var clientId: String = generateClientId()
    var options = MqttConnectOptions()
    val TAG=MqttClient::class.java.simpleName

    fun connect(context: Context,clientId:String) {
        context.toast("Connecting to Server")

//        val clientId: String = generateClientId()
        val client = MqttAndroidClient(context, serverURI, clientId)

        val options = MqttConnectOptions()
        options.userName = "admin"
        options.password = "admin".toCharArray()

        val token = client.connect(options)

        try {

            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // We are connected
                   context.toast( "onSuccess")
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d( TAG,"onFailure \n asyncActionToken: $asyncActionToken \n exeception : $exception")
                }
            }
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