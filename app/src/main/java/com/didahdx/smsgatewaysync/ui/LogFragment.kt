package com.didahdx.smsgatewaysync.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.didahdx.smsgatewaysync.R


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LogFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LogFragment : Fragment() {




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log, container, false)
    }


}
