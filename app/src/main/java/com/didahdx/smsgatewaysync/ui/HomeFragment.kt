package com.didahdx.smsgatewaysync.ui


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.didahdx.smsgatewaysync.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.model.MessageInfo
import com.didahdx.smsgatewaysync.R
import kotlinx.android.synthetic.main.fragment_home.*

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment() {
    private var messageList: ArrayList<MessageInfo> = ArrayList<MessageInfo>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        refresh_layout_home.setOnRefreshListener {
            getMessages()
        }

        getMessages()
    }

    private fun getMessages() {

        refresh_layout_home.isRefreshing = true

        messageList.clear()

        messageList.add(MessageInfo("OIUYTRTYU confirmed dd", "5:00PM", "MPESA", "OIUYTRE"))
        messageList.add(MessageInfo("OIUYTRTYU confirmed dd", "5:00PM", "MPESA", "OIUYTRE"))
        messageList.add(MessageInfo("OIUYTRTYU confirmed dd", "5:00PM", "MPESA", "OIUYTRE"))
        messageList.add(MessageInfo("OIUYTRTYU confirmed dd", "5:00PM", "MPESA", "OIUYTRE"))
        messageList.add(MessageInfo("OIUYTRTYU confirmed dd", "5:00PM", "MPESA", "OIUYTRE"))
        messageList.add(MessageInfo("OIUYTRTYU confirmed dd", "5:00PM", "MPESA", "OIUYTRE"))
        messageList.add(MessageInfo("OIUYTRTYU confirmed dd", "5:00PM", "MPESA", "OIUYTRE"))

        refresh_layout_home.isRefreshing = false

        recycler_view_message_list.layoutManager = LinearLayoutManager(activity)
        recycler_view_message_list.adapter = MessageAdapter(messageList)
    }


}
