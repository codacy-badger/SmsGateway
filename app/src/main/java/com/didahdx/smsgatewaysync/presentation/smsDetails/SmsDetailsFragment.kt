package com.didahdx.smsgatewaysync.presentation.smsDetails

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.domain.SmsInfo
import com.didahdx.smsgatewaysync.printerlib.IPrintToPrinter
import com.didahdx.smsgatewaysync.printerlib.WoosimPrnMng
import com.didahdx.smsgatewaysync.printerlib.utils.PrefMng
import com.didahdx.smsgatewaysync.printerlib.utils.Tools
import com.didahdx.smsgatewaysync.printerlib.utils.printerFactory
import com.didahdx.smsgatewaysync.util.*
import kotlinx.android.synthetic.main.fragment_sms_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel

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
    private var SmsInfo: SmsInfo? = null
    private var mPrnMng: WoosimPrnMng?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        SmsInfo = arguments?.getParcelable<SmsInfo>("SmsInfo")
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            val maskedPhoneNumber = context?.let { SpUtil.getPreferenceBoolean(it,PREF_MASKED_NUMBER) }?:false
            val smsFilter = SmsFilter(smsBody!!, maskedPhoneNumber)
            text_view_voucher_no_act.text = smsFilter.mpesaId
            text_view_transaction_date_act.text = "${smsFilter.date}  ${smsFilter.time}"
            text_view_name_act.text = smsFilter.name
            text_view_phone_number_act.text = smsFilter.phoneNumber
            text_view_amount_act.text = smsFilter.amount
            text_view_account_no_act.text = smsFilter.accountNumber
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
                shareMessage()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun print() {
        if (smsBody != null) {
            val maskedPhoneNumber = context?.let { SpUtil.getPreferenceBoolean(it,PREF_MASKED_NUMBER) }?:false
            val smsPrint = SmsFilter().checkSmsType(smsBody!!, maskedPhoneNumber)
                    Tools.isBlueToothOn(context)
            val address: String = context?.let { PrefMng.getDeviceAddr(it) } ?: ""
            if (address.isNotEmpty()) {
                if (context!=null && Tools.isBlueToothOn(context)){
                    val testPrinter: IPrintToPrinter =  BluetoothPrinter(requireContext(), smsPrint)
                    //Connect to the printer and after successful connection issue the print command.
                    mPrnMng = printerFactory.createPrnMng(context, address, testPrinter)
                }
            } else {
                context?.toast("Printer not connected ")
            }

        }
    }

    private fun shareMessage() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, smsBody)
        shareIntent.type = "text/plain"
        startActivity(shareIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPrnMng?.releaseAllocatoins()
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }

}