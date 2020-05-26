package com.didahdx.smsgatewaysync.ui.home

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.data.network.PostSms
import com.didahdx.smsgatewaysync.data.network.SmsApi
import com.didahdx.smsgatewaysync.databinding.FragmentHomeBinding
import com.didahdx.smsgatewaysync.manager.RabbitmqClient
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.model.SmsInboxInfo
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.services.LocationGpsService
import com.didahdx.smsgatewaysync.ui.UiUpdaterInterface
import com.didahdx.smsgatewaysync.ui.activities.LoginActivity
import com.didahdx.smsgatewaysync.ui.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.ui.adapters.MessageAdapterListener
import com.didahdx.smsgatewaysync.utilities.*
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(),
    UiUpdaterInterface {

    @Volatile
    var isConnected = false
    val appLog = AppLog()
    lateinit var mHomeViewModel: HomeViewModel
    private lateinit var sharedPreferences: SharedPreferences

    var bluetoothAdapter: BluetoothAdapter? = null
    var socket: BluetoothSocket? = null
    var bluetoothDevice: BluetoothDevice? = null
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null
    var workerThread: Thread? = null
    lateinit var readBuffer: ByteArray
    var readBufferPosition = 0

    @Volatile
    var stopWorker = false
    var value = ""
    val sdf = SimpleDateFormat(DATE_FORMAT)

    private val appPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.MANAGE_OWN_CALLS,
        Manifest.permission.ANSWER_PHONE_CALLS
    )


    lateinit var notificationManager: NotificationManagerCompat
    var notificationCounter = 3000
    var importantSmsNotification = 2

    @Volatile
    lateinit var rabbitmqClient: RabbitmqClient
    private var locationBroadcastReceiver: BroadcastReceiver? = null
    var userLongitude: String = " "
    var userLatitude: String = " "
    val user = FirebaseAuth.getInstance().currentUser
    var UiUpdaterInterface: UiUpdaterInterface? = null
    var outgoingMessages: Queue<MessageInfo> = LinkedList()
    var messageCount = 0
    var lastMessageSentTime = 0
    private val UPDATE_INTERVAL = 500 * 20 // 5 seconds*20
    private lateinit var locationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var currentLocation: Location
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        UiUpdaterInterface = this
        Timber.i("isConnected $isConnected")
        CoroutineScope(IO).launch {
            rabbitmqClient = RabbitmqClient(UiUpdaterInterface, user?.email!!)
            Timber.i(" object ${rabbitmqClient.hashCode()}")

            Timber.i("isConnected $isConnected")
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val urlEnabled = sharedPreferences.getBoolean(PREF_HOST_URL_ENABLED, false)
            val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
            if (!isConnected && isServiceRunning && !urlEnabled) {
                rabbitmqClient.connection(requireContext())
            }
        }

        locationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = UPDATE_INTERVAL.toLong()
        locationCallback = object : LocationCallback() {
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                super.onLocationAvailability(locationAvailability)
                if (locationAvailability.isLocationAvailable) {
                    Timber.i("Location is available")
                } else {
                    Timber.i("Location is unavailable")
                }
            }

            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Timber.i("Location result is available")
            }
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mSmsReceiver, IntentFilter(SMS_LOCAL_BROADCAST_RECEIVER)
        )

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            callReceiver, IntentFilter(CALL_LOCAL_BROADCAST_RECEIVER)
        )

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            batteryReceiver,
            IntentFilter(BATTERY_LOCAL_BROADCAST_RECEIVER)
        )

        //registering the broadcast receiver for network
        context?.registerReceiver(
            mConnectionReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentHomeBinding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_home, container, false)
        val application = requireNotNull(this.activity).application
        val database = MessagesDatabase(application).getIncomingMessageDao()
        val factory = HomeViewModelFactory(database, application)
        mHomeViewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)

        binding.homeViewModel = mHomeViewModel
        val adapter = MessageAdapter(MessageAdapterListener {
            mHomeViewModel.onMessageDetailClicked(it)
        })

        val manager = GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewMessageList.layoutManager = manager
        binding.recyclerViewMessageList.adapter = adapter
        binding.lifecycleOwner = this

        mHomeViewModel.filteredMessages.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.progressBar.hide()
                binding.textLoading.hide()
                binding.refreshLayoutHome.isRefreshing = false
                adapter.submitList(it)
