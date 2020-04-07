package com.didahdx.smsgatewaysync

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import kotlinx.android.synthetic.main.activity_sms_details.*
import java.util.ArrayList


class SmsDetailsActivity : AppCompatActivity(), PrintingCallback {


    /*********************************************************************************************************
     ********************* BLUETOOTH PRINTER CALLBACK METHODS ************************************************
     **********************************************************************************************************/

    override fun connectingWithPrinter() {
        toast("Connecting to printer")
    }

    override fun connectionFailed(error: String) {
        toast("Connecting to printer failed $error")
    }

    override fun onError(error: String) {
        toast("Error $error")
    }

    override fun onMessage(message: String) {
        toast("Message $message")
    }

    override fun printingOrderSentSuccessfully() {
        toast("Order sent to printer")
    }

    /***************************************************************************************************************************/


    var smsBody: String? = null
    var smsDate: String? = null
    var smsSender: String? = null
    var smsStatus: String? = null
    var printing: Printing? = null
    lateinit var sharedPrferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_details)
        sharedPrferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (printing != null) {
            printing?.printingCallback = this
        }

        if (intent.extras != null) {
            val bundle = intent.extras
            //Retrieve the value
            smsBody = bundle?.getString(SMS_BODY_EXTRA)
            smsDate = bundle?.getString(SMS_DATE_EXTRA)
            smsSender = bundle?.getString(SMS_SENDER_EXTRA)
            smsStatus = bundle?.getString(SMS_UPLOAD_STATUS_EXTRA)


            text_view_sender_no_act.text = smsSender
            text_view_message_body_act.text = smsBody
            text_view_receipt_date_act.text = smsDate
            text_view_status_check_act.text=smsStatus
            if (smsBody != null) {
                val smsFilter = SmsFilter(smsBody!!)
                text_view_voucher_no_act.text = smsFilter.mpesaId
                text_view_transaction_date_act.text = "${smsFilter.date}  ${smsFilter.time}"
                text_view_name_act.text = smsFilter.name
                text_view_phone_number_act.text = smsFilter.phoneNumber
                text_view_amount_act.text = smsFilter.amount
                text_view_account_no_act.text = smsFilter.accountNumber
            }
        }

        //used to display the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sms_details_menu, menu);
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.action_print -> {
                print()
            }
            R.id.action_forward_sms -> {
                forwardSms()
            }

        }
        return super.onOptionsItemSelected(item)
    }




    private fun checkWriteContactPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CONTACTS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                PERMISSION_WRITE_CONTACTS_CODE
            )
        }
    }

    private fun forwardSms() {
        checkSendSmsPermission()
        val phoneNumber = sharedPrferences.getString(PREF_PHONE_NUMBER, "")
        val usePhoneNumber = sharedPrferences.getBoolean(PREF_ENABLE_FORWARD_SMS, false)
        if (usePhoneNumber && phoneNumber != null && !phoneNumber.isNullOrEmpty() && phoneNumber.indexOf(
                "x"
            ) < 0
        ) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SEND_SMS
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                val smsManager = SmsManager.getDefault()

                val sentPI = PendingIntent.getBroadcast(
                    this, 0, Intent(SMS_SENT_INTENT), 0
                )

                val deliveredPI = PendingIntent.getBroadcast(
                    this, 0, Intent(SMS_DELIVERED_INTENT), 0
                )

                //when the SMS has been sent
                registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(arg0: Context?, arg1: Intent?) {
                        when (resultCode) {
                            Activity.RESULT_OK -> toast("SMS sent to $phoneNumber")
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> toast("Generic failure")
                            SmsManager.RESULT_ERROR_NO_SERVICE -> toast("No service")
                            SmsManager.RESULT_ERROR_NULL_PDU -> toast("Null PDU")
                            SmsManager.RESULT_ERROR_RADIO_OFF -> toast("Radio off")
                        }
                    }
                }, IntentFilter(SMS_SENT_INTENT))

                //when the SMS has been delivered
                registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(arg0: Context?, arg1: Intent?) {
                        when (resultCode) {
                            Activity.RESULT_OK -> toast("SMS delivered")
                            Activity.RESULT_CANCELED -> toast("SMS not delivered")
                        }
                    }
                }, IntentFilter(SMS_DELIVERED_INTENT))

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
            val smsprint = SmsFilter().checkSmsType(smsBody!!)

            if (!Printooth.hasPairedPrinter()) {
                startActivityForResult(
                    Intent(this, ScanningActivity::class.java)
                    , ScanningActivity.SCANNING_FOR_PRINTER
                )
            } else {
                bluetoothPrinter.printText(
                    smsprint,
                    this, getString(R.string.app_name)
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
            toast("Unpair ${Printooth.getPairedPrinter()?.name}")
        } else {
            toast("Paired with Printer ${Printooth.getPairedPrinter()?.name}")
        }
    }

    private fun checkSendSmsPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
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
                    toast("Permission granted to send sms")
                } else {
                    toast("Permission denied  to send sms")
                }
            }

        }
    }
}
