package com.didahdx.smsgatewaysync.ui


import android.Manifest
import android.content.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.LinearLayoutManager
import com.didahdx.smsgatewaysync.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.R
import kotlinx.android.synthetic.main.fragment_home.*
import android.widget.Toast
import android.app.Activity
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.App
import com.didahdx.smsgatewaysync.receiver.ConnectionReceiver
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(), MessageAdapter.OnItemClickListener,
    PrintingCallback ,ConnectionReceiver.ConnectionReceiverListener {

    /*********************************************************************************************************
     ********************* BLUETOOTH PRINTER CALLBACK METHODS ************************************************
     **********************************************************************************************************/

    override fun connectingWithPrinter() {
       Toast.makeText(activity,"Connecting to printer",Toast.LENGTH_SHORT).show()
    }

    override fun connectionFailed(error: String) {
        Toast.makeText(activity,"Connecting to printer failed $error",Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: String) {
        Toast.makeText(activity,"Error $error",Toast.LENGTH_SHORT).show()
    }

    override fun onMessage(message: String) {
        Toast.makeText(activity,"Message $message",Toast.LENGTH_SHORT).show()
    }

    override fun printingOrderSentSuccessfully() {
        Toast.makeText(activity,"Order sent to printer",Toast.LENGTH_SHORT).show()
    }

    /***************************************************************************************************************************/

    private var messageList: ArrayList<MessageInfo> = ArrayList<MessageInfo>()

    val filter = IntentFilter(SMS_RECEIVED)
    var printing: Printing? = null
    val appLog= AppLog()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.recycler_view_message_list.layoutManager = LinearLayoutManager(activity)

        //registering the broadcast receiver for network
        context?.registerReceiver(
            mConnectionReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))


        view.text_view_status.text="$APP_NAME is running"
        if (printing != null) {
            printing?.printingCallback = this
        }

        view.refresh_layout_home.setOnRefreshListener {
            backgroundCoroutineCall()
        }

        backgroundCoroutineCall()

        // Inflate the layout for this fragment
        return view
    }

    //appServices for showing notification bar
    private fun startServices() {
        val serviceIntent = Intent(activity, AppServices::class.java)
        serviceIntent.putExtra(INPUT_EXTRAS, "SmsGateway is running")
        ContextCompat.startForegroundService(activity as Activity, serviceIntent)
    }

    private fun stopServices() {
        val serviceIntent = Intent(activity, AppServices::class.java)
//       stopService(serviceIntent)
    }



    //broadcast sms receiver
    private val mSmsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {

            if (intent != null && intent.extras != null) {
                val phoneNumber = intent.extras!!.getString("phoneNumber")
                val dateString = intent.extras!!.getString("dateString")
                val messageText = intent.extras!!.getString("messageText")

                Toast.makeText(context, "$messageText $phoneNumber $dateString", Toast.LENGTH_LONG)
                    .show()

                if (phoneNumber != null && phoneNumber.equals("MPESA") && dateString != null
                    && messageText != null
                ) {
                    val mpesaId = messageText.split("\\s".toRegex()).first()
                    messageList.add(MessageInfo(messageText, dateString, phoneNumber, mpesaId))
                }
            }
        }

    }

    //broadcast connection receiver
    private val mConnectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {

            val connectionManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectionManager.activeNetworkInfo
            val isConnected= (activeNetwork != null && activeNetwork.isConnectedOrConnecting)
            when(isConnected){
                true->text_view_status.text="${getString(R.string.app_name)} is Running"
                false->text_view_status.text="No internet connection"
            }
        }
    }





    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
        super.onDestroyView()
    }

    private fun getMessages() {
        progress_bar?.visibility=View.GONE
        recycler_view_message_list?.adapter = MessageAdapter(messageList, this)
        refresh_layout_home?.isRefreshing = false
    }


    private fun backgroundCoroutineCall() {
        checkSmsPermission()
        startServices()
//        var mqttClient=MqttClient()
//        mqttClient.connect(activity as Activity)
//        mqttClient.publishMessage(activity as Activity,"test")


        if (ActivityCompat.checkSelfPermission(activity as Activity, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            view?.refresh_layout_home?.isRefreshing = true
            //coroutine background job
            CoroutineScope(IO).launch {
                var rabbitmqClient=RabbitmqClient()
                rabbitmqClient.publishMessage("Test message")
                getDbMessages()
            }
        }

        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.RECEIVE_SMS
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
                    LocalBroadcastManager
            .getInstance(requireContext())
            .registerReceiver(mSmsReceiver, filter)
        }

        LocalBroadcastManager
            .getInstance(requireContext())
            .registerReceiver(mConnectionReceiver,  IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    private suspend fun passMessagesToMain(list: ArrayList<MessageInfo>) {
        withContext(Main) {
            messageList.clear()
            messageList = ArrayList<MessageInfo>(list)
            getMessages()
        }
    }

    private suspend fun getDbMessages() {
        val messageArrayList = ArrayList<MessageInfo>()

        val cursor = activity!!.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            null,
            null,
            null,
            null
        )


        if (cursor!!.moveToNext()) {
            val nameId = cursor.getColumnIndex("address")
            val messageId = cursor.getColumnIndex("body")
            val dateId = cursor.getColumnIndex("date")

            do {
                val dateString = cursor.getString(dateId)

                if (cursor.getString(nameId).equals("MPESA")) {
                    val regexPattern = "^[A-Z0-9]*$"
                    var mpesaId: String =
                        cursor.getString(messageId).split("\\s".toRegex()).first().trim()
                    if (!regexPattern.toRegex().matches(mpesaId)) {
                        mpesaId = " "
                    }

                    messageArrayList.add(
                        MessageInfo(
                            cursor.getString(messageId),
                            Date(dateString.toLong()).toString(),
                            cursor.getString(nameId),
                            mpesaId
                        )
                    )
                }
            } while (cursor.moveToNext())

            cursor.close()
        }

        passMessagesToMain(messageArrayList)
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }


    override fun onItemClick(position: Int) {

    }

    //click Listener for pdf
    override fun onPrintPdf(position: Int) {
        val messageInfo: MessageInfo = messageList[position]
        val smsFilter=SmsFilter()
        val bluetoothPrinter=bluetoothPrinter()
        val smsprint=smsFilter.checkSmsType(messageInfo.messageBody)

//        Toast.makeText(activity, smsprint,
//            Toast.LENGTH_LONG).show()

        if (!Printooth.hasPairedPrinter()){
            startActivityForResult(
                Intent(activity, ScanningActivity::class.java)
                , ScanningActivity.SCANNING_FOR_PRINTER
            )
        }else{
            bluetoothPrinter.printText(smsprint,
                activity as Activity,getString(R.string.app_name))

        }


    }

    //used to check for write to external storage permission
    private fun checkWriteExternalStoragePermission() {
        if (ActivityCompat.checkSelfPermission(
                context as Activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_WRITE_EXTERNAL_STORAGE_CODE
            )
        }
    }

    private fun checkSmsPermission() {
        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.RECEIVE_SMS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                PERMISSION_RECEIVE_SMS_CODE
            )
        }

        if (ActivityCompat.checkSelfPermission(activity as Activity, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.READ_SMS),
                PERMISSION_READ_SMS_CODE
            )
        }

        if (ActivityCompat.checkSelfPermission(activity as Activity, Manifest.permission.FOREGROUND_SERVICE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(
                    activity as Activity,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                    PERMISSION_FOREGROUND_SERVICES_CODE
                )

            }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            PERMISSION_RECEIVE_SMS_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_READ_SMS_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }

            PERMISSION_WRITE_EXTERNAL_STORAGE_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "Permission granted", Toast.LENGTH_LONG).show()
                }

            }

            PERMISSION_FOREGROUND_SERVICES_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }





    private fun changePairAndUnpair() {
        if (!Printooth.hasPairedPrinter()) {
            Toast
                .makeText(
                    activity,
                    "Unpair ${Printooth.getPairedPrinter()?.name}",
                    Toast.LENGTH_LONG
                )
                .show()
        } else {
            Toast
                .makeText(
                    activity,
                    "Paired with Printer ${Printooth.getPairedPrinter()?.name}",
                    Toast.LENGTH_LONG
                )
                .show()
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==ScanningActivity.SCANNING_FOR_PRINTER && resultCode==Activity.RESULT_OK){
            initPrinter()
            changePairAndUnpair()
        }
    }

    private fun initPrinter() {
       if (Printooth.hasPairedPrinter()){
           printing=Printooth.printer()
       }

        if (printing!=null){
            printing?.printingCallback=this
        }
    }

    //checks on network connectivity to update the notification bar
    override fun onNetworkConnectionChanged(isConnected: Boolean) {

        if (!isConnected) {
            startServices("No internet connection")
            text_view_status.text="No internet connection"
            appLog.writeToLog(activity as Activity,"No internet Connection")
        } else {
            text_view_status.text="${getString(R.string.app_name)} is Running"
            startServices("${getString(R.string.app_name)} is Running")
            appLog.writeToLog(activity as Activity,"Connected to Internet")
        }
    }


    private fun startServices(input: String) {
        val serviceIntent = Intent(activity as Activity, AppServices::class.java)
        serviceIntent.putExtra(INPUT_EXTRAS, input)
        ContextCompat.startForegroundService(activity as Activity, serviceIntent)
    }


    override fun onDestroy() {
        super.onDestroy()
        activity?.unregisterReceiver(mConnectionReceiver)
    }
}