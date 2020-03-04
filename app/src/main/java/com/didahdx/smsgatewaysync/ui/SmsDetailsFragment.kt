package com.didahdx.smsgatewaysync.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.didahdx.smsgatewaysync.R


/**
 * A simple [Fragment] subclass.
 * Use the [SmsDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SmsDetailsFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sms_details, container, false)
    }


}
