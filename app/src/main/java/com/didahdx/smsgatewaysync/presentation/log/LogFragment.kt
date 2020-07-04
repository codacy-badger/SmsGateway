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
import com.didahdx.smsgatewaysync.databinding.FragmentLogBinding
import com.didahdx.smsgatewaysync.utilities.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LogFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LogFragment : Fragment() {

    var log: String = " "
    lateinit var logViewModel: LogViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentLogBinding =
            DataBindingUtil.inflate(inflater,R.layout.fragment_log, container, false)

        val application = requireNotNull(this.activity).application
        val factory = LogViewModelFactory(application, AppLog)
        logViewModel = ViewModelProvider(this, factory).get(LogViewModel::class.java)
        binding.logViewModel = logViewModel
        binding.lifecycleOwner = this


        logViewModel.appLogs.observe(viewLifecycleOwner, Observer { log ->
            log?.let {
                binding.textViewLog.text = it
            }
        })

        return binding.root;
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }
}