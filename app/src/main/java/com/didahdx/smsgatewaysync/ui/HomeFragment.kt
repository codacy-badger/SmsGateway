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
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.data.printable.Printable
import com.mazenrashed.printooth.data.printable.RawPrintable
import com.mazenrashed.printooth.data.printable.TextPrintable
import com.mazenrashed.printooth.data.printer.DefaultPrinter
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
class HomeFragment : Fragment(), MessageAdapter.OnItemClickListener, PrintingCallback {

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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.recycler_view_message_list.layoutManager = LinearLayoutManager(activity)

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        LocalBroadcastManager
//            .getInstance(requireContext())
//            .registerReceiver(mReceiver, filter)

    }


    //broadcast sms receiver
    private val mReceiver = object : BroadcastReceiver() {
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


    override fun onDestroyView() {
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver)
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

        if (ActivityCompat.checkSelfPermission(activity as Activity, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            view?.refresh_layout_home?.isRefreshing = true
            //coroutine background job
            CoroutineScope(IO).launch {
                getDbMessages()
            }
        }

        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.RECEIVE_SMS
            )
            == PackageManager.PERMISSION_GRANTED
        ) {

        }
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
                            Date(dateString.toLong()).toString(), cursor.getString(nameId), mpesaId
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

        if (!Printooth.hasPairedPrinter()){
            startActivityForResult(
                Intent(activity, ScanningActivity::class.java)
                , ScanningActivity.SCANNING_FOR_PRINTER
            )
        }else{
            printText(messageInfo.messageBody)
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


    fun printText(message: String) {
        val printables = ArrayList<Printable>()
        printables.add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build())

        //print header
        printables.add(
            TextPrintable.Builder()
                .setText("Test ${getString(R.string.app_name)}")
                .setCharacterCode(DefaultPrinter.CHARCODE_PC1252)
                .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                .setUnderlined(DefaultPrinter.UNDERLINED_MODE_ON)
                .setNewLinesAfter(1)
                .build()
        )

        //print body
        printables.add(
            TextPrintable.Builder()
                .setText(message)
                .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
                .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                .setUnderlined(DefaultPrinter.UNDERLINED_MODE_ON)
                .setNewLinesAfter(2)
                .build()
        )


        printing?.print(printables)
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
}