package com.didahdx.smsgatewaysync.ui


import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback


/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragmentCompat(),SharedPreferences.OnSharedPreferenceChangeListener
,PrintingCallback{

    lateinit var onSharedPreferenceChangeListener : SharedPreferences.OnSharedPreferenceChangeListener
    var printing: Printing? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            onSharedPreferenceChangeListener=this
        }

    //shared preference listener
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == PREF_HOST_URL){
          val hostUrl: Preference? = findPreference<Preference>(key)
            hostUrl?.summary=preferenceScreen.sharedPreferences.getString(PREF_HOST_URL,"")
        }


        if (key == PREF_MPESA_TYPE){
          val mpesaType: Preference? = findPreference<Preference>(key)
            mpesaType?.summary=preferenceScreen.sharedPreferences.getString(PREF_MPESA_TYPE,"")
        }
        if (key == PREF_PHONE_NUMBER){
          val phoneNumber: Preference? = findPreference<Preference>(key)
            phoneNumber?.summary=preferenceScreen.sharedPreferences.getString(PREF_PHONE_NUMBER,"")
        }

        if (key.equals(PREF_CONNECT_PRINTER)){
            val connectPrinter=findPreference<Preference>(key)
            var isPrinterConnected= preferenceScreen.sharedPreferences
                .getBoolean(PREF_CONNECT_PRINTER,false)

            if (isPrinterConnected){
                addPrinter()
            }else{
                removePrinter()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        //setting the summary values
        val hostUrl=findPreference<Preference>(PREF_HOST_URL)
        hostUrl?.summary=preferenceScreen.sharedPreferences.getString(PREF_HOST_URL,"")

        val mpesaType=findPreference<Preference>(PREF_MPESA_TYPE)
        mpesaType?.summary=preferenceScreen.sharedPreferences.getString(PREF_MPESA_TYPE,ALL)

        val phoneNumber=findPreference<Preference>(PREF_PHONE_NUMBER)
        phoneNumber?.summary=preferenceScreen.sharedPreferences.getString(PREF_PHONE_NUMBER,"+2547xxxxxxxx")
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
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

    //remove bluetooth printer
    fun removePrinter() {
        if (Printooth.hasPairedPrinter()) {
            Printooth.removeCurrentPrinter()
        }
        changePairAndUnpair()
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==ScanningActivity.SCANNING_FOR_PRINTER && resultCode== Activity.RESULT_OK){
            initPrinter()
            changePairAndUnpair()
        }else{
            val connectPrinter=findPreference<Preference>(PREF_CONNECT_PRINTER)
            connectPrinter?.setDefaultValue(false)
        }
    }

    private fun initPrinter() {
        if (Printooth.hasPairedPrinter()){
            printing=Printooth.printer()
        }

        if (printing!=null){
            printing?.printingCallback=this
        }
    }

    /*********************************************************************************************************
     ********************* BLUETOOTH PRINTER CALLBACK METHODS ************************************************
     **********************************************************************************************************/

    override fun connectingWithPrinter() {
        Toast.makeText(activity,"Connecting to printer",Toast.LENGTH_SHORT).show()
    }

    override fun connectionFailed(error: String) {
        Toast.makeText(activity,"Connecting to printer failed $error",Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: String) {
        Toast.makeText(activity,"Error $error",Toast.LENGTH_SHORT).show()
    }

    override fun onMessage(message: String) {
        Toast.makeText(activity,"Message $message",Toast.LENGTH_SHORT).show()
    }

    override fun printingOrderSentSuccessfully() {
        Toast.makeText(activity,"Order sent to printer",Toast.LENGTH_SHORT).show()
    }

    /***************************************************************************************************************************/

}
