package com.didahdx.smsgatewaysync.presentation.phonestatus

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.content.Context.ACTIVITY_SERVICE
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.AppLog
import com.didahdx.smsgatewaysync.utilities.GIGABYTE
import com.didahdx.smsgatewaysync.utilities.PERMISSION_REQUEST_ALL_CODE
import com.didahdx.smsgatewaysync.utilities.toast
import kotlinx.android.synthetic.main.fragment_phone_status.*
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


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
                Timber.e(message.toString())
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
                        try {
                            if (response.indexOf("Bal:") > -1 && response.indexOf(".") > -1) {
                                val bal = response.substring(
                                    response.indexOf("Bal:") + 4,
                                    response.indexOf(".")+3
                                )
                                text_view_phone_status?.append("\nAirtime Balance KSh: $bal")
                            }

                            context?.toast("ussd response: $response")
                            Timber.e("Success with response : $response  ")
                        } catch (e: Exception) {
                            context?.let { AppLog.logMessage("$e  ${e.localizedMessage}", it) }
                            e.printStackTrace()
                            Timber.d("$e  ${e.localizedMessage}")
                        }
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager,
                        request: String,
                        failureCode: Int
                    ) {
                        super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode)
                        context?.toast(" request $request  \n failureCode $failureCode ")
                        Timber.e("failed with code $failureCode")
                    }
                }
        }

        if (checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            == PermissionChecker.PERMISSION_GRANTED
        ) {
            try {
                Timber.e("trying to send ussd request")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telephonyManager!!.sendUssdRequest(
                        "*144#",
                        callback,
                        handler
                    )
                }
            } catch (e: Exception) {
                Timber.e("${e.message} $e")
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
                val imsi: String = telephonyManager.subscriberId
                stringBuilder.append("\nIMSI : $imsi \n ")
            }

            stringBuilder.append("\nPhone manufacturer : ${Build.MANUFACTURER} \n  ")
            stringBuilder.append("\nPhone model : ${Build.MODEL} \n")
            stringBuilder.append("\nPhone brand : ${Build.BRAND} \n")

//            val maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024
//            val totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024
//            val freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024
//
//            stringBuilder.append("\nTotal Memory  : $totalMemory MiB\n")
//            stringBuilder.append("\nMax Memory  : $maxMemory MiB\n")
//            stringBuilder.append("\nUsed Memory  : ${(maxMemory - freeMemory)} MiB\n")
//            stringBuilder.append("\nFree Memory  : $freeMemory MiB\n")

            val df2 = DecimalFormat("###,###,###,###.00")
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val bytesAvailable: Long = stat.blockSizeLong * stat.availableBlocksLong
            val megAvailable: Double = (bytesAvailable.toDouble() / (GIGABYTE).toDouble())
            val megTotal: Double =
                (stat.blockSizeLong * stat.blockCountLong).toDouble() / (GIGABYTE).toDouble()

            val freePercent: Double = (megAvailable / megTotal) * (100).toDouble()
            stringBuilder.append("\nTotal Storage : ${df2.format(megTotal)} GB\n")
            stringBuilder.append(
                "\nFree Storage : ${df2.format(megAvailable)} GB  " +
                        "(${df2.format(freePercent)}%)\n"
            )

            val mi = ActivityManager.MemoryInfo()
            val activityManager =
                requireContext().getSystemService(ACTIVITY_SERVICE) as ActivityManager?
            activityManager!!.getMemoryInfo(mi)
            val availableRAM: Double = mi.availMem / (0x100000L).toDouble()
            val TotalRam: Double = mi.totalMem / (0x100000L).toDouble()
            val percentAvail: Double = mi.availMem / mi.totalMem.toDouble() * 100.0
            stringBuilder.append("\nTotal Ram : ${df2.format(TotalRam)} MB \n")
            stringBuilder.append(
                "\nAvailable Ram : ${df2.format(availableRAM)} MB (${df2.format(
                    percentAvail
                )})% \n"
            )


            text_view_phone_status?.text = stringBuilder.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                checkBalance()
            }
            stringBuilder.append(networkUsage())
        }
    }

    fun networkUsage(): String {
        var message = " "
        // Get running processes
        val manager =
            requireContext().getSystemService(ACTIVITY_SERVICE) as ActivityManager?
        val runningApps =
            manager!!.runningAppProcesses
        for (runningApp in runningApps) {
            val received = TrafficStats.getUidRxBytes(runningApp.uid)
            val sent = TrafficStats.getUidTxBytes(runningApp.uid)
            Timber.d(
                String.format(
                    Locale.getDefault(),
                    "uid: %1d - name: %s: Sent = %1d, Rcvd = %1d",
                    runningApp.uid,
                    runningApp.processName,
                    sent,
                    received
                )
            )

            message += "${Locale.getDefault()} uid: ${runningApp.uid} - name:" +
                    " ${runningApp.processName}: Sent = $sent, Rcvd = $received"


        }

        return message
    }


    //check and requests the permission which are required
    private fun checkAndRequestPermissions(): Boolean {
        val listPermissionsNeeded = ArrayList<String>()

        for (perm in appPermissions) {
            if (checkSelfPermission(requireContext(), perm)
                != PermissionChecker.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(perm)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            requestPermissions(
                listPermissionsNeeded.toArray(arrayOf(listPermissionsNeeded.size.toString()))
                , PERMISSION_REQUEST_ALL_CODE
            )
            return false
        }
        return true
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
                    permissionResults[permissions[i]] = grantResults[i]
                    deniedCount++
                }
            }

            if (deniedCount == 0) {
                //initialise app
            } else {
                for (entry in permissionResults) {
                    val permName = entry.key
//                    var permResult = entry.value

                    if (shouldShowRequestPermissionRationale(permName)) {
                        showDialog("", "This app needs $permName to work properly",
                            "Grant Permission"
                            , DialogInterface.OnClickListener { dialog, _ ->
                                dialog.dismiss()
                                checkAndRequestPermissions()
                            },
                            "Exit App", DialogInterface.OnClickListener { dialog, _ ->
                                dialog.dismiss()
                                activity?.finish()
                            }
                            , false)
                    } else {
                        showDialog("",
                            "You have denied some permissions. Allow all permissions at [Setting] > Permission",
                            "Go to Settings"
                            ,
                            DialogInterface.OnClickListener { dialog, _ ->
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
                            DialogInterface.OnClickListener { dialog, _ ->
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
        return alert
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
