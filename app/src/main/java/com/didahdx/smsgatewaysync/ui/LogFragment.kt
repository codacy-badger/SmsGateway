package com.didahdx.smsgatewaysync.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.AppLog
import kotlinx.android.synthetic.main.fragment_log.*
import kotlinx.android.synthetic.main.fragment_log.view.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LogFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LogFragment : Fragment() {

    val appLog=AppLog()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_log, container, false)
        return view;
    }

    override fun onStart() {
        super.onStart()
        text_view_log.text=appLog.readLog(activity as Activity)
    }
}
