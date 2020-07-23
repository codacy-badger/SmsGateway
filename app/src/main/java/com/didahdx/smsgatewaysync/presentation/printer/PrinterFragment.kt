package com.didahdx.smsgatewaysync.presentation.printer

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
import com.didahdx.smsgatewaysync.printerlib.utils.PrefMng
import com.didahdx.smsgatewaysync.util.PREF_PRINTER_NAME
import com.didahdx.smsgatewaysync.util.SpUtil
import com.didahdx.smsgatewaysync.util.toast
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
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        context?.registerReceiver(mReceiver, filter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectPrinterButton.setOnClickListener(this)
        getCheckedPrinter()
//        printerConnected.text = "Printer Connected: No"
    }


    override fun onClick(v: View?) {
        when (v) {

            connectPrinterButton -> {
                val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled) {
                        if (saveSelectedPrinterBrand()){
                            this.findNavController()
                                .navigate(R.id.action_printerFragment_to_printerDetailFragment)
                        }
                    } else {
                        context?.toast("Turn on bluetooth")
                        val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBluetooth, 0)
                    }
                } else {
                    context?.toast("Device does not support bluetooth")
                }
            }

            removePrinter ->{
                context?.let { PrefMng.saveDeviceAddr(it, "") }
            }


        }
    }

    val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent?.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                printerConnected?.text = "Printer Found"
                context.toast("Printer found")
                //Device found
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                context.toast("Printer connected")
                //Device is now connected
                printerConnected?.text = "Printer Connected: Yes"
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                context.toast("Printer discovery finished")
                //Done searching
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED == action) {
                context.toast("Printer  is about to disconnect")
                //Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                context.toast("Printer is disconnect")
                //Device has disconnected
                printerConnected?.text = "Printer Connected: No"
            }
        }
    }


    private fun saveSelectedPrinterBrand(): Boolean {

        if (radBixolon.isChecked) {
            context?.let { PrefMng.saveActivePrinter(it, PrefMng.PRN_BIXOLON_SELECTED) }
            return true
        }
        if (radRongta.isChecked) {
            context?.let { PrefMng.saveActivePrinter(it, PrefMng.PRN_RONGTA_SELECTED) }
            return true
        }
        if (radWoosim.isChecked) {
            context?.let { PrefMng.saveActivePrinter(it, PrefMng.PRN_WOOSIM_SELECTED) }
            return true
        }

        if (radOthers.isChecked) {
            context?.let { PrefMng.saveActivePrinter(it, PrefMng.PRN_OTHER_PRINTERS_SELECTED) }
            return true
        }
        context?.toast( R.string.choose_printer)
        return false
    }

    private fun getCheckedPrinter() {

        when (context?.let { PrefMng.getActivePrinter(it) }) {
            PrefMng.PRN_BIXOLON_SELECTED -> {
                radBixolon?.isChecked = true
            }
            PrefMng.PRN_RONGTA_SELECTED -> {
                radRongta?.isChecked = true
            }
            PrefMng.PRN_WOOSIM_SELECTED -> {
                radWoosim?.isChecked = true
            }
            PrefMng.PRN_OTHER_PRINTERS_SELECTED -> {
                radOthers?.isChecked = true
            }
        }

        val printerName=context?.let { SpUtil.getPreferenceString(it,PREF_PRINTER_NAME," ") }
        printerConnected?.text="Connected to $printerName"
    }

    override fun onDestroy() {
        super.onDestroy()
        context?.unregisterReceiver(mReceiver)
    }
}
