package com.didahdx.smsgatewaysync.ui


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.didahdx.smsgatewaysync.R
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import kotlinx.android.synthetic.main.fragment_settings.view.*

/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =inflater.inflate(R.layout.fragment_settings, container, false)
        view.btn_add_printer.setOnClickListener {
            addPrinter()
        }

        view.btn_remove_printer.setOnClickListener {
         removePrinter()
        }

        return view
    }


    //remove bluetooth printer
    fun removePrinter() {
        if (Printooth.hasPairedPrinter()) {
            Printooth.removeCurrentPrinter()
        }
            changePairAndUnpair()
    }


    //remove bluetooth printer
    fun addPrinter() {
        if (Printooth.hasPairedPrinter()) {
            Printooth.removeCurrentPrinter()
        } else {
            startActivityForResult(
                Intent(activity, ScanningActivity::class.java)
                , ScanningActivity.SCANNING_FOR_PRINTER
            )
            changePairAndUnpair()
        }
    }

    private fun changePairAndUnpair() {
        if (!Printooth.hasPairedPrinter()) {
            Toast
                .makeText(
                    activity,
                    "Unpair ${Printooth.getPairedPrinter()?.name}",
                    Toast.LENGTH_LONG
                )
                .show()
        } else {
            Toast
                .makeText(
                    activity,
                    "Paired with Printer ${Printooth.getPairedPrinter()?.name}",
                    Toast.LENGTH_LONG
                )
                .show()
        }
    }

}
