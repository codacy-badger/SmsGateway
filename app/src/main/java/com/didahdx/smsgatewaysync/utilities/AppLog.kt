package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.content.Context.MODE_APPEND
import android.widget.Toast
import java.io.*
import java.util.*

/**
 * used to log events
 * */

class AppLog {

    //writing to log messages
    fun writeToLog(context: Context, log: String) {

        var fileOutputStream: FileOutputStream? = null

        try {
            fileOutputStream = context.openFileOutput(LOG_FILE_NAME, MODE_APPEND)
            fileOutputStream.write(log.toByteArray())
        } catch (e: OutOfMemoryError) {
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
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
  suspend fun readLog(context: Context): String {
        var text: String = ""
        val stringBuilder = StringBuilder()
        var fileInputStream: FileInputStream? = null
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
//            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
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
}


