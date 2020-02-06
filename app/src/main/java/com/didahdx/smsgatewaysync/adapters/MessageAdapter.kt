package com.didahdx.smsgatewaysync.adapters;

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.model.MessageInfo
import kotlinx.android.synthetic.main.message_container.view.*

class MessageAdapter(val messagelist: ArrayList<MessageInfo>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {


    class MessageViewHolder(val view: View) : RecyclerView.ViewHolder(view)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.message_container, parent, false)
        )
    }

    override fun getItemCount() =  messagelist.size


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message=messagelist[position]
        holder.view.text_view_sender.text=message.sender
        holder.view.text_view_message_body.text=message.messageBody
        holder.view.text_view_time.text=message.time
    }

}
