package com.didahdx.smsgatewaysync.ui.home

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.PermissionChecker
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
import androidx.work.*
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.data.network.PostSms
import com.didahdx.smsgatewaysync.data.network.SmsApi
import com.didahdx.smsgatewaysync.databinding.FragmentHomeBinding
import com.didahdx.smsgatewaysync.domain.MessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.manager.RabbitmqClient
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.services.LocationGpsService
import com.didahdx.smsgatewaysync.ui.UiUpdaterInterface
import com.didahdx.smsgatewaysync.ui.activities.LoginActivity
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.work.PrintWorker
import com.didahdx.smsgatewaysync.work.SendRabbitMqWorker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(),
    UiUpdaterInterface {

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
    private val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)

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

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        UiUpdaterInterface = this
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        CoroutineScope(IO).launch {
            rabbitmqClient = RabbitmqClient(UiUpdaterInterface, user?.email!!)
            val urlEnabled = sharedPreferences.getBoolean(PREF_HOST_URL_ENABLED, false)
            val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
            if (isServiceRunning && !urlEnabled) {
                rabbitmqClient.connection(requireContext())
            }
        }

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
        val adapter =
            MessageAdapter(MessageAdapterListener {
                mHomeViewModel.onMessageDetailClicked(it)
            })

        val manager = GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewMessageList.layoutManager = manager
        binding.recyclerViewMessageList.adapter = adapter
        binding.lifecycleOwner = this

        mHomeViewModel.getFilteredData().observe(viewLifecycleOwner, Observer {

        })
        mHomeViewModel.messageList.observe(viewLifecycleOwner, Observer {
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



//        ContextCompat.startForegroundService(requireContext(),LocationIntent)

        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onStart() {
        super.onStart()

        if (checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationIntent = Intent(requireContext(), LocationGpsService::class.java)
            context?.startService(locationIntent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
        notificationManager = NotificationManagerCompat.from(requireContext())
        if (isServiceRunning) {
            startServices()
        }
        checkAndRequestPermissions()
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
        override fun onReceive(context: Context, intent: Intent) {
            try {


                val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
                if (intent != null && isServiceRunning && BATTERY_LOCAL_BROADCAST_RECEIVER == intent.action) {
                    if (intent.extras != null) {
                        val batteryVoltage = intent.extras!!.getString(BATTERY_VOLTAGE_EXTRA)
                        var batteryPercentage =
                            intent.extras!!.getString(BATTERY_PERCENTAGE_EXTRA).toString()
                        val batteryCondition = intent.extras!!.getString(BATTERY_CONDITION_EXTRA)
                        val batteryTemperature =
                            intent.extras!!.getString(BATTERY_TEMPERATURE_EXTRA)
                        val batteryPowerSource =
                            intent.extras!!.getString(BATTERY_POWER_SOURCE_EXTRA)
                        val batteryChargingStatus =
                            intent.extras!!.getString(BATTERY_CHARGING_STATUS_EXTRA)
                        val batteryTechnology = intent.extras!!.getString(BATTERY_TECHNOLOGY_EXTRA)

                        var imei = ""
                        var networkName = ""
                        var simSerialNumber = ""
                        var imsi = ""
                        val telephonyManager = requireContext()
                            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        if (PermissionChecker.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            imei = telephonyManager.deviceId
                            networkName = telephonyManager.networkOperatorName
                            simSerialNumber = telephonyManager.simSerialNumber
                            imsi = telephonyManager.subscriberId

                        }

                        //internal storage
                        val df2 = DecimalFormat("###,###,###,###.00")
                        val stat = StatFs(Environment.getExternalStorageDirectory().path)
                        val bytesAvailable: Long = stat.blockSizeLong * stat.availableBlocksLong
                        val megAvailable: Double =
                            (bytesAvailable.toDouble() / (GIGABYTE).toDouble())
                        val megTotal: Double =
                            (stat.blockSizeLong * stat.blockCountLong).toDouble() / (GIGABYTE).toDouble()

                        //ram space
                        val mi = ActivityManager.MemoryInfo()
                        val activityManager =
                            requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                        activityManager!!.getMemoryInfo(mi)
                        val availableRAM: Double = mi.availMem / (0x100000L).toDouble()
                        val TotalRam: Double = mi.totalMem / (0x100000L).toDouble()

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
                        obj?.put("imei", imei)
                        obj?.put("imsi", imsi)
                        obj?.put("simSerialNumber", simSerialNumber)
                        obj?.put("networkName", networkName)
                        obj?.put("PhoneManufacturer", Build.MANUFACTURER)
                        obj?.put("PhoneModel", Build.MODEL)
                        obj?.put("PhoneBrand", Build.BRAND)
                        obj?.put("TotalStorage", "${df2.format(megTotal)} GB")
                        obj?.put("FreeStorage", "${df2.format(megAvailable)} GB")
                        obj?.put("TotalRam", "${df2.format(TotalRam)} MB")
                        obj?.put("FreeRam", "${df2.format(availableRAM)} MB")
                        obj?.put("client_sender", user?.email!!)
                        obj?.put("date", Date().toString())
                        obj?.put("client_gateway_type", "android_phone")

                        val data = Data.Builder()
                            .putString(KEY_TASK_MESSAGE, obj.toString())
                            .putString(KEY_EMAIL,user?.email)
                            .build()
                        if (context != null) {
                            sendToRabbitMQ(context, data)
                        }

//                        CoroutineScope(IO).launch {
//                            obj?.toString()?.let {
//                                rabbitmqClient.publishMessage(it)
//                            }
//                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                val notification = context?.let {
                    NotificationCompat.Builder(it, CHANNEL_ID)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(input)
                        .setSmallIcon(R.drawable.ic_home)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .build()
                }
                if (notification != null) {
                    notificationManager.notify(1, notification)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        context?.unregisterReceiver(mConnectionReceiver)
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(callReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(batteryReceiver)
//        stopServices()
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
            val appLog = AppLog()
            context?.let { appLog.writeToLog(it, " $status\n") }
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                showDialog("Logout", "Do you want to logout?",
                    "Yes"
                    , DialogInterface.OnClickListener { dialog, _ ->
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
                            userLatitude.toDouble(),
                            userLongitude.toDouble(),
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
                                    userLongitude,
                                    userLatitude
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


    private fun sendToRabbitMQ(context: Context, data: Data) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendRabbitMqWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

}