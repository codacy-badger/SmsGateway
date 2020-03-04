package com.didahdx.smsgatewaysync.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.didahdx.smsgatewaysync.App

import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.receiver.BatteryReceiver
import com.didahdx.smsgatewaysync.receiver.ConnectionReceiver
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_about.view.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AboutFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AboutFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AboutFragment : Fragment(),BatteryReceiver.BatteryReceiverListener {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view:View= inflater.inflate(R.layout.fragment_about, container, false)


        //registering broadcast receiver for battery
        context?.registerReceiver(BatteryReceiver(),
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )


        view.text_about_phone.text="Battery"
        view.text_network.text="Network"
        return  view
    }

    override fun onBatteryStatusChanged(batteryStatus: String) {
        text_about_phone.text=batteryStatus
    }




}
