package com.didahdx.smsgatewaysync.ui.smsDetails

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.didahdx.smsgatewaysync.model.SmsInfo
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import kotlinx.android.synthetic.main.fragment_sms_details.*
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [SmsDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SmsDetailsFragment : Fragment(R.layout.fragment_sms_details), PrintingCallback {

    /*********************************************************************************************************
     ********************* BLUETOOTH PRINTER CALLBACK METHODS ************************************************
     **********************************************************************************************************/

    override fun connectingWithPrinter() {
        requireContext().toast("Connecting to printer")
    }

    override fun connectionFailed(error: String) {
        requireContext().toast("Connecting to printer failed $error")
    }

    override fun onError(error: String) {
        requireContext().toast("Error $error")
    }

    override fun onMessage(message: String) {
        requireContext().toast("Message $message")
    }

    override fun printingOrderSentSuccessfully() {
        requireContext().toast("Order sent to printer")
    }

    /***************************************************************************************************************************/

    var smsBody: String? = " "
    var smsDate: String? = " "
    var smsSender: String? = " "
    var smsStatus: String? = " "
    var longitude: String? = " "
    var latitude: String? = " "
    var printing: Printing? = null
    lateinit var sharedPrferences: SharedPreferences
    private var SmsInfo: SmsInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        SmsInfo = arguments?.getParcelable<SmsInfo>("SmsInfo")
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPrferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (printing != null) {
            printing?.printingCallback = this
        }

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
            val maskedPhoneNumber=sharedPrferences.getBoolean(PREF_MASKED_NUMBER,false)
            val smsFilter = SmsFilter(smsBody!!,maskedPhoneNumber)
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
            val bluetoothPrinter = BluetoothPrinter()
            val maskedPhoneNumber=sharedPrferences.getBoolean(PREF_MASKED_NUMBER,false)
            val smsprint = SmsFilter().checkSmsType(smsBody!!,maskedPhoneNumber)

            if (!Printooth.hasPairedPrinter()) {
                startActivityForResult(
                    Intent(requireContext(), ScanningActivity::class.java)
                    , ScanningActivity.SCANNING_FOR_PRINTER
                )
            } else {
                bluetoothPrinter.printText(
                    smsprint,
                    requireContext(), getString(R.string.app_name)
                )

            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScanningActivity.SCANNING_FOR_PRINTER && resultCode == Activity.RESULT_OK) {
            initPrinter()
            changePairAndUnpair()
        }
    }

    private fun initPrinter() {
        if (Printooth.hasPairedPrinter()) {
            printing = Printooth.printer()
        }

        if (printing != null) {
            printing?.printingCallback = this
        }
    }

    private fun changePairAndUnpair() {
        if (!Printooth.hasPairedPrinter()) {
            requireContext().toast("Unpair ${Printooth.getPairedPrinter()?.name}")
        } else {
            requireContext().toast("Paired with Printer ${Printooth.getPairedPrinter()?.name}")
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

}