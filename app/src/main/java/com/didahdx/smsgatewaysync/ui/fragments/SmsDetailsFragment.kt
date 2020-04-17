package com.didahdx.smsgatewaysync.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.SMS_BODY_EXTRA
import com.didahdx.smsgatewaysync.utilities.SMS_DATE_EXTRA
import com.didahdx.smsgatewaysync.utilities.SMS_SENDER_EXTRA
import com.didahdx.smsgatewaysync.utilities.SmsFilter
import kotlinx.android.synthetic.main.fragment_sms_details.view.*


/**
 * A simple [Fragment] subclass.
 * Use the [SmsDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SmsDetailsFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sms_details, container, false)


        //Retrieve the value
        val smsBody: String? = arguments?.getString(SMS_BODY_EXTRA)
        val smsDate: String? = arguments?.getString(SMS_DATE_EXTRA)
        val smsSender: String? = arguments?.getString(SMS_SENDER_EXTRA)


        view.text_view_sender_no.text = smsSender
        view.text_view_message_body.text = smsBody
        view.text_view_receipt_date.text = smsDate
        if (smsBody != null) {
            val smsFilter = SmsFilter(smsBody)
            view.text_view_voucher_no.text = smsFilter.mpesaId
            view.text_view_transaction_date.text = smsFilter.date
            view.text_view_name.text=smsFilter.name
            view.text_view_phone_number.text=smsFilter.phoneNumber
            view.text_view_amount.text=smsFilter.amount
            view.text_view_account_no.text=smsFilter.accountNumber
        }


        return view
    }
}