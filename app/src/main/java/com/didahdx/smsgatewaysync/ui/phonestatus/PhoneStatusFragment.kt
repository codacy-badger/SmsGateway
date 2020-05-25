package com.didahdx.smsgatewaysync.ui.phonestatus

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.PERMISSION_REQUEST_ALL_CODE
import com.didahdx.smsgatewaysync.utilities.toast
import kotlinx.android.synthetic.main.fragment_phone_status.*


/**
 * A simple [Fragment] subclass.
 */
class PhoneStatusFragment : Fragment() {
    private val appPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        checkAndRequestPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            checkBalance()
        }
        return inflater.inflate(R.layout.fragment_phone_status, container, false)
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(mBatteryReceiver)
    }

    private fun checkBalance() {
        val telephonyManager: TelephonyManager? =
            activity?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val handler: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                Log.e("ussd", message.toString())
            }
        }
        var callback: UssdResponseCallback? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            callback =
                object : UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        super.onReceiveUssdResponse(telephonyManager, request, response)

                        val bal = response.substring(
                            response.indexOf("Bal:") + 4,
                            response.indexOf("KSH")
                        )

                        text_view_phone_status?.append("\nAirtime Balance KSh: $bal")

//                        context?.toast("ussd response: $response")
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

        if (checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
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
            val stringBuilder = StringBuilder()
            val batteryPercentage =
                intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)

            stringBuilder.append("Battery percentage:  $batteryPercentage % \n")
            stringBuilder.append("\nBattery Condition : ")

            when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> stringBuilder.append("over heat\n")
                BatteryManager.BATTERY_HEALTH_GOOD -> stringBuilder.append("good\n")
                BatteryManager.BATTERY_HEALTH_COLD -> stringBuilder.append("cold\n")
                BatteryManager.BATTERY_HEALTH_DEAD -> stringBuilder.append("dead\n")
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> stringBuilder.append("over voltage\n")
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> stringBuilder.append("failure\n")
                else -> stringBuilder.append("unknown")
            }

            stringBuilder.append("\nBattery Temperature : ")
            val temperatureInCelsius =
                intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
            stringBuilder.append("$temperatureInCelsius \u00B0C \t\t")

            val tempratureInFarenheit = ((temperatureInCelsius * 1.8) + 32).toInt()
            stringBuilder.append("$tempratureInFarenheit \u00B0F\n")

            stringBuilder.append("\nPower Source  : ")

            when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_AC -> stringBuilder.append("AC adapter\n")
                BatteryManager.BATTERY_PLUGGED_USB -> stringBuilder.append("usb connection\n")
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> stringBuilder.append("Wireless connection\n")
                else -> stringBuilder.append("no power sources\n")
            }

            stringBuilder.append("\nCharging Status : ")
            when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> stringBuilder.append("charging \n")
                BatteryManager.BATTERY_STATUS_DISCHARGING -> stringBuilder.append("discharging \n")
                BatteryManager.BATTERY_STATUS_FULL -> stringBuilder.append("full \n")
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> stringBuilder.append("not charging \n")
                BatteryManager.BATTERY_STATUS_UNKNOWN -> stringBuilder.append("unknown \n")
                else -> stringBuilder.append("unKnown\n")
            }

            val technology = intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY)
            stringBuilder.append("\nTechnology : $technology \n")

            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).toDouble() / 1000
            stringBuilder.append("\nVoltage : $voltage V\n")

            val telephonyManager =
                requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                stringBuilder.append("\nIMEI number : ${telephonyManager.deviceId} \n  ")
                stringBuilder.append("\nNetwork Name : ${telephonyManager.networkOperatorName} \n  ")
                stringBuilder.append("\nSim Serial Number : ${telephonyManager.simSerialNumber}  \n ")
                var imsi: String = ""
                imsi=telephonyManager.subscriberId
                stringBuilder.append("\nIMSI : $imsi \n ")
            }

            stringBuilder.append("\nPhone manufacturer : ${Build.MANUFACTURER} \n  ")
            stringBuilder.append("\nPhone model : ${Build.MODEL} \n")
            stringBuilder.append("\nPhone brand : ${Build.BRAND} \n")

            text_view_phone_status?.text = stringBuilder.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                checkBalance()
            }

        }
    }


    //check and requests the permission which are required
    private fun checkAndRequestPermissions(): Boolean {
        val listPermissionsNeeded = ArrayList<String>()

        for (perm in appPermissions) {
            if (checkSelfPermission(requireContext(), perm)
                != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(perm)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            requestPermissions(
                listPermissionsNeeded.toArray(arrayOf(listPermissionsNeeded.size.toString()))
                , PERMISSION_REQUEST_ALL_CODE
            )
            return false;
        }
        return true;
    }


    //getting permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ALL_CODE) {
            val permissionResults: HashMap<String, Int> = HashMap()
            var deniedCount = 0

            for (i in grantResults) {
                if (i >= 0 && grantResults.isNotEmpty() && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i])
                    deniedCount++
                }
            }

            if (deniedCount == 0) {
                //initialise app
            } else {
                for (entry in permissionResults) {
                    var permName = entry.key
                    var permResult = entry.value

                    if (shouldShowRequestPermissionRationale(permName)) {
                        showDialog("", "This app needs $permName to work properly",
                            "Grant Permission"
                            , DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                checkAndRequestPermissions()
                            },
                            "Exit App", DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                activity?.finish()
                            }
                            , false)
                    } else {
                        showDialog("",
                            "You have denied some permissions. Allow all permissions at [Setting] > Permission",
                            "Go to Settings"
                            ,
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context?.packageName, null)
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                activity?.finish()

                            },
                            "Exit App",
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                activity?.finish()
                            }
                            ,
                            false)
                        break
                    }
                }
            }
        }
    }


    //used to display alert dialog box
    private fun showDialog(
        title: String, msg: String, postiveLabel: String,
        postiveOnClick: DialogInterface.OnClickListener,
        negativeLabel: String, negativeOnClick: DialogInterface.OnClickListener,
        isCancelable: Boolean
    ): AlertDialog {

        val builder = AlertDialog.Builder(activity as Activity)
        builder.setTitle(title)
        builder.setCancelable(isCancelable)
        builder.setMessage(msg)
        builder.setPositiveButton(postiveLabel, postiveOnClick)
        builder.setNegativeButton(negativeLabel, negativeOnClick)
        val alert = builder.create()
        alert.show()
        return alert;
    }


//    operator fun get(context: Context, key: String?): String? {
//        var ret = ""
//        try {
//            val cl = context.classLoader
//            val SystemProperties = cl.loadClass("android.os.SystemProperties")
//
//            //Parameters Types
//            val paramTypes: Array<Class<*>?> = arrayOfNulls(1)
//            paramTypes[0] = String::class.java
//            val get: Method = SystemProperties.getMethod("get", *paramTypes)
//
//            //Parameters
//            val params = arrayOfNulls<Any>(1)
//            params[0] = String(key)
//            ret = get.invoke(SystemProperties, params)
//        } catch (e: java.lang.Exception) {
//            ret = ""
//            //TODO : Error handling
//        }
//        return ret
//    }


}
