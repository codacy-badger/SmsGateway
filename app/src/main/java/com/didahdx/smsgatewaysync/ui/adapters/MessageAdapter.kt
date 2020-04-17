package com.didahdx.smsgatewaysync.ui.adapters;

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.model.MpesaMessageInfo
import kotlinx.android.synthetic.main.message_container.view.*

class MessageAdapter(
    val messageList: ArrayList<MpesaMessageInfo>,
    var clickListener: OnItemClickListener
) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var mlistener: MessageAdapter.OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onPrintPdf(position: Int)
    }


    class MessageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textViewSender: TextView =view.text_view_sender
        val textViewName:TextView=view.text_view_name
        val textViewTime:TextView=view.text_view_time
        val textViewAmount: TextView=view.text_view_amount
        val textViewMpesaId:TextView=view.text_view_mpesaId

        fun initialise(item:MpesaMessageInfo, action: OnItemClickListener) {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    action.onItemClick(position)
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
        holder.textViewSender.text = message.sender
        holder.textViewName.text = message.name
        holder.textViewAmount.text=message.amount
        holder.textViewTime.text = message.time
        holder.textViewMpesaId.text = message.mpesaId

        holder.initialise(message,clickListener)
    }

}
