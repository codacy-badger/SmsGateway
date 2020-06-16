package com.didahdx.smsgatewaysync.ui.log

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.AppLog
import com.didahdx.smsgatewaysync.utilities.toast
import kotlinx.android.synthetic.main.fragment_log.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LogFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LogFragment : Fragment(R.layout.fragment_log) {

    val appLog = AppLog()
    var log: String = " "
    override fun onStart() {
        super.onStart()

        text_view_log.text = getLogs()


    }

    private fun getLogs(): String {
        var logs = " "
        CoroutineScope(IO).launch {
            val log = appLog.readLog(activity as Activity)
        }

        return logs
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(IO).cancel()
    }
}
