package com.didahdx.smsgatewaysync.presentation.smsInbox

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.databinding.FragmentSmsInboxBinding
import com.didahdx.smsgatewaysync.utilities.SMS_LOCAL_BROADCAST_RECEIVER
import com.didahdx.smsgatewaysync.utilities.hide
import kotlinx.android.synthetic.main.fragment_sms_inbox.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 */
class SmsInboxFragment : Fragment() {

    private lateinit var smsInboxViewModel: SmsInboxViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mSmsReceiver, IntentFilter(SMS_LOCAL_BROADCAST_RECEIVER)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentSmsInboxBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_sms_inbox, container, false
        )

        val application = requireNotNull(this.activity).application
        val database = MessagesDatabase(application).getIncomingMessageDao()
        val factory = SmsInboxViewModelFactory(database, application)
        smsInboxViewModel = ViewModelProvider(this, factory).get(SmsInboxViewModel::class.java)

        binding.smsInboxViewModel = smsInboxViewModel
//        val cursor: Cursor? = null
//        val inboxAdapter =
//            SmsInboxCursorAdapter(
//                cursor,
//                SmsAdapterListener { sms ->
//                    smsInboxViewModel.onMessageDetailClicked(sms)
//                })
        val adapter=SmsInboxAdapter(SmsInboxAdapterListener { sms->
            smsInboxViewModel.onMessageDetailClicked(sms)
        })

        val manager = GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewMessageList2.layoutManager = manager
        binding.recyclerViewMessageList2.adapter = adapter
        binding.lifecycleOwner = this

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
            binding.refreshLayoutHome2.isRefreshing = false
            binding.progressBar2.hide()
            binding.textLoading2.hide()
            it?.let {
                it.size
                adapter.submitList(it)
 //    used to
 //    (binding.recyclerViewMessageList.layoutManager as GridLayoutManager).scrollToPositionWithOffset(0, 0)
            }
        })

        smsInboxViewModel.messageCount.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.textLoading2.text = getString(R.string.loading_messages, it)
            }
        })

        binding.refreshLayoutHome2.setOnRefreshListener {
            binding.refreshLayoutHome2.isRefreshing = true
            CoroutineScope(IO).launch {
                smsInboxViewModel.getDbSmsMessages()
            }
        }

        binding.refreshLayoutHome2.isRefreshing = true
//        CoroutineScope(IO).launch {
//            binding.refreshLayoutHome2.isRefreshing = true
//            smsInboxViewModel.getDbSmsMessages()
//        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }

    //broadcast sms receiver
    private val mSmsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            CoroutineScope(Main).launch {
                refresh_layout_home2?.isRefreshing = true
            }
            CoroutineScope(IO).launch {
                smsInboxViewModel.getDbSmsMessages()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.sms_inbox_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.action_import -> {
                smsInboxViewModel.getAllDbSms()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mSmsReceiver)
    }
}
