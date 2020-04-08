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
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.SmsDetailsActivity
import com.didahdx.smsgatewaysync.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.manager.RabbitmqClient
import com.didahdx.smsgatewaysync.model.MpesaMessageInfo
import com.didahdx.smsgatewaysync.repository.data.IncomingMessages
import com.didahdx.smsgatewaysync.repository.data.MessagesDatabase
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.services.LocationGpsService
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : BaseFragment(), MessageAdapter.OnItemClickListener,
    UiUpdaterInterface {

    private var messageList: ArrayList<MpesaMessageInfo> = ArrayList<MpesaMessageInfo>()
    var isConnected = false
    val appLog = AppLog()
    lateinit var mHomeViewModel: HomeViewModel
    var mMessageAdapter: MessageAdapter? = null
    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
    private lateinit var sharedPreferences: SharedPreferences
    val TAG = HomeFragment::class.java.simpleName
    lateinit var rabbitmqClient: RabbitmqClient
    private var locationBroadcastReceiver: BroadcastReceiver? = null
    var userLongitude: String? = " "
    var userLatitude: String? = " "
    val user = FirebaseAuth.getInstance().currentUser
    var UiUpdaterInterface: UiUpdaterInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiUpdaterInterface = this
        Log.d("Log_Rabbitmq", "isConnected $isConnected")
        CoroutineScope(IO).launch {
            rabbitmqClient = RabbitmqClient(UiUpdaterInterface, user?.email!!)
            Log.d("Log_Rabbitmq", " object ${rabbitmqClient.hashCode()}")

            Log.d("Log_Rabbitmq", "isConnected $isConnected")
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
        mHomeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        //registering the broadcast receiver for network
        context?.registerReceiver(
            mConnectionReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mSmsReceiver,
            IntentFilter(SMS_LOCAL_BROADCAST_RECEIVER)
        )

        checkLocationPermission()
        val intent = Intent(activity as Activity, LocationGpsService::class.java)
        context?.startService(intent)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)

        if (isServiceRunning) {
            startServices()
            text_view_status?.backgroundGreen()
            text_view_status?.text = "$APP_NAME is running"
        } else {
            text_view_status?.text = "$APP_NAME is stopped"
            text_view_status?.backgroundRed()
        }
        refresh_layout_home?.setOnRefreshListener { backgroundCoroutineCall() }

        backgroundCoroutineCall()
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
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

                    val date = ""
                    val time = 12L

                    if (intent.extras != null) {
                        val phoneNumber = intent.extras!!.getString("phoneNumber")
                        val dateString = intent.extras!!.getString("dateString")
                        val messageText = intent.extras!!.getString("messageText")
//                        val date = sdf.format(Date(time.toLong())).toString()

                        context?.toast(" local receiver \n $phoneNumber $messageText ")

                        CoroutineScope(IO).launch {
                            val obj: JSONObject? = JSONObject()
                            val smsFilter = messageText?.let { SmsFilter(it) }
                            obj?.put("message_body", messageText)
                            obj?.put("receipt_date", date)
                            obj?.put("sender_id", phoneNumber)
                            obj?.put("longitude", userLongitude)
                            obj?.put("latitude", userLatitude)
                            obj?.put("client_sender", user?.email!!)
                            obj?.put("client_gateway_type", "android_phone")

                            if (phoneNumber != null && phoneNumber == "MPESA") {
                                obj?.put("message_type", "mpesa")
                                obj?.put("voucher_number", smsFilter?.mpesaId)
                                obj?.put("transaction_type", smsFilter?.mpesaType)
                                obj?.put("phone_number", smsFilter?.phoneNumber)
                                obj?.put("name", smsFilter?.name)
                                if (smsFilter?.time != NOT_AVAILABLE && smsFilter?.date != NOT_AVAILABLE) {
                                    obj?.put(
                                        "transaction_date",
                                        "${smsFilter?.date} ${smsFilter?.time}"
                                    )
                                } else if (smsFilter?.date != NOT_AVAILABLE) {
                                    obj?.put("transaction_date", smsFilter?.date)
                                } else if (smsFilter?.time !=
                                    NOT_AVAILABLE
                                ) {
                                    obj?.put("transaction_date", smsFilter?.time)
                                }
                                obj?.put("amount", smsFilter?.amount)

                            } else {
                                obj?.put("message_type", "recieved_sms")

                            }


                            obj?.toString()?.let {
                                rabbitmqClient.publishMessage(it)

                                launch {
                                    var message2: IncomingMessages? = null
                                    if (messageText != null) {
                                        message2 = IncomingMessages(
                                            messageText, time,
                                            phoneNumber!!, true
                                        )
                                    } else {
                                        message2 = null
                                    }

                                    context.let { tex ->
                                        if (message2 != null) {
                                            MessagesDatabase(tex).getIncomingMessageDao()
                                                .addMessage(message2)
                                        }
                                    }
                                } } }
                    } }
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
        super.onDestroyView()
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
        if (locationBroadcastReceiver != null) {
            context?.unregisterReceiver(locationBroadcastReceiver)
        }
    }

    private fun setUpAdapter() {
        progress_bar?.hide()
        text_loading?.hide()
        recycler_view_message_list?.layoutManager = LinearLayoutManager(activity)
        mMessageAdapter = MessageAdapter(messageList, this)
        recycler_view_message_list?.adapter = mMessageAdapter
        refresh_layout_home?.isRefreshing = false
    }

    //used to get sms from the phone
    private fun backgroundCoroutineCall() {
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

    }

    //adds message to the screen
    private suspend fun passMessagesToMain(list: ArrayList<MpesaMessageInfo>) {
        withContext(Main) {
            messageList.clear()
            messageList = ArrayList<MpesaMessageInfo>(list)
            setUpAdapter()
        }
    }


    //updates the counter on the screen
    private suspend fun UpdateCounter(messageCount: Int) {
        withContext(Main) {
            text_loading?.text = getString(R.string.loading_messages, messageCount)
        }
    }

    private suspend fun getDatabaseMessages() {

        val messageArrayList = ArrayList<MpesaMessageInfo>()

        val cursor = activity?.contentResolver?.query(
            Uri.parse("content://sms/inbox"),
            null,
            null,
            null,
            null
        )

        var messageCount = 0

        if (cursor != null && cursor.moveToNext()) {
            val nameId = cursor.getColumnIndex("address")
            val messageId = cursor.getColumnIndex("body")
            val dateId = cursor.getColumnIndex("date")
            val mpesaType = " "
            sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)


            Log.d(TAG, "mpesa sms $mpesaType ")

            do {
                val dateString = cursor.getString(dateId)

//                if (cursor.getString(nameId) == "MPESA") {

                var mpesaId: String =
                    cursor.getString(messageId).split("\\s".toRegex()).first().trim()
                if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
                    mpesaId = NOT_AVAILABLE
                }

                var smsFilter = SmsFilter(cursor.getString(messageId))


                when (mpesaType) {
                    PAY_BILL -> {
                        if (smsFilter.mpesaType == PAY_BILL) {
                            messageArrayList.add(
                                MpesaMessageInfo(
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId,
                                    "",
                                    smsFilter.amount,
                                    "",
                                    smsFilter.name,
                                    dateString.toLong()
                                )
                            )
                            UpdateCounter(messageCount)
                            messageCount++
                        }
                    }
                    DIRECT_MPESA -> {
                        if (smsFilter.mpesaType == DIRECT_MPESA) {
                            messageArrayList.add(
                                MpesaMessageInfo(
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId,
                                    "",
                                    smsFilter.amount,
                                    "",
                                    smsFilter.name,
                                    dateString.toLong()
                                )
                            )
                            UpdateCounter(messageCount)
                            messageCount++
                        }
                    }

                    BUY_GOODS_AND_SERVICES -> {
                        if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                            messageArrayList.add(
                                MpesaMessageInfo(
                                    cursor.getString(messageId),
                                    sdf.format(Date(dateString.toLong())).toString(),
                                    cursor.getString(nameId),
                                    mpesaId,
                                    "",
                                    smsFilter.amount,
                                    "",
                                    smsFilter.name,
                                    dateString.toLong()
                                )
                            )
                            UpdateCounter(messageCount)
                            messageCount++
                        }
                    }

                    else -> {
                        messageArrayList.add(
                            MpesaMessageInfo(
                                cursor.getString(messageId),
                                sdf.format(Date(dateString.toLong())).toString(),
                                cursor.getString(nameId),
                                mpesaId,
                                "",
                                smsFilter.amount,
                                "",
                                smsFilter.name,
                                dateString.toLong()
                            )
                        )
                        UpdateCounter(messageCount)
                        messageCount++
                    }
                }
