package com.didahdx.smsgatewaysync.presentation.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.domain.PrinterInfo
import com.didahdx.smsgatewaysync.printerlib.utils.PrefMng.saveDeviceAddr
import com.didahdx.smsgatewaysync.utilities.PREF_PRINTER_NAME
import com.didahdx.smsgatewaysync.utilities.SpUtil
import com.didahdx.smsgatewaysync.utilities.show
import kotlinx.android.synthetic.main.fragment_printer_detail.*
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 */
class PrinterDetailFragment : Fragment(R.layout.fragment_printer_detail) {

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mPairedDevicesArrayAdapter: ArrayAdapter<String>? = null
    val printerArray = ArrayList<PrinterInfo>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mPairedDevicesArrayAdapter = ArrayAdapter(requireContext(), R.layout.device_name)


        val mPairedListView = paired_devices as ListView
        mPairedListView.adapter = mPairedDevicesArrayAdapter
        mPairedListView.onItemClickListener = mDeviceClickListener

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val mPairedDevices = mBluetoothAdapter.bondedDevices as Set<BluetoothDevice>

        printer_detail_refresh.setOnClickListener {
            printer_detail_refresh.isRefreshing = true
            mBluetoothAdapter.startDiscovery()
        }

        if (mPairedDevices.isNotEmpty()) {
            title_paired_devices.show()
            for (mDevice in mPairedDevices) {
                printerArray.add(PrinterInfo(mDevice.name, mDevice.address))
                mPairedDevicesArrayAdapter?.add(
                    """
                        ${mDevice.name}
                        ${mDevice.address}
                        """.trimIndent()
                )
            }
        } else {
            val mNoDevices =
                "None Paired" //getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter?.add(mNoDevices)
        }
    }


    private val mDeviceClickListener =
        OnItemClickListener { mAdapterView, mView, mPosition, mLong ->
            try {
                printer_detail_refresh.isRefreshing = false
                mBluetoothAdapter.cancelDiscovery()
                val mDeviceInfo = (mView as TextView).text.toString()
                val mDeviceAddress = mDeviceInfo.substring(mDeviceInfo.length - 17)
                val printer = printerArray[mPosition]

                val printerName = printer.printerName
                context?.let { SpUtil.setPreferenceString(it,PREF_PRINTER_NAME,  printerName) }
                context?.let { saveDeviceAddr(it, printer.printerAddress) }

                Timber.i("Device_Address $mDeviceAddress")
                Timber.i("Device_Name $printerName")

                requireActivity().onBackPressed()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }


}