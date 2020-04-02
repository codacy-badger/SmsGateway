package com.didahdx.smsgatewaysync.ui

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.SmsDetailsActivity
import com.didahdx.smsgatewaysync.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.manager.RabbitmqClient
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.viewmodels.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.mazenrashed.printooth.Printooth
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(), MessageAdapter.OnItemClickListener,
    UiUpdaterInterface {

    private var messageList: ArrayList<MessageInfo> = ArrayList<MessageInfo>()


    var isConnected = false

    val appLog = AppLog()
    lateinit var mHomeViewModel: HomeViewModel
    var mMessageAdapter: MessageAdapter? = null
    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
    private lateinit var sharedPreferences: SharedPreferences
    val TAG = HomeFragment::class.java.simpleName
    lateinit var rabbitmqClient: RabbitmqClient

    val user = FirebaseAuth.getInstance().currentUser
    var UiUpdaterInterface: UiUpdaterInterface? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiUpdaterInterface = this
        CoroutineScope(IO).launch {
            rabbitmqClient = RabbitmqClient(UiUpdaterInterface, user?.email!!)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
            if (!isConnected && isServiceRunning) {
                rabbitmqClient.connection()
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.recycler_view_message_list.layoutManager = LinearLayoutManager(activity)
        mHomeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)


        //registering the broadcast receiver for network
        context?.registerReceiver(
            mConnectionReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        context?.registerReceiver(mSmsReceiver, IntentFilter(SMS_RECEIVED))
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)

        if (isServiceRunning) {
            startServices()
            view.text_view_status?.backgroundGreen()
            view.text_view_status.text = "$APP_NAME is running"
        } else {
            view.text_view_status.text = "$APP_NAME is stopped"
            view.text_view_status?.backgroundRed()
        }
        view.refresh_layout_home.setOnRefreshListener { backgroundCoroutineCall() }

        backgroundCoroutineCall()
        // Inflate the layout for this fragment
        return view
    }

    //appServices for showing notification bar
    private fun startServices() {
        val serviceIntent = Intent(activity, AppServices::class.java)
        serviceIntent.putExtra(INPUT_EXTRAS, "$APP_NAME is running")
        ContextCompat.startForegroundService(activity as Activity, serviceIntent)

    }

    private fun stopServices() {
        val serviceIntent = Intent(activity, AppServices::class.java)
//       stopService(serviceIntent)
    }


    //broadcast sms receiver
    private val mSmsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
            if (intent != null && isServiceRunning) {
                if (SMS_RECEIVED == intent.action) {

                    val extras = intent.extras
                    if (extras != null) {
                        val sms = extras.get("pdus") as Array<*>

                        for (i in sms.indices) {
                            val format = extras.getString("format")

                            var smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                SmsMessage.createFromPdu(sms[0] as ByteArray, format)
                            } else {
                                SmsMessage.createFromPdu(sms[0] as ByteArray)
                            }
                            val phoneNumber = smsMessage.originatingAddress
                            val messageText = smsMessage.messageBody.toString()
                            val sms = smsMessage.displayMessageBody

                            CoroutineScope(IO).launch {
                                rabbitmqClient.publishMessage(sms)
                            }

                            val printer = BluetoothPrinter()
                            val smsFilter = SmsFilter()
                            if (Printooth.hasPairedPrinter()) {
                                val printMessage = smsFilter.checkSmsType(messageText)
                                printer.printText(printMessage, context, APP_NAME)
                            } else {
                                Toast.makeText(context, "Printer not connected", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
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
            when ((activeNetwork != null && activeNetwork.isConnectedOrConnecting)) {
                true -> {
//                    context?.toast("network connected")
//                    text_view_status.text = "${getString(R.string.app_name)} is Running"
//                    text_view_status?.BackGroundGreen()
                }
                false -> {
//                    context?.toast("network not available")
//                    text_view_status.text = "No internet connection"
//                    text_view_status?.BackGroundRed()
                }
            }
        }
    }


    override fun onDestroyView() {
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
        super.onDestroyView()
    }

    private fun setUpAdapter() {
        progress_bar?.hide()
        text_loading?.hide()

        mMessageAdapter = MessageAdapter(messageList, this)
        recycler_view_message_list?.adapter = mMessageAdapter
        refresh_layout_home?.isRefreshing = false
    }


    fun backgroundCoroutineCall() {

        if (checkSelfPermission(activity as Activity, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            view?.refresh_layout_home?.isRefreshing = true
            view?.text_loading?.text = getString(R.string.loading_messages, 0)
            //coroutine background job
            CoroutineScope(IO).launch {
                getDatabaseMessages()
            }
        } else {
            progress_bar?.hide()
            text_loading?.hide()
            setUpAdapter()
        }

        if (checkSelfPermission(
                activity as Activity,
                Manifest.permission.RECEIVE_SMS
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
//            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mSmsReceiver, IntentFilter(SMS_RECEIVED))

        }

    }

    private suspend fun passMessagesToMain(list: ArrayList<MessageInfo>) {
        withContext(Main) {
            messageList.clear()
            messageList = ArrayList<MessageInfo>(list)
            setUpAdapter()
        }
    }


    private suspend fun UpdateCounter(messageCount: Int) {
        withContext(Main) {
            text_loading?.text = getString(R.string.loading_messages, messageCount)
        }
    }

    private suspend fun getDatabaseMessages() {

        val messageArrayList = ArrayList<MessageInfo>()

        val cursor = activity?.contentResolver?.query(
            Uri.parse("content://sms/inbox"),
            null,
            null,
            null,
            null
        )

        var messageCount = 0

        Log.d(TAG, "mpesa $messageCount")
        Log.d(TAG, "mpesa $cursor")
        if (cursor != null && cursor.moveToNext()) {
            val nameId = cursor.getColumnIndex("address")
            val messageId = cursor.getColumnIndex("body")
            val dateId = cursor.getColumnIndex("date")
            val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, ALL)


            Log.d(TAG, "mpesa sms $mpesaType ")

            do {
                val dateString = cursor.getString(dateId)

                if (cursor.getString(nameId) == "MPESA") {

                    var mpesaId: String =
                        cursor.getString(messageId).split("\\s".toRegex()).first().trim()
                    if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
                        mpesaId = NOT_AVAILABLE
                    }

                    var smsFilter = SmsFilter(cursor.getString(messageId))


                    when (mpesaType) {
                        ALL -> {
                            messageArrayList.add(
                                MessageInfo(
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId, "", smsFilter.amount, "", smsFilter.name
                                )
                            )
                            UpdateCounter(messageCount)
                            messageCount++
                        }
                        PAY_BILL -> {
                            if (smsFilter.mpesaType == PAY_BILL) {
                                messageArrayList.add(
                                    MessageInfo(
                                        cursor.getString(messageId),
                                        sdf.format(Date(dateString.toLong())).toString(),
                                        cursor.getString(nameId),
                                        mpesaId, "", smsFilter.amount, "", smsFilter.name
                                    )
                                )
                                UpdateCounter(messageCount)
                                messageCount++
                            }
                        }
                        DIRECT_MPESA -> {
                            if (smsFilter.mpesaType == DIRECT_MPESA) {
                                messageArrayList.add(
                                    MessageInfo(
                                        cursor.getString(messageId),
                                        sdf.format(Date(dateString.toLong())).toString(),
                                        cursor.getString(nameId),
                                        mpesaId, "", smsFilter.amount, "", smsFilter.name
                                    )
                                )
                                UpdateCounter(messageCount)
                                messageCount++
                            }
                        }

                        BUY_GOODS_AND_SERVICES -> {
                            if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                                messageArrayList.add(
                                    MessageInfo(
                                        cursor.getString(messageId),
                                        sdf.format(Date(dateString.toLong())).toString(),
                                        cursor.getString(nameId),
                                        mpesaId, "", smsFilter.amount, "", smsFilter.name
                                    )
                                )
                                UpdateCounter(messageCount)
                                messageCount++
                            }
                        }

                        else -> {
                            messageArrayList.add(
                                MessageInfo(
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId, "", smsFilter.amount, "", smsFilter.name
                                )
                            )
                            UpdateCounter(messageCount)
                            messageCount++
                        }
                    }
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
        val messageInfo: MessageInfo = messageList[position]
        //Put the value
//        val smsDetailsFragment = SmsDetailsFragment()
//        val args = Bundle()
//        args.putString(SMS_BODY, messageInfo.messageBody)
//        args.putString(SMS_DATE, messageInfo.time)
//        args.putString(SMS_SENDER, messageInfo.sender)
//        smsDetailsFragment.arguments = args

        val intent = Intent(context, SmsDetailsActivity::class.java)
        intent.putExtra(SMS_BODY, messageInfo.messageBody)
        intent.putExtra(SMS_DATE, messageInfo.time)
        intent.putExtra(SMS_SENDER, messageInfo.sender)
        startActivity(intent)

//        fragmentManager
//            ?.beginTransaction()
//            ?.replace(R.id.frame_layout, smsDetailsFragment)
//            ?.addToBackStack("fragment_home")
//            ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//            ?.commit()

    }

    //click Listener for pdf
    override fun onPrintPdf(position: Int) {

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


    private fun startServices(input: String) {
        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)

        if (isServiceRunning) {
            try {
                val serviceIntent = Intent(context, AppServices::class.java)
                serviceIntent.putExtra(INPUT_EXTRAS, input)
                context?.let { ContextCompat.startForegroundService(it, serviceIntent) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        activity?.unregisterReceiver(mConnectionReceiver)
    }

    override fun isConnected(value: Boolean) {
        isConnected = value
    }

    override fun notificationMessage(message: String) {
        CoroutineScope(Main).launch {

        }
    }

    override fun toasterMessage(message: String) {
        Log.d("Rabbit", "called $message")
        Log.d("Rabbit", "thread name ${Thread.currentThread().name}")
        CoroutineScope(Main).launch {
            context?.toast(message)
            Log.d("Rabbit", "thread name ${Thread.currentThread().name}")
        }
    }

    override fun updateStatusViewWith(status: String, color: String) {
        CoroutineScope(Main).launch {
            text_view_status?.text = status
            startServices(status)
            when (color) {
                RED_COLOR -> {
                    text_view_status?.backgroundRed()
                }

                GREEN_COLOR -> {
                    text_view_status?.backgroundGreen()
                }
                else -> {
                }
            }
        }
    }

    override fun publish(isReadyToPublish: Boolean) {
        if (isReadyToPublish) {
        }
    }

    override fun sendSms(phoneNumber: String, message: String) {
        CoroutineScope(Main).launch {

            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.SEND_SMS
                    )
                }
                == PackageManager.PERMISSION_GRANTED
            ) {
                val smsManager = SmsManager.getDefault()

                val sentPI = PendingIntent.getBroadcast(
                    context, 0, Intent(SENT), 0
                )

                val deliveredPI = PendingIntent.getBroadcast(
                    context, 0, Intent(DELIVERED), 0
                )


                //when the SMS has been sent
                context?.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(arg0: Context?, arg1: Intent?) {
                        when (resultCode) {
                            Activity.RESULT_OK -> context?.toast("SMS sent to $phoneNumber")
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> context?.toast("Generic failure")
                            SmsManager.RESULT_ERROR_NO_SERVICE -> context?.toast("No service")
                            SmsManager.RESULT_ERROR_NULL_PDU -> context?.toast("Null PDU")
                            SmsManager.RESULT_ERROR_RADIO_OFF -> context?.toast("Radio off")
                        }
                    }
                }, IntentFilter(SENT))

                //when the SMS has been delivered
                context?.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(arg0: Context?, arg1: Intent?) {
                        when (resultCode) {
                            Activity.RESULT_OK -> context?.toast("SMS delivered")
                            Activity.RESULT_CANCELED -> context?.toast("SMS not delivered")
                        }
                    }
                }, IntentFilter(DELIVERED))

                val parts = smsManager.divideMessage(message)

                val arraySendInt = java.util.ArrayList<PendingIntent>()
                arraySendInt.add(sentPI)
                val arrayDelivery = java.util.ArrayList<PendingIntent>()
                arrayDelivery.add(deliveredPI)
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    arraySendInt,
                    arrayDelivery)
            }
        }
    }

}