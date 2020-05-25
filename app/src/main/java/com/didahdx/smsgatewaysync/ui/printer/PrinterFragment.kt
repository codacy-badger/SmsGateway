package com.didahdx.smsgatewaysync.ui.printer

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController

import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.toast
import kotlinx.android.synthetic.main.fragment_printer.*

/**
 * A simple [Fragment] subclass.
 */
class PrinterFragment : Fragment(R.layout.fragment_printer), View.OnClickListener {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectPrinterButton.setOnClickListener(this)

    }



    override fun onClick(v: View?) {
        when (v) {

            connectPrinterButton -> {
                val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled) {
                        this.findNavController()
                            .navigate(R.id.action_printerFragment_to_printerDetailFragment)
                    } else {
                        val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBluetooth, 0)
                    }
                } else {
                    requireContext().toast("Device does not support bluetooth")
                }
            }
        }
    }

}
