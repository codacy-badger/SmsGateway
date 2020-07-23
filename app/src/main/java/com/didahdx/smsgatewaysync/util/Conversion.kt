package com.didahdx.smsgatewaysync.utilities

import java.text.SimpleDateFormat
import java.util.*

object Conversion {

    //converting bytes to KB,MB,GB,TB
    fun convertBytes(size: Long): String {
        val n: Long = 1024
        var s = ""
        val kb = size / n.toDouble()
        val mb = kb / n
        val gb = mb / n
        val tb = gb / n
        if (size < n) {
            s = "$size Bytes"
        } else if (size >= n && size < n * n) {
            s = String.format("%.2f", kb) + " KB"
        } else if (size >= n * n && size < n * n * n) {
            s = String.format("%.2f", mb) + " MB"
        } else if (size >= n * n * n && size < n * n * n * n) {
            s = String.format("%.2f", gb) + " GB"
        } else if (size >= n * n * n * n) {
            s = String.format("%.2f", tb) + " TB"
        }
        return s
    }

    fun getFormattedDate(date: Date):String{
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return  sdf.format(date).toString()
    }
}