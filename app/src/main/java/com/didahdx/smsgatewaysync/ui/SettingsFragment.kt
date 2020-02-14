package com.didahdx.smsgatewaysync.ui


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.PERMISSION_CALL_PHONE_CODE
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
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        view.btn_add_printer.setOnClickListener {
            addPrinter()
        }

        view.btn_remove_printer.setOnClickListener {
            removePrinter()
        }

        view.btn_check_balance.setOnClickListener {
            checkCredit()
        }

        return view
    }


    fun checkCredit() {

        checkCallPermission()


        val i = Intent(Intent.ACTION_CALL)
        i.data = Uri.parse("tel:${Uri.encode("*131#")}")

        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(i)
        }

    }

    private fun checkCallPermission() {
        if (ActivityCompat.checkSelfPermission(activity as Activity, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity as Activity,
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSION_CALL_PHONE_CODE
            )
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            PERMISSION_CALL_PHONE_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
