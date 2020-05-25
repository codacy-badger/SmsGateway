package com.didahdx.smsgatewaysync.ui.printer

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.model.PrinterInfo
import com.didahdx.smsgatewaysync.utilities.PREF_PRINTER_NAME
import com.didahdx.smsgatewaysync.utilities.show
import kotlinx.android.synthetic.main.fragment_printer_detail.*
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 */
class PrinterDetailFragment : Fragment(R.layout.fragment_printer_detail) {

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mPairedDevicesArrayAdapter: ArrayAdapter<String>? = null
    val printerArray=ArrayList<PrinterInfo>()


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val filter = IntentFilter()
//
//        filter.addAction(BluetoothDevice.ACTION_FOUND)
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//
//        requireContext().registerReceiver(mReceiver, filter)
//    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mPairedDevicesArrayAdapter = ArrayAdapter(requireContext(), R.layout.device_name)


        val mPairedListView = paired_devices as ListView
        mPairedListView.adapter = mPairedDevicesArrayAdapter
        mPairedListView.onItemClickListener = mDeviceClickListener

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val mPairedDevices = mBluetoothAdapter.bondedDevices as Set<BluetoothDevice>

        printer_detail_refresh.setOnClickListener {
            printer_detail_refresh.isRefreshing=true
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

//    override fun onDestroy() {
//        super.onDestroy()
//        mBluetoothAdapter?.cancelDiscovery()
//        requireContext().unregisterReceiver(mReceiver)
//    }


    private val mDeviceClickListener =
        OnItemClickListener { mAdapterView, mView, mPosition, mLong ->
            try {
                mBluetoothAdapter.cancelDiscovery()
                val mDeviceInfo = (mView as TextView).text.toString()
                val mDeviceAddress = mDeviceInfo.substring(mDeviceInfo.length - 17)
                val printer= printerArray[mPosition]

                val printerName=printer.printerName

                val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val editor = sp.edit()
                editor.putString(PREF_PRINTER_NAME, printerName)
                editor.commit()

                Timber.i("Device_Address $mDeviceAddress")
                Timber.i("Device_Name $printerName")

                requireActivity().onBackPressed()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

//    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            val action = intent.action
//            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == action) {
//                //discovery starts, we can show progress dialog or perform other tasks
//            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
//                printer_detail_refresh.isRefreshing=false
//                //discovery finishes, dismis progress dialog
//            } else if (BluetoothDevice.ACTION_FOUND == action) {
//                //bluetooth device found
//                val device =
//                    intent.getParcelableExtra<Parcelable>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
//                if (device != null) {
//                    printerArray.add(PrinterInfo(device?.name, device?.address,device))
//                    mPairedDevicesArrayAdapter?.add(
//                        """
//                        ${device?.name}
//                        ${device?.address}
//                        """.trimIndent()
//                    )
//                }
//            }
//        }
//    }

}