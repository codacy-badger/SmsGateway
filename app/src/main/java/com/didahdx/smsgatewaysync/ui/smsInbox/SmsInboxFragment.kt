package com.didahdx.smsgatewaysync.ui.smsInbox

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
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
import com.didahdx.smsgatewaysync.ui.adapters.SmsAdapterListener
import com.didahdx.smsgatewaysync.ui.adapters.SmsInboxAdapter
import com.didahdx.smsgatewaysync.ui.adapters.SmsInboxAdapterListener
import com.didahdx.smsgatewaysync.ui.adapters.SmsInboxCursorAdapter

import com.didahdx.smsgatewaysync.utilities.hide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 */
class SmsInboxFragment : Fragment() {

    private lateinit var smsInboxViewModel: SmsInboxViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentSmsInboxBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_sms_inbox, container, false
        )

        val application = requireNotNull(this.activity).application
        val factory = SmsInboxViewModelFactory(application)
        smsInboxViewModel = ViewModelProvider(this, factory).get(SmsInboxViewModel::class.java)

        binding.smsInboxViewModel = smsInboxViewModel
        val adapter = SmsInboxAdapter(SmsInboxAdapterListener {
            smsInboxViewModel.onMessageDetailClicked(it)
        })


        val inboxAdapter=
            smsInboxViewModel.getCursor()?.let {
                SmsInboxCursorAdapter(it,SmsAdapterListener {
                    smsInboxViewModel.onMessageDetailClicked(it)
                })
            }

        val manager = GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewMessageList2.layoutManager = manager
//        binding.recyclerViewMessageList2.adapter = adapter
        binding.recyclerViewMessageList2.adapter = inboxAdapter
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
            binding.refreshLayoutHome2.isRefreshing=false
            binding.progressBar2.hide()
            binding.textLoading2.hide()
            it?.let {
                inboxAdapter?.swapCursor(it)
//                used to
//                (binding.recyclerViewMessageList.layoutManager as GridLayoutManager).scrollToPositionWithOffset(0, 0)
            }
        })

        smsInboxViewModel.messageCount.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.textLoading2.text = getString(R.string.loading_messages, it)
            }
        })

        binding.refreshLayoutHome2.setOnRefreshListener {
            binding.refreshLayoutHome2.isRefreshing=true
            CoroutineScope(IO).launch {
                smsInboxViewModel.getDbSmsMessages()
            }
            binding.refreshLayoutHome2.isRefreshing=false
        }


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
        CoroutineScope(IO).launch {
            smsInboxViewModel.getDbSmsMessages()
        }
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }

}
