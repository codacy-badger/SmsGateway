package com.didahdx.smsgatewaysync.presentation.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.BuildConfig
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.util.APP_NAME
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_about.view.*
import kotlinx.android.synthetic.main.fragment_about.view.text_about_phone


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AboutFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AboutFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AboutFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_about, container, false)

        view.text_about_phone.text = "Battery"
        view.text_network.text = "Network"
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val versionName = BuildConfig.VERSION_NAME

        text_network.text = "\nApp name: $APP_NAME  \n\nVersion number: $versionName "
    }
}
