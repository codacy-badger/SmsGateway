package com.didahdx.smsgatewaysync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import kotlinx.android.synthetic.main.activity_sms_details.*


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
    var printing: Printing? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_details)

        if (printing != null) {
            printing?.printingCallback = this
        }

        if (intent.extras != null) {
            val bundle = intent.extras
            //Retrieve the value
            smsBody = bundle?.getString(SMS_BODY)
            smsDate = bundle?.getString(SMS_DATE)
            smsSender = bundle?.getString(SMS_SENDER)

            text_view_sender_no_act.text = smsSender
            text_view_message_body_act.text = smsBody
            text_view_receipt_date_act.text = smsDate
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
        }
        return super.onOptionsItemSelected(item)
    }

    private fun print() {
        if (smsBody != null) {
            val smsFilter = SmsFilter()
            val bluetoothPrinter = bluetoothPrinter()

            val smsprint = smsFilter.checkSmsType(smsBody!!)

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

}