//                used to
//                (binding.recyclerViewMessageList.layoutManager as GridLayoutManager).scrollToPositionWithOffset(0, 0)
            }
        })

        //navigating to sms detail screen
        mHomeViewModel.eventMessageClicked.observe(viewLifecycleOwner, Observer {
            it?.let {
                val bundle = bundleOf("SmsInfo" to mHomeViewModel.setUpSmsInfo(it))
                this.findNavController()
                    .navigate(R.id.action_homeFragment_to_smsDetailsFragment, bundle)
                mHomeViewModel.onMessageDetailNavigated()
            }
        })

        mHomeViewModel.messageCount.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.textLoading.text = getString(R.string.loading_messages, it)
            }
        })

        binding.refreshLayoutHome.setOnRefreshListener {
            binding.refreshLayoutHome.isRefreshing = true
            mHomeViewModel.refreshIncomingDatabase()
            binding.refreshLayoutHome.isRefreshing = false
        }

        val intent = Intent(requireContext(), LocationGpsService::class.java)
        context?.startService(intent)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
        notificationManager = NotificationManagerCompat.from(requireContext())
        if (isServiceRunning) {
            startServices()
            text_view_status?.backgroundGreen()
            text_view_status?.text = "$APP_NAME is running"
        } else {
            text_view_status?.text = "$APP_NAME is stopped"
            text_view_status?.backgroundRed()
        }

        checkAndRequestPermissions()
        startGettingLocation()
        navController = Navigation.findNavController(view)
    }


    //appServices for showing notification bar
    private fun startServices() {
        val serviceIntent = Intent(requireContext(), AppServices::class.java)
        serviceIntent.putExtra(INPUT_EXTRAS, "$APP_NAME is running")
        ContextCompat.startForegroundService(requireContext(), serviceIntent)

    }

    private fun stopServices() {
        val serviceIntent = Intent(requireContext(), AppServices::class.java)
        requireContext().stopService(serviceIntent)
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
            if (intent != null && isServiceRunning && BATTERY_LOCAL_BROADCAST_RECEIVER == intent.action) {
                if (intent.extras != null) {
                    val batteryVoltage = intent.extras!!.getString(BATTERY_VOLTAGE_EXTRA)
                    var batteryPercentage =
                        intent.extras!!.getString(BATTERY_PERCENTAGE_EXTRA).toString()
                    val batteryCondition = intent.extras!!.getString(BATTERY_CONDITION_EXTRA)
                    val batteryTemperature = intent.extras!!.getString(BATTERY_TEMPERATURE_EXTRA)
                    val batteryPowerSource = intent.extras!!.getString(BATTERY_POWER_SOURCE_EXTRA)
                    val batteryChargingStatus =
                        intent.extras!!.getString(BATTERY_CHARGING_STATUS_EXTRA)
                    val batteryTechnology = intent.extras!!.getString(BATTERY_TECHNOLOGY_EXTRA)
                    startGettingLocation()

                    val obj: JSONObject? = JSONObject()
                    obj?.put("type", "phoneStatus")
                    obj?.put("batteryPercentage", batteryPercentage)
                    obj?.put("batteryCondition", batteryCondition)
                    obj?.put("batteryTemperature", batteryTemperature)
                    obj?.put("batteryPowerSource", batteryPowerSource)
                    obj?.put("batteryChargingStatus", batteryChargingStatus)
                    obj?.put("batteryTechnology", batteryTechnology)
                    obj?.put("batteryVoltage", batteryVoltage)
                    obj?.put("longitude", userLongitude)
                    obj?.put("latitude", userLatitude)
                    obj?.put("client_sender", user?.email!!)
                    obj?.put("date", Date().toString())
                    obj?.put("client_gateway_type", "android_phone")

                    CoroutineScope(IO).launch {
                        obj?.toString()?.let {
                            rabbitmqClient.publishMessage(it)
                        }
                    }
                }
            }
        }
    }


    private val callReceiver = object : BroadcastReceiver() {
        //broadcast call receiver
        override fun onReceive(context: Context?, intent: Intent?) {
            var phoneNumber: String? = " "
            val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
            if (intent != null && isServiceRunning && CALL_LOCAL_BROADCAST_RECEIVER == intent.action) {
                if (intent.extras != null) {
                    phoneNumber = intent.extras!!.getString(PHONE_NUMBER_EXTRA)
                    val callType = intent.extras!!.getString(CALL_TYPE_EXTRA)
                    val startTime = intent.extras!!.getString(START_TIME_EXTRA)
                    val endTime = intent.extras!!.getString(END_TIME_EXTRA)
                    startGettingLocation()
                    val obj: JSONObject? = JSONObject()
                    obj?.put("type", "calls")
                    obj?.put("longitude", userLongitude)
                    obj?.put("latitude", userLatitude)
                    obj?.put("client_sender", user?.email!!)
                    obj?.put("client_gateway_type", "android_phone")
                    obj?.put("call_type", callType)
                    obj?.put("phone_number", phoneNumber)
                    obj?.put("start_time", startTime)
                    obj?.put("end_time", endTime)

                    CoroutineScope(IO).launch {
                        obj?.toString()?.let {
                            rabbitmqClient.publishMessage(it)
                        }
                    }
                }
            }
            if (phoneNumber != null) {
                mHomeViewModel?.hangUpCall()
            }

        }
    }

    //broadcast sms receiver
    private val mSmsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
            if (intent != null && isServiceRunning && SMS_LOCAL_BROADCAST_RECEIVER == intent.action) {
                Timber.i("action local ${intent.action}")
                if (intent.extras != null) {
                    val phoneNumber = intent.extras!!.getString("phoneNumber")
                    val dateTimeStamp = intent.extras!!.getLong("date")
                    val messageText = intent.extras!!.getString("messageText")
                    val date = Date(dateTimeStamp).toString()

                    startGettingLocation()
                    val maskedPhoneNumber = sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)
                    val printType = sharedPreferences.getString(PREF_PRINT_TYPE, "")
                    val obj: JSONObject? = JSONObject()
                    val smsFilter = messageText?.let { SmsFilter(it, maskedPhoneNumber) }
                    val printMessage =
                        smsFilter?.checkSmsType(messageText.trim(), maskedPhoneNumber)
                    val importantSmsType = sharedPreferences.getString(PREF_IMPORTANT_SMS_NOTIFICATION, " ")

                    if (messageText != null && phoneNumber != null &&
                       ( smsFilter?.mpesaType == importantSmsType || importantSmsType=="All")) {
                        Timber.i(" $messageText \n $phoneNumber ")
                        notificationMessage(messageText, phoneNumber)
                        requireContext().toast("test $importantSmsType $messageText $phoneNumber")
                    }

                    if (printMessage != null && smsFilter?.mpesaType == printType ) {
                        CoroutineScope(IO).launch{
                            intentPrint(printMessage)
                        }
                    }


                    obj?.put("type", "message")
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
                        } else if (smsFilter.date != NOT_AVAILABLE) {
                            obj?.put("transaction_date", smsFilter.date)
                        } else if (smsFilter.time !=
                            NOT_AVAILABLE
                        ) {
                            obj?.put("transaction_date", smsFilter.time)
                        }
                        obj?.put("amount", smsFilter?.amount)

                    } else {
                        obj?.put("message_type", "recieved_sms")
                    }



                    CoroutineScope(IO).launch {
                        obj?.toString()?.let {
                            var status = false
                            val urlEnabled =
                                sharedPreferences.getBoolean(PREF_HOST_URL_ENABLED, false)
                            if (!urlEnabled) {
                                rabbitmqClient.publishMessage(it)
                                status = true
                            }

                            val sdf = SimpleDateFormat(DATE_FORMAT)
                            val message2: MpesaMessageInfo?

                            if (messageText != null && dateTimeStamp != null && phoneNumber != null) {
                                val maskedPhoneNumber =
                                    sharedPreferences.getBoolean(PREF_MASKED_NUMBER, false)
                                val smsFilter = SmsFilter(messageText, maskedPhoneNumber)

                                withContext(Main) {
                                    Timber.i("Otp code ${smsFilter.otpCode} website ${smsFilter.otpWebsite}")
                                    println("Otp code ${smsFilter.otpCode} website ${smsFilter.otpWebsite}")

                                    val smspost = SmsInboxInfo(
                                        0, messageText.trim(),
                                        sdf.format(Date(dateTimeStamp)).toString(),
                                        phoneNumber,
                                        smsFilter.mpesaId,
                                        smsFilter.phoneNumber,
                                        smsFilter.amount,
                                        smsFilter.accountNumber,
                                        smsFilter.name,
                                        dateTimeStamp,
                                        true, userLongitude, userLatitude
                                    )
                                    postMessage(smspost)
                                }

                                message2 = MpesaMessageInfo(
                                    messageText.trim(),
                                    sdf.format(Date(dateTimeStamp)).toString(),
                                    phoneNumber,
                                    smsFilter.mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    dateTimeStamp,
                                    status, userLongitude, userLatitude
                                )

                                context.let { tex ->
                                    MessagesDatabase(tex).getIncomingMessageDao()
                                        .addMessage(message2)
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
                    CoroutineScope(IO).launch {
                        postMessage()
                    }
                }
                false -> {

                }
            }
        }
    }


    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
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
        context?.unregisterReceiver(mConnectionReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(callReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(batteryReceiver)
//        stopServices()
    }

    //used to check if the app has connected
    override fun isConnected(value: Boolean) {
        CoroutineScope(Main).launch {
            isConnected = value
        }
    }

    //used to show to notification
    override fun notificationMessage(message: String) {
        CoroutineScope(Main).launch {
            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID_2)
                .setContentTitle("Notification Message")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_home)
                .build()

            notificationManager.notify(notificationCounter, notification)
            notificationCounter++
        }
    }

    //used to show to notification
    fun notificationMessage(message: String, phoneNumber: String) {
        CoroutineScope(Main).launch {
            requireContext().toast("notification call")
            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID_3)
                .setContentTitle(phoneNumber)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_message)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(message))
                .build()

            notificationManager.notify(importantSmsNotification, notification)
            importantSmsNotification++
        }
    }


    //used to show toast messages
    override fun toasterMessage(message: String) {
        Timber.d("called $message")
        Timber.d("thread name ${Thread.currentThread().name}")
        CoroutineScope(Main).launch {
            context?.toast(message)
            Timber.d("thread name ${Thread.currentThread().name}")
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
            val localSubscriptionManager = SubscriptionManager.from(requireContext())
            if (context?.let {
                    checkSelfPermission(
                        it,
                        Manifest.permission.SEND_SMS
                    )
                }
                == PackageManager.PERMISSION_GRANTED
            ) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (checkSelfPermission(
                            requireContext(),
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

                for (`object` in outgoingMessages) {
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

                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        context?.registerReceiver(locationBroadcastReceiver, IntentFilter(LOCATION_UPDATE_INTENT))
        if (locationBroadcastReceiver == null) {
            locationBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (LOCATION_UPDATE_INTENT == intent.action) {
                        val longitude = intent.getStringExtra(LONGITUDE_EXTRA)
                        val latitude = intent.getStringExtra(LATITUDE_EXTRA)
                        val altitude = intent.getStringExtra(ALTITUDE_EXTRA)
                        userLatitude = latitude
                        userLongitude = longitude

                        context.toast("$latitude ${intent.getStringExtra(LONGITUDE_EXTRA)}")

                    }
                }
            }
        }

    }

    //check and requests the permission which are required
    private fun checkAndRequestPermissions(): Boolean {
        val listPermissionsNeeded = ArrayList<String>()

        for (perm in appPermissions) {
            if (checkSelfPermission(
                    requireContext(),
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(perm);
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
            for (i in grantResults.indices) {
                if (i >= 0 && grantResults.isNotEmpty() && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults[permissions[i]] = grantResults[i]
                    deniedCount++
                }
            }

            if (deniedCount == 0) {
                //initialise app
            } else {
                for (entry in permissionResults) {
                    var permName = entry.key.toString()
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

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setCancelable(isCancelable)
        builder.setMessage(msg)
        builder.setPositiveButton(postiveLabel, postiveOnClick)
        builder.setNegativeButton(negativeLabel, negativeOnClick)
        val alert = builder.create()
        alert.show()
        return alert;
    }

    //getting location of user using location api
    private fun startGettingLocation() {
        if (checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationProviderClient?.requestLocationUpdates(
                locationRequest,
                locationCallback,
                activity?.mainLooper
            )
            locationProviderClient?.lastLocation
                ?.addOnSuccessListener(OnSuccessListener<Location> { location ->
                    location?.let {
                        currentLocation = location
                        userLongitude = currentLocation.longitude.toString()
                        userLatitude = currentLocation.latitude.toString()

//                    context?.toast(" long $userLongitude  lati $userLatitude")
                        Timber.i(" long $userLongitude  lati $userLatitude")
                    }
                })

            locationProviderClient?.lastLocation
                ?.addOnFailureListener(OnFailureListener { e ->
                    Timber.i("Exception while getting the location: ${e.message}")
                })
        } else {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
//                context?.toast("Permission needed")

            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    PERMISSION_ACCESS_FINE_LOCATION_CODE
                )
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                showDialog("Logout", "Do you want to logout?",
                    "Yes"
                    , DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        stopServices()
                        requireActivity().onBackPressed()

                    },
                    "No", DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    }
                    , false)

            }
            R.id.forwardPending -> {
                CoroutineScope(IO).launch {
                    postMessage()
                }
            }

        }
        return NavigationUI.onNavDestinationSelected(
            item,
            navController
        ) || super.onOptionsItemSelected(item)
    }


   suspend private fun InitPrinter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() as BluetoothAdapter
        try {
            if (!bluetoothAdapter?.isEnabled!!) {
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetooth, 0)
            }
            val printerName = sharedPreferences.getString(PREF_PRINTER_NAME, "")
            if (!(printerName != null && printerName.isNotEmpty())) {
                return
            }
            val pairedDevices = bluetoothAdapter?.bondedDevices
            if (pairedDevices?.size!! > 0) {
                for (device in pairedDevices) {
                    if (device.name == printerName) //Note, you will need to change this to match the name of your device
                    {
                        bluetoothDevice = device
                        break
                    }
                }
                val uuid =
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //Standard SerialPortService ID
                val m: Method = bluetoothDevice!!.javaClass.getMethod(
                    "createRfcommSocket", *arrayOf<Class<*>?>(
                        Int::class.javaPrimitiveType
                    )
                )
                socket = m?.invoke(bluetoothDevice, 1) as BluetoothSocket?
                bluetoothAdapter?.cancelDiscovery()
                socket?.connect()
                outputStream = socket?.outputStream
                inputStream = socket?.inputStream
                beginListenForData()
            } else {
                value = "No Devices found"
                CoroutineScope(Main).launch{
                    requireContext().toast(value)
                }
                return
            }
        } catch (ex: java.lang.Exception) {
            value = "$ex\n InitPrinter \n"
            CoroutineScope(Main).launch{
                requireContext().toast(value)
            }
        }

    }


   suspend private fun beginListenForData() {
        try {
            val handler = Handler()

            // this is the ASCII code for a newline character
            val delimiter: Byte = 10
            stopWorker = false
            readBufferPosition = 0
            readBuffer = ByteArray(1024)
            workerThread = Thread(Runnable {
                while (!Thread.currentThread().isInterrupted && !stopWorker) {
                    try {
                        val bytesAvailable = inputStream?.available()
                        if (bytesAvailable != null && bytesAvailable > 0) {
                            val packetBytes = ByteArray(bytesAvailable)
                            inputStream!!.read(packetBytes)
                            for (i in 0 until bytesAvailable) {
                                val b = packetBytes[i]
                                if (b == delimiter) {
                                    val encodedBytes = ByteArray(readBufferPosition)
                                    System.arraycopy(
                                        readBuffer, 0,
                                        encodedBytes, 0,
                                        encodedBytes.size
                                    )

                                    // specify US-ASCII encoding
                                    val data = String(encodedBytes, Charsets.US_ASCII)
                                    readBufferPosition = 0

                                    // tell the user data were sent to bluetooth printer device
                                    handler.post(Runnable { Timber.d(data) })
                                } else {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        stopWorker = true
                    }
                }
            })
            workerThread?.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


   suspend fun intentPrint(messageBody: String) {
        val buffer: ByteArray = messageBody.toByteArray()
        val printHeader = byteArrayOf(0xAA.toByte(), 0x55, 2, 0)
        printHeader[3] = buffer.size.toByte()
        InitPrinter()
        if (printHeader.size > 128) {
            value = "\nValue is more than 128 size\n"
            CoroutineScope(Main).launch{
                requireContext().toast(value)
            }
        } else {
            try {
                outputStream?.write(messageBody.toByteArray())
                outputStream?.close()
                socket?.close()
            } catch (ex: java.lang.Exception) {
                value = "$ex\nExcep IntentPrint \n"
                CoroutineScope(Main).launch{
                    requireContext().toast(value)
                }
            }
        }
    }


    suspend fun postMessage() {
        val urlEnabled = sharedPreferences.getBoolean(PREF_HOST_URL_ENABLED, false)
        val url = sharedPreferences.getString(PREF_HOST_URL, " ")
        val database = MessagesDatabase(requireContext()).getIncomingMessageDao()
        if (urlEnabled && url != null) {
            val pendingMessages = database.getPeddingMessages(false)
            try {
                for (me in pendingMessages.indices) {
                    val message = pendingMessages[me]
                    val smsFilter = SmsFilter(message.messageBody, false)
                    val amount = message.amount.replace("Ksh", "").replace(",", "")

                    if (smsFilter.mpesaType != NOT_AVAILABLE) {

                        val post = PostSms(
                            message.accountNumber,
                            amount.toDouble(),
                            message.latitude.toDouble(),
                            message.longitude.toDouble(),
                            message.messageBody,
                            message.name,
                            message.receiver,
                            " ${smsFilter.date}  ${smsFilter.time}",
                            message.sender,
                            sdf.format(Date(message.date)).toString(),
                            smsFilter.mpesaType,
                            message.mpesaId
                        )

                        val postSms: Call<PostSms> = SmsApi.retrofitService.postSms(url, post)
                        postSms.enqueue(object : Callback<PostSms> {
                            override fun onFailure(call: Call<PostSms>, t: Throwable) {
                                Timber.i("Localised message ${t.localizedMessage}")
                            }

                            override fun onResponse(
                                call: Call<PostSms>,
                                response: Response<PostSms>
                            ) {
                                if (!response.isSuccessful) {
                                    Timber.i("Code: %s", response.code());
                                    return;
                                }

                                Timber.i("Code: ${response.code()} call ${call.toString()} ");

                                val updateMessage = MpesaMessageInfo(
                                    message.messageBody,
                                    message.time,
                                    message.sender,
                                    message.mpesaId,
                                    message.receiver,
                                    message.amount,
                                    message.accountNumber,
                                    message.name,
                                    message.date,
                                    true,
                                    message.longitude,
                                    message.latitude
                                )

                                updateMessage.id = message.id

                                CoroutineScope(IO).launch {
                                    database.updateMessage(updateMessage)
                                }
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun postMessage(smsInboxInfo: SmsInboxInfo) {
        CoroutineScope(IO).launch {
            val url = sharedPreferences.getString(PREF_HOST_URL, " ")
            val urlEnabled = sharedPreferences.getBoolean(PREF_HOST_URL_ENABLED, false)
            try {
                val amount = smsInboxInfo.amount.replace("Ksh", "").replace(",", "")
                Timber.i(amount)
                val smsFilter = SmsFilter(smsInboxInfo.messageBody, false)
                if (smsFilter.mpesaType != NOT_AVAILABLE && urlEnabled && url != null) {

                    val post = PostSms(
                        smsInboxInfo.accountNumber,
                        amount.toDouble(),
                        smsInboxInfo.latitude.toDouble(),
                        smsInboxInfo.longitude.toDouble(),
                        smsInboxInfo.messageBody,
                        smsInboxInfo.name,
                        smsInboxInfo.receiver,
                        " ${smsFilter.date}  ${smsFilter.time}",
                        smsInboxInfo.sender,
                        sdf.format(Date(smsInboxInfo.date)).toString(),
                        smsFilter.mpesaType,
                        smsInboxInfo.mpesaId
                    )

                    println(post.toString())

                    val postSms: Call<PostSms> = SmsApi.retrofitService.postSms(url, post)
                    postSms.enqueue(object : Callback<PostSms> {
                        override fun onFailure(call: Call<PostSms>, t: Throwable) {
                            Timber.i("Localised message ${t.localizedMessage}")
                        }

                        override fun onResponse(call: Call<PostSms>, response: Response<PostSms>) {
                            if (!response.isSuccessful) {
                                Timber.i("Code: %s", response.code());
                                return;
                            }

                            Timber.i("Code: ${response.code()} call ${call.toString()} ");
                        }

                    })
                }
            } catch (e: Exception) {
                println("post error message ${e.localizedMessage}")
                Timber.i("post error message ${e.localizedMessage}")
            }
        }
    }

}