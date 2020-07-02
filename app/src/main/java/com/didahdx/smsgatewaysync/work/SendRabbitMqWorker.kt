package com.didahdx.smsgatewaysync.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.didahdx.smsgatewaysync.manager.RabbitMqConnector
import com.didahdx.smsgatewaysync.utilities.AppLog.logMessage
import com.didahdx.smsgatewaysync.utilities.KEY_EMAIL
import com.didahdx.smsgatewaysync.utilities.KEY_TASK_MESSAGE
import com.didahdx.smsgatewaysync.utilities.PUBLISH_FROM_CLIENT
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AlreadyClosedException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeoutException


class SendRabbitMqWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    val app = appContext

    companion object {
        const val WORK_NAME = "com.didahdx.smsgatewaysync.work.SendRabbitMqWorker"
    }

    override suspend fun doWork(): Result {
        try {
            val data = inputData
            val message = data.getString(KEY_TASK_MESSAGE)
            val email = data.getString(KEY_EMAIL)
            if (message != null && email != null) {
                publishMessage(message, email)
            }
        } catch (e: HttpException) {
            delay(30000) 
            Timber.d(" $e ${e.localizedMessage}")
            logMessage(" $e ${e.localizedMessage}",app)
            return Result.retry()
        } catch (e: TimeoutException) {
            delay(30000)
            logMessage(" $e ${e.localizedMessage}",app)
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        } catch (e: AlreadyClosedException) {
                delay(30000)
            logMessage(" $e ${e.localizedMessage}",app)
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        } catch (e: Exception) {
            logMessage(" $e ${e.localizedMessage}",app)
            Timber.d(" $e ${e.localizedMessage}")
            return Result.failure()
        }

        return Result.success()
    }

    private fun publishMessage(message: String, email: String) {
        val channel = RabbitMqConnector.channel
        val props = AMQP.BasicProperties.Builder()
            .correlationId(email)
            .replyTo(email)
            .deliveryMode(1)
            .build()

        channel.basicPublish(
            "", PUBLISH_FROM_CLIENT, false,
            props, message.toByteArray()
        )

    }

}