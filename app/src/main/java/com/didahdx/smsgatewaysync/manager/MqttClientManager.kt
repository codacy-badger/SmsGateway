package com.didahdx.smsgatewaysync.manager

//import android.content.Context
//import android.util.Log
//import com.didahdx.smsgatewaysync.model.MqttConnectionParam
//import com.didahdx.smsgatewaysync.ui.UiUpdaterInterface
//import com.didahdx.smsgatewaysync.utilities.APP_NAME
//import com.didahdx.smsgatewaysync.utilities.GREEN_COLOR
//import com.didahdx.smsgatewaysync.utilities.RED_COLOR
//import org.eclipse.paho.android.service.MqttAndroidClient
//import org.eclipse.paho.client.mqttv3.*
//
//class MqttClientManager(
//    private val connectionParams: MqttConnectionParam,
//    val context: Context,
//    val uiUpdater: UiUpdaterInterface?
//) {
//
//    private val client =
//        MqttAndroidClient(context, connectionParams.host, connectionParams.clientId)
//
//
//    init {
//        client.setCallback(object : MqttCallbackExtended {
//            override fun connectComplete(b: Boolean, s: String) {
//                Log.w("mqtt", "connected $s")
//                uiUpdater?.updateStatusViewWith("$APP_NAME is running", GREEN_COLOR)
//            }
//
//            override fun connectionLost(throwable: Throwable?) {
////                uiUpdater?.publish(false)
//                Log.w("mqtt", "Lost connection")
//                uiUpdater?.updateStatusViewWith("Lost Connection to server", RED_COLOR)
//            }
//
//            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
//                Log.w("Mqtt", mqttMessage.toString())
//                uiUpdater?.toasterMessage(mqttMessage.toString())
//            }
//
//            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
////                uiUpdater?.toasterMessage("M")
//            }
//        })
//
//    }
//
//
//    fun connect() {
//
//        if (client.isConnected) {
//            return
//        }
//
//        val mqttConnectOptions = MqttConnectOptions()
//        mqttConnectOptions.isAutomaticReconnect = true
//        mqttConnectOptions.isCleanSession = false
//        //mqttConnectOptions.setUserName(this.connectionParams.username)
//        //mqttConnectOptions.setPassword(this.connectionParams.password.toCharArray())
//        try {
//            val params = this.connectionParams
//            client.connect(mqttConnectOptions, null, object : IMqttActionListener {
//                override fun onSuccess(asyncActionToken: IMqttToken) {
//                    uiUpdater?.updateStatusViewWith("$APP_NAME is running", GREEN_COLOR)
//                    uiUpdater?.publish(true)
//                    val disconnectedBufferOptions = DisconnectedBufferOptions()
//                    disconnectedBufferOptions.isBufferEnabled = true
//                    disconnectedBufferOptions.bufferSize = 5000
//                    disconnectedBufferOptions.isPersistBuffer = true
//                    disconnectedBufferOptions.isDeleteOldestMessages = false
//                    client.setBufferOpts(disconnectedBufferOptions)
//                    subscribe(params.topic)
//
//                }
//
//                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
//                    uiUpdater?.updateStatusViewWith("Error connecting to server", RED_COLOR)
////                    uiUpdater?.publish(false)
//                    Log.w("Mqtt", "Failed to connect to: " + params.host + exception.toString())
//                }
//            })
//        } catch (ex: MqttException) {
//            ex.printStackTrace()
//        }
//    }
//
//    fun disconnect() {
//        try {
//            client.disconnect(null, object : IMqttActionListener {
//                /**
//                 * This method is invoked when an action has completed successfully.
//                 * @param asyncActionToken associated with the action that has completed
//                 */
//                override fun onSuccess(asyncActionToken: IMqttToken?) {
////                    uiUpdater?.publish(false)
//                }
//
//                /**
//                 * This method is invoked when an action fails.
//                 * If a client is disconnected while an action is in progress
//                 * onFailure will be called. For connections
//                 * that use cleanSession set to false, any QoS 1 and 2 messages that
//                 * are in the process of being delivered will be delivered to the requested
//                 * quality of service next time the client connects.
//                 * @param asyncActionToken associated with the action that has failed
//                 */
//                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//                    uiUpdater?.toasterMessage("Failed to disconnect ${exception.toString()}")
//                }
//
//            })
//        } catch (ex: MqttException) {
//            System.err.println("Exception disconnect")
//            ex.printStackTrace()
//        }
//    }
//
//
//    // Subscribe to topic
//    fun subscribe(topic: String) {
//        try {
//            client.subscribe(topic, 0, null, object : IMqttActionListener {
//                override fun onSuccess(asyncActionToken: IMqttToken?) {
//                    Log.w("Mqtt", "Subscription!")
//                }
//
//                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//                    Log.w("Mqtt", "Subscription fail!")
//                }
//            })
//        } catch (ex: MqttException) {
//            System.err.println("Exception subscribing")
//            ex.printStackTrace()
//        }
//    }
//
//    // Unsubscribe the topic
//    fun unsubscribe(topic: String) {
//
//        try {
//            client.unsubscribe(topic, null, object : IMqttActionListener {
//                override fun onSuccess(asyncActionToken: IMqttToken?) {
//                    uiUpdater?.updateStatusViewWith("UnSubscribed to Topic", GREEN_COLOR)
//                }
//
//                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//                    uiUpdater?.updateStatusViewWith("Failed to UnSubscribe to Topic", RED_COLOR)
//                }
//
//            })
//        } catch (ex: MqttException) {
//            System.err.println("Exception unsubscribe")
//            ex.printStackTrace()
//        }
//
//    }
//
//    fun publish(message: String) {
//        try {
//            if (!client.isConnected) {
//                connect()
//            } else {
//                client.publish(
//                    this.connectionParams.topic,
//                    message.toByteArray(),
//                    0,
//                    false,
//                    null,
//                    object : IMqttActionListener {
//                        override fun onSuccess(asyncActionToken: IMqttToken?) {
//                            Log.w("Mqtt", "Publish Success!")
//                        }
//
//                        override fun onFailure(
//                            asyncActionToken: IMqttToken?,
//                            exception: Throwable?
//                        ) {
//                            Log.w("Mqtt", "Publish Failed!")
//                        }
//
//                    })
//            }
//        } catch (ex: MqttException) {
//            System.err.println("Exception publishing  ")
//            ex.printStackTrace()
//        }
//    }
//}