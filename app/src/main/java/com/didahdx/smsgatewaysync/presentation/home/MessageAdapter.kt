package com.didahdx.smsgatewaysync.presentation.home;

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.databinding.MessageContainerBinding
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.presentation.home.MessageAdapter.MessageViewHolder.Companion.from

class MessageAdapter(val clickListener: MessageAdapterListener) :
    ListAdapter<MpesaMessageInfo, MessageAdapter.MessageViewHolder>(
        MessageAdapterDiffCallback()
    ) {


    class MessageViewHolder private constructor(val binding: MessageContainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MpesaMessageInfo, clickListener: MessageAdapterListener) {
            binding.messageText = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): MessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = MessageContainerBinding.inflate(layoutInflater, parent, false)

                return MessageViewHolder(
                    binding
                )
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return from(parent)
    }


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, clickListener)
    }

}

class MessageAdapterListener(val clickListener: (messageId: MpesaMessageInfo) -> Unit) {
    fun onClick(mMessage: MpesaMessageInfo) = clickListener(mMessage)
}

class MessageAdapterDiffCallback : DiffUtil.ItemCallback<MpesaMessageInfo>() {
    override fun areItemsTheSame(
        oldItem: MpesaMessageInfo,
        newItem: MpesaMessageInfo
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: MpesaMessageInfo,
        newItem: MpesaMessageInfo
    ): Boolean {
        return oldItem == newItem
    }
}