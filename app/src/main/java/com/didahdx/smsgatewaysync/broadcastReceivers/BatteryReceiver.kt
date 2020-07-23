package com.didahdx.smsgatewaysync.broadcastReceivers

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.work.Data
import com.didahdx.smsgatewaysync.domain.PhoneStatus
import com.didahdx.smsgatewaysync.util.*
import com.didahdx.smsgatewaysync.work.WorkerUtil.sendToRabbitMQ
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import java.text.DecimalFormat
import java.util.*


/**
 * used to check battery status
 * */
class BatteryReceiver : BroadcastReceiver() {
    val newIntent = Intent(BATTERY_LOCAL_BROADCAST_RECEIVER)
    val user = FirebaseAuth.getInstance().currentUser?.email ?: NOT_AVAILABLE
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val isServiceOn =
                context.let { SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY) }
            if (isServiceOn && BATTERY_LOCAL_BROADCAST_RECEIVER == intent.action) {
                if (intent.extras != null) {
                    val batteryVoltage =
                        intent.extras!!.getString(BATTERY_VOLTAGE_EXTRA) ?: NOT_AVAILABLE
                    val batteryPercentage =
                        intent.extras!!.getString(BATTERY_PERCENTAGE_EXTRA).toString()
                            ?: NOT_AVAILABLE
                    val batteryCondition =
                        intent.extras!!.getString(BATTERY_CONDITION_EXTRA) ?: NOT_AVAILABLE
                    val batteryTemperature =
                        intent.extras!!.getString(BATTERY_TEMPERATURE_EXTRA) ?: NOT_AVAILABLE
                    val batteryPowerSource =
                        intent.extras!!.getString(BATTERY_POWER_SOURCE_EXTRA) ?: NOT_AVAILABLE
                    val batteryChargingStatus =
                        intent.extras!!.getString(BATTERY_CHARGING_STATUS_EXTRA)
                            ?: NOT_AVAILABLE
                    val batteryTechnology =
                        intent.extras!!.getString(BATTERY_TECHNOLOGY_EXTRA) ?: NOT_AVAILABLE

                    var imei  = ""
                    var networkName = ""
                    var simSerialNumber = ""
                    var imsi = ""
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PermissionChecker.PERMISSION_GRANTED
                    ) {

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            imei = telephonyManager.deviceId ?: NOT_AVAILABLE
                            simSerialNumber = telephonyManager.simSerialNumber ?: NOT_AVAILABLE
                            imsi = telephonyManager.subscriberId ?: NOT_AVAILABLE
                        }
                        networkName = telephonyManager.networkOperatorName ?: NOT_AVAILABLE
                    }

                    //internal storage
                    val df2 = DecimalFormat("###,###,###,###.00")
                    val stat = StatFs(Environment.getExternalStorageDirectory().path)
                    val bytesAvailable: Long = stat.blockSizeLong * stat.availableBlocksLong
                    val megAvailable: Double =
                        (bytesAvailable.toDouble() / (GIGABYTE).toDouble())
                    val megTotal: Double =
                        (stat.blockSizeLong * stat.blockCountLong).toDouble() / (GIGABYTE).toDouble()

                    //ram space
                    val mi = ActivityManager.MemoryInfo()
                    val activityManager =
                        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                    activityManager!!.getMemoryInfo(mi)
                    val availableRAM: Double = mi.availMem / (0x100000L).toDouble()
                    val TotalRam: Double = mi.totalMem / (0x100000L).toDouble()

                    val phoneStatus = PhoneStatus(
                        type = "phoneStatus",
                        batteryPercentage = batteryPercentage,
                        batteryCondition = batteryCondition,
                        batteryTemperature = batteryTemperature,
                        batteryPowerSource = batteryPowerSource,
                        batteryChargingStatus = batteryChargingStatus,
                        batteryTechnology = batteryTechnology,
                        batteryVoltage = batteryVoltage,
                        longitude = SpUtil.getPreferenceString(context, PREF_LONGITUDE, " "),
                        latitude = SpUtil.getPreferenceString(context, PREF_LATITUDE, " "),
                        imei = imei,
                        imsi = imsi,
                        simSerialNumber = simSerialNumber,
                        networkName = networkName,
                        PhoneManufacturer = Build.MANUFACTURER,
                        PhoneModel = Build.MODEL,
                        PhoneBrand = Build.BRAND,
                        TotalStorage = "${df2.format(megTotal)} GB",
                        FreeStorage = "${df2.format(megAvailable)} GB",
                        TotalRam = "${df2.format(TotalRam)} MB",
                        FreeRam = "${df2.format(availableRAM)} MB",
                        client_sender = user,
                        date = Date().toString(),
                        client_gateway_type = ANDROID_PHONE,
                        airtimeBalance = "",
                        sentBundles = "",
                        receivedBundles = "",
                        totalBundles = " "
                        )

                    val gson = Gson()


                    val data = Data.Builder()
                        .putString(KEY_TASK_MESSAGE, gson.toJson(phoneStatus))
                        .putString(KEY_EMAIL, user)
                        .build()
                    sendToRabbitMQ(context, data)

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
