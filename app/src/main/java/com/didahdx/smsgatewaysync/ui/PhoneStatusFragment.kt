package com.didahdx.smsgatewaysync.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.receiver.BatteryReceiver
import kotlinx.android.synthetic.main.fragment_phone_status.*

/**
 * A simple [Fragment] subclass.
 */
class PhoneStatusFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

            context?.registerReceiver(mBatteryReceiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return inflater.inflate(R.layout.fragment_phone_status, container, false)
    }


    //broadcast sms receiver
    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val stringBuilder=StringBuilder()
            val batteryPercentage=
                BatteryReceiver.intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0)

            stringBuilder.append("Battery percentage: $batteryPercentage % \n")
            stringBuilder.append("\nBattery Condition: \n")

            when(BatteryReceiver.intent.getIntExtra(BatteryManager.EXTRA_HEALTH,0)){
                BatteryManager.BATTERY_HEALTH_OVERHEAT-> stringBuilder.append("over heat\n")
                BatteryManager.BATTERY_HEALTH_GOOD-> stringBuilder.append("good\n")
                BatteryManager.BATTERY_HEALTH_COLD-> stringBuilder.append("cold\n")
                BatteryManager.BATTERY_HEALTH_DEAD-> stringBuilder.append("dead\n")
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE-> stringBuilder.append("over voltage\n")
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE-> stringBuilder.append("failure\n")
                else->stringBuilder.append("unknown")
            }

            stringBuilder.append("\nBattery Temperature: \n")
            val temperatureInCelsius=
                BatteryReceiver.intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10
            stringBuilder.append("$temperatureInCelsius \u00B0C\n")

            val tempratureInFarenheit=((temperatureInCelsius*1.8)+32).toInt()
            stringBuilder.append("$tempratureInFarenheit \u00B0F\n")

            stringBuilder.append("\n Power Source \n")

            when(BatteryReceiver.intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)){
                BatteryManager.BATTERY_PLUGGED_AC->stringBuilder.append("AC adapter\n")
                BatteryManager.BATTERY_PLUGGED_USB->stringBuilder.append("usb connection\n")
                BatteryManager.BATTERY_PLUGGED_WIRELESS->stringBuilder.append("Wireless connection\n")
                else->stringBuilder.append("no power sources\n")
            }

            stringBuilder.append("\n Charging Status \n")
            when(BatteryReceiver.intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> stringBuilder.append("charging \n")
                BatteryManager.BATTERY_STATUS_DISCHARGING -> stringBuilder.append("discharging \n")
                BatteryManager.BATTERY_STATUS_FULL -> stringBuilder.append("full \n")
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> stringBuilder.append("not charging \n")
                BatteryManager.BATTERY_STATUS_UNKNOWN -> stringBuilder.append("unknown \n")
                else->stringBuilder.append("unKnown\n")
            }

            val technology= BatteryReceiver.intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY)
            stringBuilder.append("\nTechnology \n $technology \n")

            val voltage= BatteryReceiver.intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0).toDouble()/1000
            stringBuilder.append("\nVoltage \n $voltage V\n")


//            text_view_phone_status.text= stringBuilder.toString()
        }
    }


}
