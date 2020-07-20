package com.didahdx.smsgatewaysync.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.utilities.*


/**
 * used to check battery status
 * */
class BatteryReceiver : BroadcastReceiver() {
    val newIntent = Intent(BATTERY_LOCAL_BROADCAST_RECEIVER)
    override fun onReceive(context: Context, intent: Intent) {
        val batteryStatus: String = checkBatteryStatus(intent, context)

    }

    private fun checkBatteryStatus(intent: Intent, context: Context): String {
        val stringBuilder = StringBuilder()
        val batteryPercentage = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)

        stringBuilder.append("Battery percentage: $batteryPercentage % \n")
        newIntent.putExtra(BATTERY_PERCENTAGE_EXTRA, batteryPercentage.toString())


        stringBuilder.append("\nBattery Condition: \n")

        val batteryCondition = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "over heat"
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failure"
            else -> "unknown"
        }

        newIntent.putExtra(BATTERY_CONDITION_EXTRA, batteryCondition)

        stringBuilder.append("\nBattery Temperature: \n")
        val temperatureInCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
        stringBuilder.append("$temperatureInCelsius \u00B0C\n")

        val tempratureInFarenheit = ((temperatureInCelsius * 1.8) + 32).toInt()
        stringBuilder.append("$tempratureInFarenheit \u00B0F\n")
        newIntent.putExtra(BATTERY_TEMPERATURE_EXTRA, "$temperatureInCelsius °C")

        stringBuilder.append("\nPower Source \n")


        val powerSource = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb connection"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless connection"
            else -> "no power sources"
        }
        newIntent.putExtra(BATTERY_POWER_SOURCE_EXTRA, powerSource)

        stringBuilder.append("\nCharging Status \n")
        val chargingStatus = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging "
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not charging "
            BatteryManager.BATTERY_STATUS_UNKNOWN -> "unknown "
            else -> "unKnown"
        }
        newIntent.putExtra(BATTERY_CHARGING_STATUS_EXTRA, chargingStatus)

        val technology = intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY)
        stringBuilder.append("\nTechnology \n $technology \n")
        newIntent.putExtra(BATTERY_TECHNOLOGY_EXTRA, technology)

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).toDouble() / 1000
        stringBuilder.append("\nVoltage \n $voltage V\n")
        newIntent.putExtra(BATTERY_VOLTAGE_EXTRA, "$voltage V")

        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)

        return stringBuilder.toString()
    }

}