//                }
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
        val messageInfo: MpesaMessageInfo = messageList[position]

        val date = messageInfo.dateTime
        var smsStatus = NOT_AVAILABLE
        var incomingMessages: IncomingMessages

        CoroutineScope(IO).launch {
            context?.let {


                val messagesList = MessagesDatabase(it).getIncomingMessageDao().getMessage(date)
                val messagesList2 = MessagesDatabase(it).getIncomingMessageDao().getAllMessages()

                val i = 0

                CoroutineScope(Main).launch {
                    context?.toast("${messagesList2?.size}")

                    context?.toast(
                        "count sms ${messagesList2?.size}  ${messagesList2[i]?.messageBody}  ${sdf.format(
                            Date(messagesList2[i]?.date)
                        )}  ${messagesList2[i]?.date} ${messagesList2[i]?.status} " +
                                " mlist  ${messagesList?.size} $date  ${sdf.format(
                                    Date(date)
                                )}"
                    )


                    if (!messagesList.isNullOrEmpty() && messagesList[0].status) {
                        smsStatus = if (messagesList[0].status) {
                            "Uploaded"
                        } else {
                            "pending"
                        }
                    }


//                    context?.toast("${messagesList.size}")
//                    context?.toast(smsStatus)
                    val intent = Intent(context, SmsDetailsActivity::class.java)
                    intent.putExtra(SMS_BODY_EXTRA, messageInfo.messageBody)
                    intent.putExtra(SMS_DATE_EXTRA, messageInfo.time)
                    intent.putExtra(SMS_SENDER_EXTRA, messageInfo.sender)
                    intent.putExtra(SMS_UPLOAD_STATUS_EXTRA, smsStatus)
                    startActivity(intent)
                }

            }

        }


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

    //used to check if the app has connected
    override fun isConnected(value: Boolean) {
        CoroutineScope(Main).launch {
            isConnected = value
        }
    }

    //used to show to show
    override fun notificationMessage(message: String) {
        CoroutineScope(Main).launch {

        }
    }

    //used to show toast messages
    override fun toasterMessage(message: String) {
        Log.d("Rabbit", "called $message")
        Log.d("Rabbit", "thread name ${Thread.currentThread().name}")
        CoroutineScope(Main).launch {
            context?.toast(message)
            Log.d("Rabbit", "thread name ${Thread.currentThread().name}")
        }
    }

    //used to update status bar
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


    //sending out sms
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
                    context, 0, Intent(SMS_SENT_INTENT), 0
                )

                val deliveredPI = PendingIntent.getBroadcast(
                    context, 0, Intent(SMS_DELIVERED_INTENT), 0
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
                }, IntentFilter(SMS_SENT_INTENT))

                //when the SMS has been delivered
                context?.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(arg0: Context?, arg1: Intent?) {
                        when (resultCode) {
                            Activity.RESULT_OK -> context?.toast("SMS delivered")
                            Activity.RESULT_CANCELED -> context?.toast("SMS not delivered")
                        }
                    }
                }, IntentFilter(SMS_DELIVERED_INTENT))

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
                    arrayDelivery
                )
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (locationBroadcastReceiver == null) {
            locationBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (LOCATION_UPDATE_INTENT == intent.action) {
                        val longitude = intent.getStringExtra(LONGITUDE_EXTRA)
                        val latitude = intent.getStringExtra(LATITUDE_EXTRA)
                        val altitude = intent.getStringExtra(ALTITUDE_EXTRA)
                        userLatitude = latitude
                        userLongitude = longitude

                    }
                }
            }
        }
        context?.registerReceiver(locationBroadcastReceiver, IntentFilter(LOCATION_UPDATE_INTENT))
    }


    private fun checkLocationPermission() {
        if (checkSelfPermission(
                activity as Activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                activity as Activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_ACCESS_FINE_LOCATION_CODE
            )
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_ACCESS_COARSE_LOCATION_CODE
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

    }


}