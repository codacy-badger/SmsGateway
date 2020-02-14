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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.didahdx.smsgatewaysync.HelperClass.printMessage
import com.didahdx.smsgatewaysync.services.SmsService
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
class HomeFragment : Fragment(), MessageAdapter.OnItemClickListener {

    private var messageList: ArrayList<MessageInfo> = ArrayList<MessageInfo>()
    val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
    val filter = IntentFilter(SMS_RECEIVED)
    private val PERMISSION_RECEIVE_SMS_CODE = 2
    private val PERMISSION_READ_SMS_CODE = 100
    private val PERMISSION_WRITE_EXTERNAL_STORAGE_CODE = 500
    val INPUT_EXTRAS = "inputExtras"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.recycler_view_message_list.layoutManager = LinearLayoutManager(activity)

        view.refresh_layout_home.setOnRefreshListener {
            backgroundCoroutineCall()
        }

        backgroundCoroutineCall()

        // Inflate the layout for this fragment
        return view
    }


    private fun startServices() {
        val serviceIntent = Intent(activity, SmsService::class.java)
        serviceIntent.putExtra(INPUT_EXTRAS, "SMS")
        ContextCompat.startForegroundService(activity as Activity, serviceIntent)
    }

    private fun stopServices(){
        val serviceIntent = Intent(activity, SmsService::class.java)
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
        recycler_view_message_list?.adapter = MessageAdapter(messageList, this)
        refresh_layout_home?.isRefreshing = false
    }


    private fun backgroundCoroutineCall() {
        checkSmsPermission()
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
            startServices()
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

        checkWriteExternalStoragePermission()

        if (ActivityCompat.checkSelfPermission(
                context as Activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val file = printMessage().createPdf(messageInfo.messageBody)
                val packageManager = activity?.packageManager
                val testIntent = Intent(Intent.ACTION_VIEW)
                testIntent.type = "application/pdf"

                val list = packageManager?.queryIntentActivities(
                    testIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                if (list?.size!! > 0) {
                    Toast.makeText(activity, "Printing", Toast.LENGTH_LONG).show()
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = Uri.fromFile(file)
                    intent.setDataAndType(uri, "application/pdf")
                    activity?.startActivity(intent)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    "Printing failed  ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
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
                    startServices()
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
}
