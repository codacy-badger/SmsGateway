package com.didahdx.smsgatewaysync.ui.smsDetails

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.android.synthetic.main.fragment_sms_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [SmsDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SmsDetailsFragment : Fragment(R.layout.fragment_sms_details) {

    var smsBody: String? = " "
    var smsDate: String? = " "
    var smsSender: String? = " "
    var smsStatus: String? = " "
    var longitude: String? = " "
    var latitude: String? = " "
    lateinit var sharedPrferences: SharedPreferences
    private var SmsInfo: SmsInfo? = null
    var value = ""
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        SmsInfo = arguments?.getParcelable<SmsInfo>("SmsInfo")
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPrferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        //Retrieve the value
        smsBody = SmsInfo?.messageBody
        smsDate = SmsInfo?.time
        smsSender = SmsInfo?.sender
        smsStatus = SmsInfo?.status
        longitude = SmsInfo?.longitude
        latitude = SmsInfo?.latitude


        text_view_sender_no_act.text = smsSender
        text_view_message_body_act.text = smsBody
        text_view_receipt_date_act.text = smsDate
        text_view_status_check_act.text = smsStatus
        text_view_longitude_act.text = longitude
        text_view_latitude_act.text = latitude
        if (smsBody != null) {
            val maskedPhoneNumber = sharedPrferences.getBoolean(PREF_MASKED_NUMBER, false)
            val smsFilter = SmsFilter(smsBody!!, maskedPhoneNumber)
            text_view_voucher_no_act.text = smsFilter.mpesaId
            text_view_transaction_date_act.text = "${smsFilter.date}  ${smsFilter.time}"
            text_view_name_act.text = smsFilter.name
            text_view_phone_number_act.text = smsFilter.phoneNumber
            text_view_amount_act.text = smsFilter.amount
            text_view_account_no_act.text = smsFilter.accountNumber
        }
        //when the SMS has been sent
        context?.registerReceiver(smsSent, IntentFilter(SMS_SENT_INTENT))

        //when the SMS has been delivered
        context?.registerReceiver(smsDelivered, IntentFilter(SMS_DELIVERED_INTENT))
    }


    private val smsSent = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context?, arg1: Intent?) {
            when (resultCode) {
                Activity.RESULT_OK -> context?.toast("SMS sent")
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> context?.toast("Generic failure")
                SmsManager.RESULT_ERROR_NO_SERVICE -> context?.toast("No service")
                SmsManager.RESULT_ERROR_NULL_PDU -> context?.toast("Null PDU")
                SmsManager.RESULT_ERROR_RADIO_OFF -> context?.toast("Radio off")
            }
        }
    }

    private val smsDelivered = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context?, arg1: Intent?) {
            when (resultCode) {
                Activity.RESULT_OK -> context?.toast("SMS delivered")
                Activity.RESULT_CANCELED -> context?.toast("SMS not delivered")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.sms_details_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_print -> {
                print()
            }
            R.id.action_forward_sms -> {
//                forwardSms()
                shareMessage()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun forwardSms() {
        checkSendSmsPermission()
        checkPhoneStatusPermission()
        lateinit var smsManager: SmsManager
        val defaultSim = sharedPrferences.getString(PREF_SIM_CARD, "")
        val localSubscriptionManager = SubscriptionManager.from(requireContext())
        val phoneNumber = sharedPrferences.getString(PREF_PHONE_NUMBER, "")
        val usePhoneNumber = sharedPrferences.getBoolean(PREF_ENABLE_FORWARD_SMS, false)
        if (usePhoneNumber && phoneNumber != null && !phoneNumber.isNullOrEmpty() &&
            phoneNumber.indexOf("x") < 0
        ) {

            if (checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
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
                    requireContext(), 0, Intent(SMS_SENT_INTENT), 0
                )

                val deliveredPI = PendingIntent.getBroadcast(
                    requireContext(), 0, Intent(SMS_DELIVERED_INTENT), 0
                )

                val parts = smsManager.divideMessage(smsBody)

                val arraySendInt = ArrayList<PendingIntent>()
                arraySendInt.add(sentPI)
                val arrayDelivery = ArrayList<PendingIntent>()
                arrayDelivery.add(deliveredPI)
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    arraySendInt,
                    arrayDelivery
                )

            } else {

                val sendIntent = Intent(Intent.ACTION_VIEW)
                sendIntent.data = Uri.parse("sms:")
                sendIntent.putExtra("sms_body", smsBody)
                startActivity(sendIntent)
            }

        } else {
            val sendIntent = Intent(Intent.ACTION_VIEW)
            sendIntent.data = Uri.parse("sms:")
            sendIntent.putExtra("sms_body", smsBody)
            startActivity(sendIntent)
        }
    }

    private fun print() {
        if (smsBody != null) {
            val maskedPhoneNumber = sharedPrferences.getBoolean(PREF_MASKED_NUMBER, false)
            val smsPrint = SmsFilter().checkSmsType(smsBody!!, maskedPhoneNumber)

            CoroutineScope(IO).launch {
                intentPrint(smsPrint)
            }

        }
    }


    private fun checkSendSmsPermission() {
        if (checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.SEND_SMS),
                PERMISSION_RECEIVE_SMS_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            PERMISSION_SEND_SMS_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requireContext().toast("Permission granted to send sms")
                } else {
                    requireContext().toast("Permission denied  to send sms")
                }
            }
            PERMISSION_READ_PHONE_STATE_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
            }

        }
    }


    private fun checkPhoneStatusPermission() {
        if (checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PERMISSION_READ_PHONE_STATE_CODE
            )
        }
    }

    private fun shareMessage() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, smsBody)
        shareIntent.type = "text/plain"
        startActivity(shareIntent)
    }

    private suspend fun intentPrint(messageBody: String) {
        val buffer: ByteArray = messageBody.toByteArray()
        val printHeader = byteArrayOf(0xAA.toByte(), 0x55, 2, 0)
        printHeader[3] = buffer.size.toByte()
        initPrinter()
        if (printHeader.size > 128) {
            value = "\nValue is more than 128 size\n"
            CoroutineScope(Main).launch {
                requireContext().toast(value)
            }
        } else {
            try {
                outputStream?.write(messageBody.toByteArray())
                outputStream?.close()
                socket?.close()
            } catch (ex: java.lang.Exception) {
                value = "$ex\nExcep IntentPrint \n"
                CoroutineScope(Main).launch {
                    requireContext().toast(value)
                }
            }
        }
    }

    private suspend fun initPrinter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() as BluetoothAdapter
        try {
            if (!bluetoothAdapter?.isEnabled!!) {
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetooth, 0)
            }
            val printerName = sharedPrferences.getString(PREF_PRINTER_NAME, "")
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
                val m: Method = bluetoothDevice!!.javaClass.getMethod(
                    "createRfcommSocket", *arrayOf<Class<*>?>(
                        Int::class.javaPrimitiveType
                    )
                )
                socket = m.invoke(bluetoothDevice, 1) as BluetoothSocket?
                bluetoothAdapter?.cancelDiscovery()
                socket?.connect()
                outputStream = socket?.outputStream
                inputStream = socket?.inputStream
                beginListenForData()
            } else {
                value = "No Devices found"
                CoroutineScope(Main).launch {
                    requireContext().toast(value)
                }
                return
            }
        } catch (ex: java.lang.Exception) {
            value = "$ex\n InitPrinter \n"
            CoroutineScope(Main).launch {
                requireContext().toast(value)
            }
        }

    }


    private fun beginListenForData() {
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
                                    handler.post({ Timber.d(data) })
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


    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }
}