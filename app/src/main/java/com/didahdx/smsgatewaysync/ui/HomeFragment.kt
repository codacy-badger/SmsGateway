package com.didahdx.smsgatewaysync.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.SmsDetailsActivity
import com.didahdx.smsgatewaysync.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import com.didahdx.smsgatewaysync.viewmodels.HomeViewModel
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(), MessageAdapter.OnItemClickListener {

    private var messageList: ArrayList<MessageInfo> = ArrayList<MessageInfo>()

    val filter = IntentFilter(SMS_RECEIVED)

    val appLog = AppLog()
    lateinit var mHomeViewModel: HomeViewModel
    var mMessageAdapter: MessageAdapter? = null
    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
    lateinit var sharedPrferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.recycler_view_message_list.layoutManager = LinearLayoutManager(activity)
        mHomeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
//        mHomeViewModel.getMessages().observe(viewLifecycleOwner, Observer{
//         mMessageAdapter?.notifyDataSetChanged()
//        });
        //registering the broadcast receiver for network

        sharedPrferences = PreferenceManager.getDefaultSharedPreferences(context)
        context?.registerReceiver(
            mConnectionReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        view.text_view_status.text = "$APP_NAME is running"
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
                    messageList.add(
                        MessageInfo(
                            messageText, dateString, phoneNumber, mpesaId,
                            "", "", "", ""
                        )
                    )
                    setUpAdapter()
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
            val isConnected = (activeNetwork != null && activeNetwork.isConnectedOrConnecting)
            when (isConnected) {
                true -> {
                    text_view_status.text = "${getString(R.string.app_name)} is Running"
                    text_view_status?.background =
                        resources.getDrawable(R.drawable.item_background_green)
                }
                false -> {
                    text_view_status.text = "No internet connection"
                    text_view_status?.background =
                        resources.getDrawable(R.drawable.item_background_red)
                }
            }
        }
    }


    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
        super.onDestroyView()
    }

    private fun setUpAdapter() {
        progress_bar?.visibility = View.GONE
        text_loading?.visibility = View.GONE



        mMessageAdapter = MessageAdapter(messageList, this)
        recycler_view_message_list?.adapter = mMessageAdapter
        refresh_layout_home?.isRefreshing = false
    }


    private fun backgroundCoroutineCall() {
        checkSmsPermission()
        startServices()
//        var mqttClient = MqttClient()
//        mqttClient.connect(activity as Activity)
//        mqttClient.publishMessage(activity as Activity, "test")
//        var rabbitmqClient = RabbitmqClient()
//        rabbitmqClient.publishMessage("Test message")


        if (ActivityCompat.checkSelfPermission(activity as Activity, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            view?.refresh_layout_home?.isRefreshing = true
            view?.text_loading?.text = getString(R.string.loading_messages, 0)
            //coroutine background job
            CoroutineScope(IO).launch {
                getDatabaseMessages()
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
            .registerReceiver(
                mConnectionReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
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

        if (cursor != null && cursor.moveToNext()) {
            val nameId = cursor.getColumnIndex("address")
            val messageId = cursor.getColumnIndex("body")
            val dateId = cursor.getColumnIndex("date")
            val mpesaType = sharedPrferences.getString(PREF_MPESA_TYPE, ALL)

            do {
                val dateString = cursor.getString(dateId)

//                if (cursor.getString(nameId).equals("MPESA")) {

                    var mpesaId: String =
                        cursor.getString(messageId).split("\\s".toRegex()).first().trim()
                    if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
                        mpesaId = " "
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

                        else->{
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
        val messageInfo: MessageInfo = messageList[position]
        //Put the value
        val smsDetailsFragment = SmsDetailsFragment()
        val args = Bundle()
        args.putString(SMS_BODY, messageInfo.messageBody)
        args.putString(SMS_DATE, messageInfo.time)
        args.putString(SMS_SENDER, messageInfo.sender)
        smsDetailsFragment.arguments = args

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

        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.FOREGROUND_SERVICE
            )
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