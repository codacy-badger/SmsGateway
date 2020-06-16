package com.didahdx.smsgatewaysync.ui.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.toast
import kotlinx.android.synthetic.main.fragment_printer.*


/**
 * A simple [Fragment] subclass.
 */
class PrinterFragment : Fragment(R.layout.fragment_printer), View.OnClickListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        requireContext().registerReceiver(mReceiver, filter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectPrinterButton.setOnClickListener(this)
        printerConnected.text = "Printer Connected: No"
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

    val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent?.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                //Device found
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                //Device is now connected
                printerConnected.text = "Printer Connected: Yes"
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                //Done searching
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED == action) {
                //Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                //Device has disconnected
                printerConnected.text = "Printer Connected: No"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(mReceiver)
    }
}
