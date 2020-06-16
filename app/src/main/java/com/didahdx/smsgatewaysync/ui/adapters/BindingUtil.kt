package com.didahdx.smsgatewaysync.ui.adapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo

@BindingAdapter("Sender")
fun TextView.setSender(item: MpesaMessageInfo?) {
    text = item?.sender
}

@BindingAdapter("Time")
fun TextView.setTime(item: MpesaMessageInfo?) {
    text = item?.time
}

@BindingAdapter("MpesaId")
fun TextView.setMpesaId(item: MpesaMessageInfo?) {
    text = item?.mpesaId
}

@BindingAdapter("Amount")
fun TextView.setAmount(item: MpesaMessageInfo?) {
    text = item?.amount
}

@BindingAdapter("Name")
fun TextView.setName(item: MpesaMessageInfo?) {
    text = item?.name
}

/**
 * Used for smsInbox
 * */

@BindingAdapter("sSender")
fun TextView.setSender(item: SmsInboxInfo?) {
    text = item?.sender
}

@BindingAdapter("sTime")
fun TextView.setTime(item: SmsInboxInfo?) {
    text = item?.time
}

@BindingAdapter("sMpesaId")
fun TextView.setMpesaId(item: SmsInboxInfo?) {
    text = item?.mpesaId
}

@BindingAdapter("sAmount")
fun TextView.setAmount(item: SmsInboxInfo?) {
    text = item?.amount
}

@BindingAdapter("sName")
fun TextView.setName(item: SmsInboxInfo?) {
    text = item?.name
}