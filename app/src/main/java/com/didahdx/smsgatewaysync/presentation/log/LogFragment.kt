package com.didahdx.smsgatewaysync.presentation.log

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.NonNull
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.didahdx.smsgatewaysync.BuildConfig
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.databinding.FragmentLogBinding
import com.didahdx.smsgatewaysync.util.APP_NAME
import com.didahdx.smsgatewaysync.util.IOExecutor
import kotlinx.android.synthetic.main.fragment_log.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class LogFragment : Fragment() {
    lateinit var logViewModel: LogViewModel
    private val SHARED_PROVIDER_AUTHORITY: String = BuildConfig.APPLICATION_ID + ".provider"
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
//        context?.let { generateLogFile(text_view_log?.text.toString(), it) }
//        val logPath = File(context?.filesDir, "${context?.resources?.getString(R.string.log_file_dir)}")
//        val file = File(logPath,"${context?.resources?.getString(R.string.log_file_name)}")
//        val contentUri =
//            context?.let { getUriForFile(it, "${it.packageName}.provider", file) }
//
//        val perm=Intent.FLAG_GRANT_READ_URI_PERMISSION and FLAG_GRANT_WRITE_URI_PERMISSION
//        context?.grantUriPermission("${context?.packageName}.provider", contentUri,  perm)
//        val sharingIntent = Intent(Intent.ACTION_SEND)
//        sharingIntent.type = "text/*"
//        sharingIntent.data = contentUri
//        sharingIntent.flags = perm
//        startActivity(Intent.createChooser(sharingIntent, "share log file with"))



        val sharedFile = context?.let { createFile(text_view_log?.text.toString(), it) }
        if (context!=null && sharedFile !=null){

            val uri: Uri = FileProvider.getUriForFile(requireContext(), SHARED_PROVIDER_AUTHORITY, sharedFile)

            val intentBuilder: ShareCompat.IntentBuilder =
                ShareCompat.IntentBuilder.from(requireActivity())
                    .setType("text/*")
                    .addStream(uri)
                    .setChooserTitle("Log file ${BuildConfig.VERSION_NAME}")

            val chooserIntent: Intent = intentBuilder.createChooserIntent()
            startActivity(chooserIntent)
        }

    }


    private fun generateLogFile(value:String, context:Context,destination:File){
        IOExecutor.instance?.execute {
            var fileOutputStream: FileOutputStream? = null

            try {
                fileOutputStream = FileOutputStream(destination)
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

    @NonNull
    @Throws(IOException::class)
    private fun createFile(value: String,context: Context): File? {
        val logFolder = File(context.filesDir, context.resources.getString(R.string.log_file_dir))
        logFolder.mkdirs()
        val logFile: File
        logFile = File.createTempFile("$APP_NAME ${BuildConfig.VERSION_NAME} log", ".txt", logFolder)
        logFile.createNewFile()
        generateLogFile(value,context,logFile)
        return logFile
    }


}
