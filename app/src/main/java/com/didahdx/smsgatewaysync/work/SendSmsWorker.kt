package com.didahdx.smsgatewaysync.work

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.didahdx.smsgatewaysync.domain.MessageInfo
import com.didahdx.smsgatewaysync.util.*
import com.didahdx.smsgatewaysync.util.AppLog.logMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeoutException

class SendSmsWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    val context = appContext
    override suspend fun doWork(): Result {
        try {
            val data = inputData
            val message = data.getString(KEY_TASK_MESSAGE)
            val phoneNumber = data.getString(KEY_PHONE_NUMBER)
            if (message != null && phoneNumber != null) {
                sendSms(phoneNumber, message)
            }
        } catch (e: HttpException) {
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        } catch (e: TimeoutException) {
            Timber.d(" $e ${e.localizedMessage}")
            return Result.retry()
        } catch (e: Exception) {
            Timber.d(" $e ${e.localizedMessage}")
            return Result.failure()
        }

        return Result.success()
    }

    fun sendSms(phoneNumber: String, message: String) {
        val outgoingMessages: Queue<MessageInfo> = LinkedList()
        var messageCount = 0
        CoroutineScope(Main).launch {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            lateinit var smsManager: SmsManager
            val defaultSim = sharedPreferences.getString(PREF_SIM_CARD, "")
            val localSubscriptionManager =context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            if (checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                )
                == PackageManager.PERMISSION_GRANTED
            ) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (localSubscriptionManager.activeSubscriptionInfoCount > 1) {
                            val localList: List<*> =
                                localSubscriptionManager.activeSubscriptionInfoList
                            val simInfo1 = localList[0] as SubscriptionInfo
                            val simInfo2 = localList[1] as SubscriptionInfo

                            smsManager = when (defaultSim) {
                                SIM_CARD_1 -> {
                                    //SendSMS From SIM One
                                    SmsManager.getSmsManagerForSubscriptionId(simInfo1.subscriptionId)
                                }
                                SIM_CARD_2 -> {
                                    //SendSMS From SIM Two
                                    SmsManager.getSmsManagerForSubscriptionId(simInfo2.subscriptionId)
                                }
                                else -> {
                                    SmsManager.getDefault()
                                }
                            }
                        } else {
                            smsManager = SmsManager.getDefault()
                        }
                    } else {
                        smsManager = SmsManager.getDefault()
                    }
                } else {
                    smsManager = SmsManager.getDefault()
                }


                val sentPI = PendingIntent.getBroadcast(
                    context, 0, Intent(SMS_SENT_INTENT), 0
                )

                val deliveredPI = PendingIntent.getBroadcast(
                    context, 0, Intent(SMS_DELIVERED_INTENT), 0
                )


                //when the SMS has been sent
                context.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(arg0: Context?, arg1: Intent?) {
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                context.toast("SMS sent to $phoneNumber")
                                logMessage("SMS sent to $phoneNumber",context)
                            }
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                                context.toast("Generic failure")
                                logMessage("Generic failure sending sms to $phoneNumber",context)
                            }
                            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                                context.toast("No service")
                                logMessage("failed sending sms to $phoneNumber because service is currently unavailable ",context)
                            }
                            SmsManager.RESULT_ERROR_NULL_PDU -> {
                                context.toast("Null PDU")
                                logMessage("Failed sending sms to $phoneNumber because no pdu provided",context)
                            }
                            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                context.toast("Radio off")
                                logMessage("Failed sending sms to $phoneNumber because radio was explicitly turned off",context)
                            }
                        }
                    }
                }, IntentFilter(SMS_SENT_INTENT))

                //when the SMS has been delivered
                context.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(arg0: Context?, arg1: Intent?) {
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                context.toast("SMS delivered")
                                logMessage("Sms delivered to $phoneNumber",context)
                            }
                            Activity.RESULT_CANCELED -> {
                                context.toast("SMS not delivered")
                                logMessage("SMS not delivered to $phoneNumber",context)
                            }
                        }
                    }
                }, IntentFilter(SMS_DELIVERED_INTENT))


                val arraySendInt = ArrayList<PendingIntent>()
                arraySendInt.add(sentPI)
                val arrayDelivery = ArrayList<PendingIntent>()
                arrayDelivery.add(deliveredPI)

                outgoingMessages.add(MessageInfo(phoneNumber, message))

                for (`object` in outgoingMessages) {
                    val element = `object` as MessageInfo
                    val parts = smsManager.divideMessage(element.messageBody)

                    smsManager.sendMultipartTextMessage(
                        element.phoneNumber,
                        null,
                        parts,
                        arraySendInt,
                        arrayDelivery
                    )
                    messageCount++
                    outgoingMessages.remove()

                }
            }
        }
    }


}