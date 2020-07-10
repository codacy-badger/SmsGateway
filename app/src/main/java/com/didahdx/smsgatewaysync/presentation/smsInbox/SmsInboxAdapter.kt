package com.didahdx.smsgatewaysync.presentation.smsInbox

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.databinding.SmsInboxContainerBinding
import com.didahdx.smsgatewaysync.domain.SmsInboxInfo
import com.didahdx.smsgatewaysync.presentation.smsInbox.SmsInboxAdapter.SmsInboxViewHolder.Companion.from


class SmsInboxAdapter(private val clickListener: SmsInboxAdapterListener) :
    ListAdapter<SmsInboxInfo, SmsInboxAdapter.SmsInboxViewHolder>(
        SmsInboxAdapterDiffCallback()
    ) {


    class SmsInboxViewHolder private constructor(val binding: SmsInboxContainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SmsInboxInfo, clickListener: SmsInboxAdapterListener) {
            binding.messageText = item
//            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): SmsInboxViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SmsInboxContainerBinding.inflate(layoutInflater, parent, false)

                return SmsInboxViewHolder(
                    binding
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsInboxViewHolder {
        return from(parent)
    }

    override fun onBindViewHolder(holder: SmsInboxViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, clickListener)
    }

}

class SmsInboxAdapterListener(val clickListener: (messageId: SmsInboxInfo) -> Unit) {
    fun onClick(mMessage: SmsInboxInfo) = clickListener(mMessage)
}

class SmsInboxAdapterDiffCallback : DiffUtil.ItemCallback<SmsInboxInfo>() {
    override fun areItemsTheSame(
        oldItem: SmsInboxInfo,
        newItem: SmsInboxInfo
    ): Boolean {
        return oldItem.date == newItem.date
    }

    override fun areContentsTheSame(
        oldItem: SmsInboxInfo,
        newItem: SmsInboxInfo
    ): Boolean {
        return oldItem == newItem
    }
}