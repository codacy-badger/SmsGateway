package com.didahdx.smsgatewaysync.ui


import android.Manifest
import android.content.*
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.LinearLayoutManager
import com.didahdx.smsgatewaysync.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.receiver.SmsReceiver
import kotlinx.android.synthetic.main.fragment_home.*
import android.widget.Toast
import android.content.ContentResolver
import android.R.attr.name
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.didahdx.smsgatewaysync.HelperClass.printMessage
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
    private val PERMISSION_WRITE_EXTERNAL_STORAGE_CODE = 500


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        LocalBroadcastManager
            .getInstance(requireContext())
            .registerReceiver(mReceiver, filter)

        messageList = ArrayList<MessageInfo>()
        recycler_view_message_list.layoutManager = LinearLayoutManager(activity)

        refresh_layout_home.setOnRefreshListener {
            backgroundCoroutineCall()
        }

        backgroundCoroutineCall()
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
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver)
        super.onDestroyView()
    }

    private fun getMessages() {
        recycler_view_message_list?.adapter = MessageAdapter(messageList, this)
        refresh_layout_home?.isRefreshing = false
    }


    private fun backgroundCoroutineCall() {
        refresh_layout_home.isRefreshing = true
        //coroutine background job
        CoroutineScope(IO).launch {
            getDbMessages()
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
           val file= printMessage().createPdf(messageInfo.messageBody)
            val packageManager = activity?.packageManager
            val testIntent = Intent(Intent.ACTION_VIEW)
            testIntent.type = "application/pdf"

            val list =packageManager?.queryIntentActivities(testIntent,PackageManager.MATCH_DEFAULT_ONLY)
            if(list?.size!! > 0){
                Toast.makeText(activity,"Printing",Toast.LENGTH_LONG).show()
                val intent=Intent(Intent.ACTION_VIEW)
                val uri=Uri.fromFile(file)
                intent.setDataAndType(uri,"application/pdf")
                activity?.startActivity(intent)
            }else{
                Toast.makeText(activity,"Download a pdf viewer to see this file",Toast.LENGTH_LONG).show()
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
                context as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_WRITE_EXTERNAL_STORAGE_CODE
            )
        }
    }

}
