package com.didahdx.smsgatewaysync.ui

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.model.MpesaMessageInfo
import com.didahdx.smsgatewaysync.repository.data.IncomingMessages
import com.didahdx.smsgatewaysync.repository.data.MessagesDatabase
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.services.LocationGpsService
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.viewmodels.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
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
    private val appPermissions= arrayOf(  Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.READ_SMS,
         Manifest.permission.READ_PHONE_STATE)

    @Volatile
    lateinit var rabbitmqClient: RabbitmqClient
    private var locationBroadcastReceiver: BroadcastReceiver? = null
    var userLongitude: String? = " "
    var userLatitude: String? = " "
    val user = FirebaseAuth.getInstance().currentUser
    var UiUpdaterInterface: UiUpdaterInterface? = null
    var outgoingMessages: Queue<MessageInfo> = LinkedList()
    var messageCount = 0
    var lastMessageSentTime = 0

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
                rabbitmqClient.connection(activity as Activity)

            }
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mSmsReceiver,
            IntentFilter(SMS_LOCAL_BROADCAST_RECEIVER)
        )
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

        if (checkAndRequest()){
            checkLocationPermission()
            checkPhoneStatusPermission()
        }
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


        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        refresh_layout_home?.setOnRefreshListener { backgroundCoroutineCall() }
        backgroundCoroutineCall()
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
            if (intent != null && isServiceRunning && SMS_LOCAL_BROADCAST_RECEIVER == intent.action) {
                Log.d("sms_rece", "action local ${intent.action}")
                if (intent.extras != null) {
                    val phoneNumber = intent.extras!!.getString("phoneNumber")
                    val dateTimeStamp = intent.extras!!.getLong("date")
                    val messageText = intent.extras!!.getString("messageText")
                    val date = sdf.format(Date(dateTimeStamp)).toString()

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
                                        messageText, dateTimeStamp,
                                        phoneNumber!!, true
                                    )
                                } else {
                                    message2 = null
                                }

                                context.let { tex ->
                                    if (message2 != null) {
                                        MessagesDatabase(tex).getIncomingMessageDao()
                                            .updateMessage(message2)
                                    }
                                }
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
    private suspend fun updateCounter(messageCount: Int) {
        withContext(Main) {
            text_loading?.text = getString(R.string.loading_messages, messageCount)
        }
    }

    private suspend fun getDatabaseMessages() {

        var messageArrayList = ArrayList<MpesaMessageInfo>()
        var incomingMessages = ArrayList<IncomingMessages>()
        context?.let {

            incomingMessages.addAll(MessagesDatabase(it).getIncomingMessageDao().getAllMessages())
        }
        val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)


        if (incomingMessages.size > 0) {
            for (i in incomingMessages.indices) {
                val smsFilter = SmsFilter(incomingMessages[i].messageBody)

                when (mpesaType) {
                    PAY_BILL -> {
                        if (smsFilter.mpesaType == PAY_BILL) {
                            messageArrayList.add(
                                MpesaMessageInfo(
                                    incomingMessages[i].id,
                                    incomingMessages[i].messageBody,
                                    sdf.format(Date(incomingMessages[i].date)).toString(),
                                    incomingMessages[i].sender,
                                    smsFilter.mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    incomingMessages[i].date,
                                    incomingMessages[i].status
                                )
                            )
                            updateCounter(i)
                        }
                    }

                    DIRECT_MPESA -> {
                        if (smsFilter.mpesaType == DIRECT_MPESA) {
                            messageArrayList.add(
                                MpesaMessageInfo(
                                    incomingMessages[i].id,
                                    incomingMessages[i].messageBody,
                                    sdf.format(Date(incomingMessages[i].date)).toString(),
                                    incomingMessages[i].sender,
                                    smsFilter.mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    incomingMessages[i].date,
                                    incomingMessages[i].status
                                )
                            )
                            updateCounter(i)
                        }
                    }

                    BUY_GOODS_AND_SERVICES -> {
                        if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
                            messageArrayList.add(
                                MpesaMessageInfo(
                                    incomingMessages[i].id,
                                    incomingMessages[i].messageBody,
                                    sdf.format(Date(incomingMessages[i].date)).toString(),
                                    incomingMessages[i].sender,
                                    smsFilter.mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    incomingMessages[i].date,
                                    incomingMessages[i].status
                                )
                            )
                            updateCounter(i)
                        }
                    }
                    else -> {

                        messageArrayList.add(
                            MpesaMessageInfo(
                                incomingMessages[i].id,
                                incomingMessages[i].messageBody,
                                sdf.format(Date(incomingMessages[i].date)).toString(),
                                incomingMessages[i].sender,
                                smsFilter.mpesaId,
                                smsFilter.phoneNumber,
                                smsFilter.amount,
                                smsFilter.accountNumber,
                                smsFilter.name,
                                incomingMessages[i].date,
                                incomingMessages[i].status
                            )
                        )
                        updateCounter(i)
                    }
                }

            }
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

                    if (!messagesList.isNullOrEmpty() && messagesList[0].status) {
                        smsStatus = if (messageInfo.status) {
                            "Uploaded"
                        } else {
                            "pending"
                        }
                    }
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
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            mSmsReceiver
        )
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
            lateinit var smsManager: SmsManager
            val defaultSim = sharedPreferences.getString(PREF_SIM_CARD, "")
            val localSubscriptionManager = SubscriptionManager.from(activity as Activity)
            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.SEND_SMS
                    )
                }
                == PackageManager.PERMISSION_GRANTED
            ) {


                context?.toast("called $phoneNumber")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (ActivityCompat.checkSelfPermission(
                            activity as Activity,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (localSubscriptionManager.activeSubscriptionInfoCount > 1) {
                            val localList: List<*> =
                                localSubscriptionManager.activeSubscriptionInfoList
                            val simInfo1 = localList[0] as SubscriptionInfo
                            val simInfo2 = localList[1] as SubscriptionInfo

                            smsManager = when (defaultSim) {
                                getString(R.string.sim_card_one) -> {
                                    //SendSMS From SIM One
                                    SmsManager.getSmsManagerForSubscriptionId(simInfo1.subscriptionId)
                                }
                                getString(R.string.sim_card_two) -> {
                                    //SendSMS From SIM Two
                                    SmsManager.getSmsManagerForSubscriptionId(simInfo2.subscriptionId)
                                }
                                else -> {
                                    SmsManager.getDefault()
                                }
                            }
                        } else {
                            smsManager = SmsManager.getDefault()
                        }
                    } else {
                        smsManager = SmsManager.getDefault()
                    }
                } else {
                    smsManager = SmsManager.getDefault()
                }


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


                val arraySendInt = java.util.ArrayList<PendingIntent>()
                arraySendInt.add(sentPI)
                val arrayDelivery = java.util.ArrayList<PendingIntent>()
                arrayDelivery.add(deliveredPI)

                outgoingMessages.add(MessageInfo(phoneNumber, message))

                synchronized(outgoingMessages) {
                    for (`object` in outgoingMessages) {

                        CoroutineScope(IO).launch {
                            try {
                                delay(30000)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                        val element = `object` as MessageInfo
                        val parts = smsManager.divideMessage(element.messageBody)

                        smsManager.sendMultipartTextMessage(
                            element.phoneNumber,
                            null,
                            parts,
                            arraySendInt,
                            arrayDelivery
                        )
                        messageCount++
                        outgoingMessages.remove()
                        Log.d(
                            "teretg",
                            "message count $messageCount queue size ${outgoingMessages.size}"
                        )
                    }
                }
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
            ) != PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                appPermissions
                ,
                PERMISSION_ACCESS_FINE_LOCATION_CODE
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

    private fun checkPhoneStatusPermission() {
        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PERMISSION_READ_PHONE_STATE_CODE
            )
        }


    }


    public fun checkAndRequest():Boolean{

        var listPermissionsNeeded= ArrayList<String>()

        for(perm in appPermissions){

            if(checkSelfPermission( activity as Activity,perm) !=PackageManager.PERMISSION_GRANTED){
                listPermissionsNeeded.add(perm);
            }

        }

        if(!listPermissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(
                activity as Activity,
                listPermissionsNeeded.toArray(arrayOf(listPermissionsNeeded.size.toString()))
                ,PERMISSION_ACCESS_FINE_LOCATION_CODE)
            return false;
        }

        return true;
    }
}