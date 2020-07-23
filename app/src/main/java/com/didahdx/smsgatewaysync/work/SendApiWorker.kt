package com.didahdx.smsgatewaysync.work

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.didahdx.smsgatewaysync.data.network.PostSms
import com.didahdx.smsgatewaysync.data.network.SmsApi
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeoutException

class SendApiWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    val context=appContext

    override suspend fun doWork(): Result {
        try {
            val data = inputData
            val message = data.getString(KEY_TASK_MESSAGE_API)
            if (message != null) {
//                postMessage(message, context)
            }
        } catch (e: HttpException) {
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        } catch (e: TimeoutException) {
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        }catch (e: Exception) {
           Timber.d(" $e ${e.localizedMessage}")
            return Result.failure()
        }

        return Result.success()
    }

    fun postMessage(smsInboxInfo: SmsInboxInfo,context:Context) {
        CoroutineScope(IO).launch {
            val sharedPreferences= PreferenceManager.getDefaultSharedPreferences(context)
            val url = sharedPreferences.getString(PREF_HOST_URL, " ")
            val urlEnabled = sharedPreferences.getBoolean(PREF_HOST_URL_ENABLED, false)
            try {
                val amount = smsInboxInfo.amount.replace("Ksh", "").replace(",", "")
                Timber.i(amount)
                val smsFilter = SmsFilter(smsInboxInfo.messageBody, false)
                if (smsFilter.mpesaType != NOT_AVAILABLE && urlEnabled && url != null) {

                    val post = PostSms(
                        smsInboxInfo.accountNumber,
                        amount.toDouble(),
                        smsInboxInfo.latitude.toDouble(),
                        smsInboxInfo.longitude.toDouble(),
                        smsInboxInfo.messageBody,
                        smsInboxInfo.name,
                        smsInboxInfo.receiver,
                        " ${smsFilter.date}  ${smsFilter.time}",
                        smsInboxInfo.sender,
                        Conversion.getFormattedDate(Date(smsInboxInfo.date)),
                        smsFilter.mpesaType,
                        smsInboxInfo.mpesaId
                    )

                    println(post.toString())

                    val postSms: Call<PostSms> = SmsApi.retrofitService.postSms(url, post)
                    postSms.enqueue(object : Callback<PostSms> {
                        override fun onFailure(call: Call<PostSms>, t: Throwable) {
                            Timber.i("Localised message ${t.localizedMessage}")
                        }

                        override fun onResponse(
                            call: Call<PostSms>,
                            response: Response<PostSms>
                        ) {
                            if (!response.isSuccessful) {
                                Timber.i("Code: %s", response.code())
                                return
                            }
                            Timber.i("Code: ${response.code()} call $call ")
                        }
                    })
                }
            } catch (e: Exception) {
                Timber.i("post error message ${e.localizedMessage}")
            }
        }
    }
}