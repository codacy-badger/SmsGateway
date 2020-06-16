package com.didahdx.smsgatewaysync.ui.smsInbox

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.databinding.SmsInboxContainerBinding
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.ui.smsInbox.SmsInboxCursorAdapter.SmsViewHolder.Companion.from
import com.didahdx.smsgatewaysync.utilities.*
import java.text.SimpleDateFormat
import java.util.*

class SmsInboxCursorAdapter(
    cursor: Cursor?,
    private val clickListener: SmsAdapterListener
) :
    RecyclerView.Adapter<SmsInboxCursorAdapter.SmsViewHolder>() {
    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)

    var mCursor = cursor

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
                return SmsViewHolder(
                    binding
                )
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        return from(parent)
    }


    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        if (mCursor != null) {
            if (!mCursor?.moveToPosition(position)!!) {
                return
            }
            val nameId = mCursor?.getColumnIndex("address")
            val messageId = mCursor?.getColumnIndex("body")
            val dateId = mCursor?.getColumnIndex("date")
            val dateString = dateId?.let { mCursor?.getString(it) }
            val smsFilter = messageId?.let { mCursor?.getString(it)?.let { SmsFilter(it, false) } }
            var smsinbox = ArrayList<SmsInboxInfo>()
            nameId?.let { mCursor?.getString(it) }?.let {
                dateString?.toLong()?.let { it1 ->
                    if (messageId != null) {
                        if (smsFilter != null) {
                            mCursor?.getString(messageId)?.let { it2 ->
                                SmsInboxInfo(
                                    messageId,
                                    it2,
                                    sdf.format(dateString?.toLong()?.let { it1 -> Date(it1) })
                                        .toString(),
                                    it,
                                    smsFilter.mpesaId,
                                    smsFilter.phoneNumber,
                                    smsFilter.amount,
                                    smsFilter.accountNumber,
                                    smsFilter.name,
                                    it1, false, "", ""
                                )
                            }?.let { it3 -> smsinbox.add(it3) }
                        }
                    }
                }
            }

            smsinbox?.let { holder.bind(it[0], clickListener) }
        }
    }

    override fun getItemCount(): Int {
        try {
            return mCursor?.count!!
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun swapCursor(newCursor: Cursor) {
        mCursor?.close()

        if (newCursor != null) {
            mCursor = newCursor
            notifyDataSetChanged()
        }
    }

}


class SmsAdapterListener(val clickListener: (messageId: SmsInboxInfo) -> Unit) {
    fun onClick(mMessage: SmsInboxInfo) = clickListener(mMessage)
}

