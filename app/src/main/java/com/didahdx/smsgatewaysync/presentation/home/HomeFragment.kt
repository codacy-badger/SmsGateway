package com.didahdx.smsgatewaysync.presentation.home

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.*
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.*
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.data.network.PostSms
import com.didahdx.smsgatewaysync.data.network.SmsApi
import com.didahdx.smsgatewaysync.databinding.FragmentHomeBinding
import com.didahdx.smsgatewaysync.domain.PhoneStatus
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.services.LocationGpsService
import com.didahdx.smsgatewaysync.presentation.activities.LoginActivity
import com.didahdx.smsgatewaysync.util.*
import com.didahdx.smsgatewaysync.work.WorkerUtil.sendToRabbitMQ
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.metrics.AddTrace
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment() {

    lateinit var mHomeViewModel: HomeViewModel
    var value = ""

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
    var userLongitude: String = " "
    var userLatitude: String = " "
    val user = FirebaseAuth.getInstance().currentUser
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            batteryReceiver,
            IntentFilter(BATTERY_LOCAL_BROADCAST_RECEIVER)
        )

        context?.registerReceiver(statusReceiver, IntentFilter(STATUS_INTENT_BROADCAST_RECEIVER))
        //registering the broadcast receiver for network
        context?.registerReceiver(
            mConnectionReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

    }

    @AddTrace(name = "HomeFragmentOnCreateView")
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


        val email = FirebaseAuth.getInstance().currentUser?.email ?: "email not available "
        context?.let { SpUtil.setPreferenceString(it, PREF_USER_EMAIL, email) }
        val isServiceOn =
            context?.let { SpUtil.getPreferenceBoolean(it, PREF_SERVICES_KEY) } ?: true

        CoroutineScope(IO).launch {
            getConnectionType()
        }
        if (isServiceOn) {
            val color =
                context?.let { SpUtil.getPreferenceString(it, PREF_STATUS_COLOR, RED_COLOR) }
            val status = context?.let {
                SpUtil.getPreferenceString(it, PREF_STATUS_MESSAGE, ERROR_CONNECTING_TO_SERVER)
            } ?: ERROR_CONNECTING_TO_SERVER
            binding.textViewStatus.text = status
//        startServices(status)
            when (color) {
                RED_COLOR -> {
                    binding.textViewStatus.backgroundRed()
                    binding.textViewConnectionType.backgroundRed()
                    binding.linearStatus.backgroundRed()
                }

                GREEN_COLOR -> {
                    binding.textViewStatus.backgroundGreen()
                    binding.textViewConnectionType.backgroundGreen()
                    binding.linearStatus.backgroundGreen()
                }

            }
        } else {
            binding.textViewStatus.backgroundGrey()
            binding.textViewConnectionType.backgroundGrey()
            binding.linearStatus.backgroundGrey()
            binding.textViewStatus.text = "$APP_NAME is disabled"
        }


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

    @AddTrace(name = "HomeFragmentOnViewCreated")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isServiceOn =
            context?.let { SpUtil.getPreferenceBoolean(it, PREF_SERVICES_KEY) } ?: true
        notificationManager = NotificationManagerCompat.from(requireContext())

        if (isServiceOn && ServiceState.STOPPED == context?.let { getServiceState(it) }) {
            startServices()
            context?.toast(" Service started home ${context?.let { getServiceState(it) }}")
        }

        checkAndRequestPermissions()
        navController = Navigation.findNavController(view)
    }


    //appServices for showing notification bar
    private fun startServices() {
        val status = context?.let {
            SpUtil.getPreferenceString(it, PREF_STATUS_MESSAGE, ERROR_CONNECTING_TO_SERVER)
        } ?: ERROR_CONNECTING_TO_SERVER
        val serviceIntent = Intent(requireContext(), AppServices::class.java)
        serviceIntent.action = AppServiceActions.START.name
        serviceIntent.putExtra(INPUT_EXTRAS, status)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }

    private fun stopServices() {
        context?.let { setRestartServiceState(it, false) }
        val serviceIntent = Intent(requireContext(), AppServices::class.java)
        serviceIntent.action = AppServiceActions.STOP.name
        ContextCompat.startForegroundService(activity as Activity, serviceIntent)
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {

                val isServiceOn =
                    context.let { SpUtil.getPreferenceBoolean(context, PREF_SERVICES_KEY) }
                if (isServiceOn && BATTERY_LOCAL_BROADCAST_RECEIVER == intent.action) {
                    if (intent.extras != null) {
                        val batteryVoltage =
                            intent.extras!!.getString(BATTERY_VOLTAGE_EXTRA) ?: NOT_AVAILABLE
                        val batteryPercentage =
                            intent.extras!!.getString(BATTERY_PERCENTAGE_EXTRA).toString()
                                ?: NOT_AVAILABLE
                        val batteryCondition =
                            intent.extras!!.getString(BATTERY_CONDITION_EXTRA) ?: NOT_AVAILABLE
                        val batteryTemperature =
                            intent.extras!!.getString(BATTERY_TEMPERATURE_EXTRA) ?: NOT_AVAILABLE
                        val batteryPowerSource =
                            intent.extras!!.getString(BATTERY_POWER_SOURCE_EXTRA) ?: NOT_AVAILABLE
                        val batteryChargingStatus =
                            intent.extras!!.getString(BATTERY_CHARGING_STATUS_EXTRA)
                                ?: NOT_AVAILABLE
                        val batteryTechnology =
                            intent.extras!!.getString(BATTERY_TECHNOLOGY_EXTRA) ?: NOT_AVAILABLE

                        var imei = ""
                        var networkName = ""
                        var simSerialNumber = ""
                        var imsi = ""
                        val telephonyManager = requireContext()
                            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        if (checkSelfPermission(
                                requireContext(),
                                Manifest.permission.READ_PHONE_STATE
                            ) == PermissionChecker.PERMISSION_GRANTED
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

                        val phoneStatus = PhoneStatus(
                            type = "phoneStatus",
                            batteryPercentage = batteryPercentage,
                            batteryCondition = batteryCondition,
                            batteryTemperature = batteryTemperature,
                            batteryPowerSource = batteryPowerSource,
                            batteryChargingStatus = batteryChargingStatus,
                            batteryTechnology = batteryTechnology,
                            batteryVoltage = batteryVoltage,
                            longitude = userLongitude,
                            latitude = userLatitude,
                            imei = imei,
                            imsi = imsi,
                            simSerialNumber = simSerialNumber,
                            networkName = networkName,
                            PhoneManufacturer = Build.MANUFACTURER,
                            PhoneModel = Build.MODEL,
                            PhoneBrand = Build.BRAND,
                            TotalStorage = "${df2.format(megTotal)} GB",
                            FreeStorage = "${df2.format(megAvailable)} GB",
                            TotalRam = "${df2.format(TotalRam)} MB",
                            FreeRam = "${df2.format(availableRAM)} MB",
                            client_sender = user?.email!!,
                            date = Date().toString(),
                            client_gateway_type = ANDROID_PHONE,
                            totalBundles = "",
                            sentBundles = "",
                            receivedBundles = "",
                            airtimeBalance = " "
                        )

                        val gson = Gson()


                        val data = Data.Builder()
                            .putString(KEY_TASK_MESSAGE, gson.toJson(phoneStatus))
                            .putString(KEY_EMAIL, user?.email)
                            .build()
                        if (context != null) {
                            sendToRabbitMQ(context, data)
                        }

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
            var isWifiConn: Boolean = false
            var isMobileConn: Boolean = false
            connectionManager.allNetworks.forEach { network ->
                connectionManager.getNetworkInfo(network)?.apply {
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            isWifiConn = isWifiConn or isConnected
                            text_view_connection_type?.text = "Connected to Wifi"
                        }
                        ConnectivityManager.TYPE_MOBILE -> {
                            text_view_connection_type?.text = "Connected to Mobile data"
                            context.toast(" Connected to Mobile data")
                            isMobileConn = isMobileConn or isConnected
                        }
                    }
                    val activeNetwork = connectionManager.activeNetworkInfo
                    if ((activeNetwork != null && activeNetwork.isConnectedOrConnecting)) {
                        CoroutineScope(IO).launch {
                            postMessage()
                        }
                    } else {
                        text_view_connection_type?.text = "Connection lost"
                    }
                }
            }


        }
    }


    //broadcast connection receiver
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {

            var color = SpUtil.getPreferenceString(context, PREF_STATUS_COLOR, RED_COLOR)
            var status = SpUtil.getPreferenceString(
                context, PREF_STATUS_MESSAGE,
                ERROR_CONNECTING_TO_SERVER
            )

            if (STATUS_INTENT_BROADCAST_RECEIVER == intent?.action) {
                if (null != intent?.extras) {
                    status = intent.extras?.getString(STATUS_MESSAGE_EXTRA) ?: status
                    color = intent.extras?.getString(STATUS_COLOR_EXTRA) ?: color
                }


                text_view_status?.text = status
                when (color) {
                    RED_COLOR -> {
                        text_view_status?.backgroundRed()
                        text_view_connection_type?.backgroundRed()
                        linear_status?.backgroundRed()
                    }
                    GREEN_COLOR -> {
                        text_view_status?.backgroundGreen()
                        text_view_connection_type?.backgroundGreen()
                        linear_status?.backgroundGreen()
                    }
                }

            }
        }
    }


    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }


    override fun onDestroy() {
        super.onDestroy()
        context?.unregisterReceiver(mConnectionReceiver)
        context?.unregisterReceiver(statusReceiver)
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(callReceiver)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(batteryReceiver)
//        stopServices()
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
                        context?.showDialog("", "This app needs $permName to work properly",
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
                        context?.showDialog("",
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                context?.showDialog("Logout", "Do you want to logout?",
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
        val urlEnabled =
            context?.let { SpUtil.getPreferenceBoolean(it, PREF_HOST_URL_ENABLED) } ?: false
        val url = context?.let { SpUtil.getPreferenceString(it, PREF_HOST_URL, NOT_AVAILABLE) }
        val database = MessagesDatabase(requireContext()).getIncomingMessageDao()
        if (urlEnabled && !url.isNullOrEmpty()) {
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
                            Conversion.getFormattedDate(Date(message.date)),
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


    suspend fun getConnectionType() {
        val connectionManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var connectionType = ""
        var isConnected = false
        var isWifiConn = false
        var isMobileConn = false
        connectionManager.allNetworks.forEach { network ->
            connectionManager.getNetworkInfo(network)?.apply {
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> {
                        isWifiConn = isWifiConn or isConnected
                        connectionType = "Connected to Wifi"
                    }

                    ConnectivityManager.TYPE_MOBILE -> {
                        isMobileConn = isMobileConn or isConnected
                        connectionType = "Connected to Mobile Data"
                    }
                }
                val activeNetwork = connectionManager.activeNetworkInfo
                if (!(activeNetwork != null && activeNetwork.isConnectedOrConnecting)) {

                    connectionType = "Connection lost"
                }
            }
        }

        CoroutineScope(Main).launch {
            text_view_connection_type.text = connectionType
        }
    }

}