package com.didahdx.smsgatewaysync.presentation.log

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.databinding.FragmentLogBinding
import kotlinx.android.synthetic.main.fragment_log.*

class LogFragment : Fragment() {
    lateinit var logViewModel: LogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentLogBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_log, container, false)
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
//                binding.textViewLog.loadDataWithBaseURL("", it, "text/html", "UTF-8", "")
                binding.textViewLog.text = it
            }
        })

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.log_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share_log -> {
                shareLogs()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shareLogs() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, text_view_log?.text)
        shareIntent.type = "text/plain"
        startActivity(shareIntent)
    }
}
