package com.didahdx.smsgatewaysync.ui.adapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo

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