package com.didahdx.smsgatewaysync.ui.adapters

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.databinding.SmsInboxContainerBinding
import com.didahdx.smsgatewaysync.model.SmsInboxInfo
import com.didahdx.smsgatewaysync.ui.adapters.SmsInboxCursorAdapter.SmsViewHolder.Companion.from
import com.didahdx.smsgatewaysync.utilities.*
import java.text.SimpleDateFormat
import java.util.*

class SmsInboxCursorAdapter(
    context: Context,
    cursor: Cursor,
    private val clickListener: SmsAdapterListener
) :
    CustomCursorAdapter<SmsInboxCursorAdapter.SmsViewHolder>(cursor) {
    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
    val mContext = context

    class SmsViewHolder private constructor(val binding: SmsInboxContainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SmsInboxInfo, clickListener: SmsAdapterListener) {
            binding.messageText = item
            binding.clickListener = clickListener
        }

        companion object {
            fun from(parent: ViewGroup): SmsViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SmsInboxContainerBinding.inflate(layoutInflater, parent, false)
                return SmsViewHolder(binding)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        return from(parent)
    }


    override fun swapCursor(newCursor: Cursor?) {
        super.swapCursor(newCursor)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, cursor: Cursor?) {

        val nameId = cursor?.getColumnIndex("address")
        val messageId = cursor?.getColumnIndex("body")
        val dateId = cursor?.getColumnIndex("date")
        val dateString = dateId?.let { cursor.getString(it) }
        val smsFilter = messageId?.let { mMessageId ->
            cursor?.getString(mMessageId)?.let { mMessage ->
                SmsFilter(mMessage, false)
            }
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

        var smsInbox = ArrayList<SmsInboxInfo>()
//        when (mpesaType) {
//            PAY_BILL -> {
//                if (smsFilter?.mpesaType == PAY_BILL) {
//                    nameId?.let {
//                        cursor.getString(it)
//                    }?.let { sender ->
//                        dateString?.toLong()?.let { dateStr ->
//                            smsFilter?.mpesaId?.let { mpesaId ->
//                                smsInbox.add(
//                                    SmsInboxInfo(
//                                        messageId,
//                                        cursor.getString(messageId),
//                                        sdf.format(dateString?.toLong()?.let { it1 -> Date(it1) })
//                                            .toString(),
//                                        sender,
//                                        mpesaId,
//                                        smsFilter?.phoneNumber,
//                                        smsFilter?.amount,
//                                        smsFilter?.accountNumber,
//                                        smsFilter?.name,
//                                        dateStr, true, "", ""
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//            DIRECT_MPESA -> {
//                if (smsFilter?.mpesaType == DIRECT_MPESA) {
//                    nameId?.let {
//                        cursor.getString(it)
//                    }?.let { sender ->
//                        dateString?.toLong()?.let { dateStr ->
//                            smsFilter?.mpesaId?.let { mpesaId ->
//                                smsInbox.add(
//                                    SmsInboxInfo(
//                                        messageId,
//                                        cursor.getString(messageId),
//                                        sdf.format(dateString?.toLong()?.let { it1 -> Date(it1) })
//                                            .toString(),
//                                        sender,
//                                        mpesaId,
//                                        smsFilter?.phoneNumber,
//                                        smsFilter?.amount,
//                                        smsFilter?.accountNumber,
//                                        smsFilter?.name,
//                                        dateStr, true, "", ""
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//            BUY_GOODS_AND_SERVICES -> {
//                if (smsFilter?.mpesaType == BUY_GOODS_AND_SERVICES) {
//                    nameId?.let {
//                        cursor.getString(it)
//                    }?.let { sender ->
//                        dateString?.toLong()?.let { dateStr ->
//                            smsFilter?.mpesaId?.let { mpesaId ->
//                                smsInbox.add(
//                                    SmsInboxInfo(
//                                        messageId,
//                                        cursor.getString(messageId),
//                                        sdf.format(dateString?.toLong()?.let { it1 -> Date(it1) })
//                                            .toString(),
//                                        sender,
//                                        mpesaId,
//                                        smsFilter?.phoneNumber,
//                                        smsFilter?.amount,
//                                        smsFilter?.accountNumber,
//                                        smsFilter?.name,
//                                        dateStr, true, "", ""
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//            else -> {
//                nameId?.let {
//                    cursor.getString(it)
//                }?.let { sender ->
//                    dateString?.toLong()?.let { dateStr ->
//                        smsFilter?.mpesaId?.let { mpesaId ->
//                            smsInbox.add(
//                                SmsInboxInfo(
//                                    messageId,
//                                    cursor.getString(messageId),
//                                    sdf.format(dateString?.toLong()?.let { it1 -> Date(it1) })
//                                        .toString(),
//                                    sender,
//                                    mpesaId,
//                                    smsFilter?.phoneNumber,
//                                    smsFilter?.amount,
//                                    smsFilter?.accountNumber,
//                                    smsFilter?.name,
//                                    dateStr, true, "", ""
//                                )
//                            )
//                        }
//                    }
//                }
//            }
//
//        }

        nameId?.let {
            cursor.getString(it)
        }?.let { sender ->
            dateString?.toLong()?.let { dateStr ->
                smsFilter?.mpesaId?.let { mpesaId ->
                    smsInbox.add(
                        SmsInboxInfo(
                            messageId,
                            cursor.getString(messageId),
                            sdf.format(dateString?.toLong()?.let { it1 -> Date(it1) })
                                .toString(),
                            sender,
                            mpesaId,
                            smsFilter?.phoneNumber,
                            smsFilter?.amount,
                            smsFilter?.accountNumber,
                            smsFilter?.name,
                            dateStr, true, "", ""
                        )
                    )
                }
            }
        }


        if (smsInbox.isNotEmpty()) {
            holder.bind(smsInbox[0], clickListener)
        }
    }

}

class SmsAdapterListener(val clickListener: (messageId: SmsInboxInfo) -> Unit) {
    fun onClick(mMessage: SmsInboxInfo) = clickListener(mMessage)
}