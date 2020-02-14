package com.didahdx.smsgatewaysync.adapters;

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.model.MessageInfo
import kotlinx.android.synthetic.main.message_container.view.*

class MessageAdapter(
    val messageList: ArrayList<MessageInfo>,
    var clickListener: OnItemClickListener
) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var mlistener: MessageAdapter.OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onPrintPdf(position: Int)
    }


    class MessageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun initialise(item:MessageInfo,action: OnItemClickListener) {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    action.onItemClick(position)
                }
            }

            itemView.image_view_more.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    action.onPrintPdf(position)
                }
            }


        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.message_container, parent, false)
        )
    }

    override fun getItemCount() = messageList.size


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.view.text_view_sender.text = message.sender
        holder.view.text_view_message_body.text = message.messageBody
        holder.view.text_view_time.text = message.time
        holder.view.text_view_mpesaId.text = message.mpesaId

        holder.initialise(message,clickListener)
    }

}
