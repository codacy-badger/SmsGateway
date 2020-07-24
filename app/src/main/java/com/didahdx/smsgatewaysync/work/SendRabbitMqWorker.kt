package com.didahdx.smsgatewaysync.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.didahdx.smsgatewaysync.rabbitMq.RabbitMqConnector
import com.didahdx.smsgatewaysync.util.*
import com.didahdx.smsgatewaysync.util.AppLog.logMessage
import com.google.firebase.perf.metrics.AddTrace
import com.rabbitmq.client.AMQP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeoutException


class SendRabbitMqWorker(appContext: Context, params: WorkerParameters) :
    Worker(appContext, params) {
    val app = appContext

    companion object {
        const val WORK_NAME = "com.didahdx.smsgatewaysync.work.SendRabbitMqWorker"
        const val PING_WORK_NAME = "com.didahdx.smsgatewaysync.work.SendRabbitMqWorker.PINGING"
    }

    @AddTrace(name = "SendRabbitMqWorkerDoWork", enabled = true /* optional */)
    override  fun doWork(): Result {
        try {
            val data = inputData
            val message = data.getString(KEY_TASK_MESSAGE)
            val email = data.getString(KEY_EMAIL)
            val isServiceOn = SpUtil.getPreferenceBoolean(app, PREF_SERVICES_KEY)
            if (message != null && email != null) {
                if (RabbitMqConnector.connection.isOpen && isServiceOn) {
                    publishMessage(message, email)
                } else {
//                    CoroutineScope(Main).launch {
//                        app.toast("$WORK_NAME \n ${RabbitMqConnector.connection.isOpen} CONNECTION IS NOT OPEN ")
//                    }

                    return Result.retry()
                }
            }
        } catch (e: HttpException) {
            Timber.d("Worker $e ${e.localizedMessage}")
            logMessage("Worker $e ${e.localizedMessage}", app)
            return Result.retry()
        } catch (e: TimeoutException) {
            logMessage("Worker $e ${e.localizedMessage}", app)
            Timber.d("Worker $e ${e.localizedMessage}")
            return Result.retry()
        }  catch (e: Exception) {
            logMessage("Worker $e ${e.localizedMessage}", app)
            Timber.d("Worker $e ${e.localizedMessage}")

            CoroutineScope(Main).launch {
                app.toast("$WORK_NAME $e ${e.localizedMessage}")
            }
            return Result.failure()
        }

        return Result.success()
    }

    private fun publishMessage(message: String, email: String) {
        Timber.d("Thread name ${Thread.currentThread().name}")
        val channel = RabbitMqConnector.channel
        val props = AMQP.BasicProperties.Builder()
            .correlationId(email)
            .replyTo(email)
            .deliveryMode(1)
            .build()
        channel.confirmSelect()
        channel.waitForConfirms()
        channel.addConfirmListener(
            { deliveryTag, multiple ->
                    Timber.d(" Delivery ack $deliveryTag   multiple   $multiple")
            },
            { deliveryTag, multiple ->
                    Timber.d(" Delivery Not ack $deliveryTag   multiple $multiple")
            })

        channel.basicPublish(
            "", PUBLISH_FROM_CLIENT, false,
            props, message.toByteArray()
        )

    }

}