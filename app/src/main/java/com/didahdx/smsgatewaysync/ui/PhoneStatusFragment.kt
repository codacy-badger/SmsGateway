package com.didahdx.smsgatewaysync.ui

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.receiver.BatteryReceiver
import com.didahdx.smsgatewaysync.utilities.PERMISSION_CALL_PHONE_CODE
import com.didahdx.smsgatewaysync.utilities.toast
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            checkBalance()
        }
        context?.registerReceiver(mBatteryReceiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return inflater.inflate(R.layout.fragment_phone_status, container, false)
    }

    private fun checkBalance() {
        checkCallPhonePermission()

        val telephonyManager: TelephonyManager? =
            activity?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val handler: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                Log.e("ussd", message.toString())
            }
        }
        var callback: UssdResponseCallback?=null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            callback =
                object : UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        super.onReceiveUssdResponse(telephonyManager, request, response)
                        context?.toast("ussd response: $response")
                        Log.e("ussd", "Success with response : $response  ")
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager,
                        request: String,
                        failureCode: Int
                    ) {
                        super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode)
                        context?.toast(" request $request  \n failureCode $failureCode ")
                        Log.e("ussd", "failed with code $failureCode")
                    }
                }
        }

        if (ActivityCompat.checkSelfPermission(activity as Activity, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
        try {
            Log.e("ussd", "trying to send ussd request")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager!!.sendUssdRequest(
                   "*144#",
                    callback,
                    handler
                )
            }
        } catch (e: Exception) {
            val msg = e.message
            Log.e("DEBUG", e.toString())
            e.printStackTrace()
        }
    }
    }

    //broadcast sms receiver
    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stringBuilder=StringBuilder()
            val batteryPercentage=
                intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0)

            stringBuilder.append("Battery percentage: $batteryPercentage % \n")
            stringBuilder.append("\nBattery Condition: \n")

            when(intent.getIntExtra(BatteryManager.EXTRA_HEALTH,0)){
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
                intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10
            stringBuilder.append("$temperatureInCelsius \u00B0C\n")

            val tempratureInFarenheit=((temperatureInCelsius*1.8)+32).toInt()
            stringBuilder.append("$tempratureInFarenheit \u00B0F\n")

            stringBuilder.append("\n Power Source \n")

            when(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)){
                BatteryManager.BATTERY_PLUGGED_AC->stringBuilder.append("AC adapter\n")
                BatteryManager.BATTERY_PLUGGED_USB->stringBuilder.append("usb connection\n")
                BatteryManager.BATTERY_PLUGGED_WIRELESS->stringBuilder.append("Wireless connection\n")
                else->stringBuilder.append("no power sources\n")
            }

            stringBuilder.append("\n Charging Status \n")
            when(intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> stringBuilder.append("charging \n")
                BatteryManager.BATTERY_STATUS_DISCHARGING -> stringBuilder.append("discharging \n")
                BatteryManager.BATTERY_STATUS_FULL -> stringBuilder.append("full \n")
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> stringBuilder.append("not charging \n")
                BatteryManager.BATTERY_STATUS_UNKNOWN -> stringBuilder.append("unknown \n")
                else->stringBuilder.append("unKnown\n")
            }

            val technology= intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY)
            stringBuilder.append("\nTechnology \n $technology \n")

            val voltage= intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0).toDouble()/1000
            stringBuilder.append("\nVoltage \n $voltage V\n")

            text_view_phone_status?.text= stringBuilder.toString()
        }
    }

    private fun checkCallPhonePermission() {
        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.CALL_PHONE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSION_CALL_PHONE_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            PERMISSION_CALL_PHONE_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }}}
}
