package com.didahdx.smsgatewaysync.work

import android.accounts.NetworkErrorException
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.didahdx.smsgatewaysync.manager.RabbitMqConnector
import com.didahdx.smsgatewaysync.utilities.KEY_TASK_MESSAGE
import com.didahdx.smsgatewaysync.utilities.PUBLISH_FROM_CLIENT
import com.didahdx.smsgatewaysync.utilities.toast
import com.rabbitmq.client.AMQP
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
            if (message != null) {
                publishMessage(message, "")
            }
        } catch (e: HttpException) {
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        } catch (e: TimeoutException) {
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        }catch (e: Exception) {
            app.toast("Worker $e ${e.localizedMessage}")
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


        channel?.basicPublish(
            "", PUBLISH_FROM_CLIENT, false,
            props, message.toByteArray()
        )
        Timber.d("message sent!!!")
    }

}