package com.didahdx.smsgatewaysync.presentation.log

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.databinding.FragmentLogBinding
import com.didahdx.smsgatewaysync.util.IOExecutor
import kotlinx.android.synthetic.main.fragment_log.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

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
        context?.let { generateLogFile(text_view_log?.text.toString(), it) }

        val file =
            File("${context?.filesDir}/${context?.resources?.getString(R.string.log_file_name)}")
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/*"
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        startActivity(Intent.createChooser(sharingIntent, "share log file with"))

    }


    private fun generateLogFile(value:String, context:Context){
        IOExecutor.instance?.execute {
            var fileOutputStream: FileOutputStream? = null

            try {
                fileOutputStream = context.openFileOutput(
                    context.resources.getString(R.string.log_file_name), MODE_PRIVATE
                )
                fileOutputStream.write(value.toByteArray())
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (io: IOException) {
                io.printStackTrace()
            } finally {
                try {
                    fileOutputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
