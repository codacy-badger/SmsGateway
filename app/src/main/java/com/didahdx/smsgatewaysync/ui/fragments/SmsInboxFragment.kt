package com.didahdx.smsgatewaysync.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.databinding.FragmentSmsInboxBinding
import com.didahdx.smsgatewaysync.model.SmsInboxInfo
import com.didahdx.smsgatewaysync.ui.adapters.SmsInboxAdapter
import com.didahdx.smsgatewaysync.ui.adapters.SmsInboxAdapterListener
import com.didahdx.smsgatewaysync.ui.viewmodels.SmsInboxViewModel
import com.didahdx.smsgatewaysync.ui.viewmodels.SmsInboxViewModelFactory
import com.didahdx.smsgatewaysync.utilities.hide
import com.didahdx.smsgatewaysync.utilities.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple [Fragment] subclass.
 */
class SmsInboxFragment : Fragment() {

    private lateinit var smsInboxViewModel:SmsInboxViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentSmsInboxBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_sms_inbox, container, false)

        val application = requireNotNull(this.activity).application
        val factory= SmsInboxViewModelFactory(application)
        smsInboxViewModel= ViewModelProvider(this,factory).get(SmsInboxViewModel::class.java)

        binding.smsInboxViewModel=smsInboxViewModel
        val adapter = SmsInboxAdapter(SmsInboxAdapterListener {
            smsInboxViewModel.onMessageDetailClicked(it)
        })

        val manager = GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewMessageList2.layoutManager = manager
        binding.recyclerViewMessageList2.adapter = adapter
        binding.lifecycleOwner=this

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity?.onBackPressed()
        }

        //navigating to sms detail screen
        smsInboxViewModel.eventMessageClicked.observe(viewLifecycleOwner, Observer {
            it?.let {
                val bundle = bundleOf("SmsInfo" to smsInboxViewModel.setUpSmsInfo(it))
                this.findNavController()
                    .navigate(R.id.action_smsInboxFragment_to_smsDetailsFragment, bundle)
                smsInboxViewModel.onMessageDetailNavigated()
            }
        })

        smsInboxViewModel.messageArrayList.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.progressBar2.hide()
                binding.textLoading2.hide()
                adapter.submitList(it)
//                used to
//                (binding.recyclerViewMessageList.layoutManager as GridLayoutManager).scrollToPositionWithOffset(0, 0)
            }
        })



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(IO).launch {
          smsInboxViewModel.getDbSmsMessages()
        }


    }

    override fun onStart() {
        super.onStart()

    }


    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }

}




//    , MessageAdapter.OnItemClickListener {
//
//    private lateinit var sharedPreferences: SharedPreferences
//    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
//    var mMessageAdapter: MessageAdapter? = null
//    private var messageList: ArrayList<MpesaMessageInfo> = ArrayList<MpesaMessageInfo>()
//    lateinit var navController: NavController
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//        refresh_layout_home2?.setOnRefreshListener { backgroundCoroutineCall() }
//        backgroundCoroutineCall()
//        navController = Navigation.findNavController(view)
//
//    }
//
//    //used to get sms from the phone
//    private fun backgroundCoroutineCall() {
//        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
//            == PackageManager.PERMISSION_GRANTED
//        ) {
//            refresh_layout_home2?.isRefreshing = true
//            text_loading2?.text = getString(R.string.loading_messages, 0)
//            //coroutine background job
//            CoroutineScope(IO).launch {
//                getDbSmsMessages()
//            }
//        } else {
//            progress_bar2?.hide()
//            text_loading2?.hide()
//            setUpAdapter()
//        }
//
//    }
//
//    private fun setUpAdapter() {
//        progress_bar2?.hide()
//        text_loading2?.hide()
//        recycler_view_message_list2?.layoutManager = LinearLayoutManager(activity)
//        mMessageAdapter = MessageAdapter(messageList, this)
//        recycler_view_message_list2?.adapter = mMessageAdapter
//        refresh_layout_home2?.isRefreshing = false
//
//        if (messageList.size <= 0) {
//            text_loading2?.show()
//            text_loading2?.text = "No messages available at the moment"
//        }
//    }
//
//
//
//
//    //updates the counter on the screen
//    private suspend fun updateCounter(messageCount: Int) {
//        withContext(Main) {
//            text_loading2?.text = getString(R.string.loading_messages, messageCount)
//        }
//    }
//
//    //adds message to the screen
//    private suspend fun passMessagesToMain(list: ArrayList<MpesaMessageInfo>) {
//        withContext(Main) {
//            messageList.clear()
//            messageList = ArrayList<MpesaMessageInfo>(list)
//            setUpAdapter()
//        }
//    }
//
//    override fun onItemClick(position: Int) {
//        val messageInfo: MpesaMessageInfo = messageList[position]
//
//        val date = messageInfo.date
//        var smsStatus: String
//
//        CoroutineScope(IO).launch {
//            context?.let {
//                val messagesList = MessagesDatabase(it).getIncomingMessageDao().getMessage(date)
//
//
//
//                CoroutineScope(Main).launch {
//
//                    smsStatus = if (!messagesList.isNullOrEmpty() && messagesList[0].status) {
//                        if (messageInfo.status) {
//                            "Uploaded"
//                        } else {
//                            "pending"
//                        }
//                    } else {
//                        NOT_AVAILABLE
//                    }
//
//
//                    val smsInfo = SmsInfo(
//                        messageInfo.messageBody,
//                        messageInfo.time,
//                        messageInfo.sender,
//                        smsStatus,
//                        messageInfo.longitude,
//                        messageInfo.latitude)
//
//                    val bundle= bundleOf("SmsInfo" to smsInfo)
//                    navController.navigate(R.id.action_smsInboxFragment_to_smsDetailsFragment,bundle)
//
//                }
//            }
//        }
//    }
//
//}