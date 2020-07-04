package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.content.Context.MODE_APPEND
import androidx.preference.PreferenceManager
import androidx.work.*
import com.didahdx.smsgatewaysync.domain.LogFormat
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import timber.log.Timber
import java.io.*
import java.util.*

/**
 * used to log events
 * */

object AppLog {

    //writing to log messages
    private fun writeToLog(context: Context, log: String, send: Boolean) {
        val email = PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(PREF_USER_EMAIL, "NA")

        val data = Data.Builder()
            .putString(KEY_TASK_MESSAGE, log)
            .putString(KEY_EMAIL, email)
            .build()

        if (send) {
            sendToRabbitMQ(context, data)
        }

        val logs: LogFormat = Gson().fromJson(log, LogFormat::class.java)
        val value="\n\n${logs.date} \n ${logs.log}"

        var fileOutputStream: FileOutputStream? = null

        try {
            fileOutputStream = context.openFileOutput(LOG_FILE_NAME, MODE_APPEND)
            fileOutputStream.write(value.toByteArray())

            if (send) {
                delete(context)
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (io: IOException) {
            io.printStackTrace()
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    //reading log messages
    fun readLog(context: Context): String {
        var text = ""
        val stringBuilder = StringBuilder()
        var fileInputStream: FileInputStream? = null
        var fileOutputStream: FileOutputStream? = null
        try {
            fileInputStream = context.openFileInput(LOG_FILE_NAME)
            val isr = InputStreamReader(fileInputStream)
            val br = BufferedReader(isr)


            br.forEachLine {
                stringBuilder.append(it).append("\n")
            }

            text = stringBuilder.toString()

        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            try {
                fileOutputStream = context.openFileOutput(LOG_FILE_NAME, MODE_APPEND)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    fileOutputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fileInputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return text
    }


    private fun delete(context: Context) {
        val arrayLogs = ArrayList<String>()
        var fileInputStream: FileInputStream? = null

        try {
            fileInputStream = context.openFileInput(LOG_FILE_NAME)
            val isr = InputStreamReader(fileInputStream)
            val br = BufferedReader(isr)

            br.forEachLine {
                arrayLogs.add(it)
            }

            if (arrayLogs.size > 62) {
                val stringBuilder = StringBuilder()
                val count = arrayLogs.size - 10
                for (i in arrayLogs.indices) {
                    if (i >= count) {
                        stringBuilder.append(arrayLogs[i]).append("\n\n")
                    }
                }

                val dir: File = context.filesDir
                val file = File(dir, LOG_FILE_NAME)
                val deleted = file.delete()
                if (deleted) {
                    Timber.d("Deleted the file: %s", file.name)
                } else {
                    Timber.d("Failed to delete the file.")
                }
                writeToLog(context, stringBuilder.toString(), false)
            }
        } catch (e: Exception) {
            Timber.d(" $e  ${e.localizedMessage}")
            e.printStackTrace()
        } finally {
            try {
                fileInputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }


    private fun sendToRabbitMQ(context: Context, data: Data) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendRabbitMqWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    fun logMessage(message: String,context: Context) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
        val logFormat = LogFormat(
            date = Date().toString(),
            type = "logs",
            client_gateway_type = ANDROID_PHONE,
            log = message,
            client_sender = email
        )
        writeToLog(context, Gson().toJson(logFormat), true)
    }
}


