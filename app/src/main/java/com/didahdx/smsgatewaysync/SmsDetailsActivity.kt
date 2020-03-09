package com.didahdx.smsgatewaysync

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.didahdx.smsgatewaysync.utilities.SMS_BODY
import com.didahdx.smsgatewaysync.utilities.SMS_DATE
import com.didahdx.smsgatewaysync.utilities.SMS_SENDER
import com.didahdx.smsgatewaysync.utilities.SmsFilter
import kotlinx.android.synthetic.main.activity_sms_details.*


class SmsDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_details)
        if (intent.extras!=null){
            val bundle=intent.extras
            //Retrieve the value
            val smsBody: String? = bundle?.getString(SMS_BODY)
            val smsDate: String? = bundle?.getString(SMS_DATE)
            val smsSender: String? = bundle?.getString(SMS_SENDER)

            text_view_sender_no_act.text = smsSender
           text_view_message_body_act.text = smsBody
            text_view_receipt_date_act.text = smsDate
            if (smsBody != null) {
                val smsFilter = SmsFilter(smsBody)
                text_view_voucher_no_act.text = smsFilter.mpesaId
              text_view_transaction_date_act.text = smsFilter.date
                text_view_name_act.text=smsFilter.name
                text_view_phone_number_act.text=smsFilter.phoneNumber
                text_view_amount_act.text=smsFilter.amount
                text_view_account_no_act.text=smsFilter.accountNumber
            }
        }

        //used to display the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
      if (item.itemId ==android.R.id.home ) {
                finish()
        }
        return super.onOptionsItemSelected(item)
    }


}
