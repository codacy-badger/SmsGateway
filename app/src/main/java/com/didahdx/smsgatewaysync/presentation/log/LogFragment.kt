package com.didahdx.smsgatewaysync.presentation.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.databinding.FragmentLogBinding

class LogFragment : Fragment() {

    lateinit var logViewModel: LogViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentLogBinding =
            DataBindingUtil.inflate(inflater,R.layout.fragment_log, container, false)

        val application = requireNotNull(this.activity).application
        val database = MessagesDatabase(application).getLogInfoDao()
        val factory = LogViewModelFactory(application, database)
        logViewModel = ViewModelProvider(this, factory).get(LogViewModel::class.java)
        binding.logViewModel = logViewModel
        binding.lifecycleOwner = this


        logViewModel.getLogs().observe(viewLifecycleOwner, Observer {

        })

        logViewModel.messageLogs.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.textViewLog.text = it
            }
        })


        return binding.root
    }
}
