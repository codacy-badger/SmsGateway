package com.didahdx.smsgatewaysync.ui.fragments


import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.didahdx.smsgatewaysync.BuildConfig
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener
    , PrintingCallback, Preference.OnPreferenceClickListener {

    lateinit var onSharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener
    lateinit var onPreferenceClickListener: Preference.OnPreferenceClickListener
    var printing: Printing? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        onPreferenceClickListener = this
        onSharedPreferenceChangeListener = this
    }

    //shared preference listener
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

        when (key) {

            PREF_HOST_URL -> {
                val hostUrl: Preference? = findPreference<Preference>(key)
                hostUrl?.summary = preferenceScreen.sharedPreferences.getString(PREF_HOST_URL, "")
            }

            PREF_MPESA_TYPE -> {
                val mpesaType: Preference? = findPreference<Preference>(key)
                mpesaType?.summary =
                    preferenceScreen.sharedPreferences.getString(PREF_MPESA_TYPE, "")
            }

            PREF_PHONE_NUMBER -> {
                val phoneNumber: Preference? = findPreference<Preference>(key)
                phoneNumber?.summary =
                    preferenceScreen.sharedPreferences.getString(PREF_PHONE_NUMBER, "")
            }

            PREF_SIM_CARD -> {
                val simCard: Preference? = findPreference<Preference>(key)
                simCard?.summary = preferenceScreen.sharedPreferences.getString(PREF_SIM_CARD, "")
            }

            PREF_PRINT_TYPE -> {
                val printType: Preference? = findPreference<Preference>(key)
                printType?.summary =
                    preferenceScreen.sharedPreferences.getString(PREF_PRINT_TYPE, "")
            }

            PREF_CONNECT_PRINTER -> {
                val connectPrinter = findPreference<Preference>(key)
                val isPrinterConnected = preferenceScreen.sharedPreferences
                    .getBoolean(PREF_CONNECT_PRINTER, false)

                if (isPrinterConnected) {
                    addPrinter()
                } else {
                    removePrinter()
                }
            }

            PREF_SERVICES_KEY -> {
                val connectPrinter = findPreference<Preference>(key)
                val isServiceRunning = preferenceScreen.sharedPreferences
                    .getBoolean(PREF_SERVICES_KEY, true)

                if (isServiceRunning) {
                    startServices("$APP_NAME is Running")
                    context?.toast("Services is running")
                } else {
                    context?.toast("Services is stopped")
                    stopServices()
                }
            }

            PREF_FEEDBACK -> {

            }
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        val feedBack = findPreference(PREF_FEEDBACK) as Preference?
        when (preference) {
            feedBack -> {
                sendEmail()
            }
        }

        return false
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )

        //setting the summary values
        val hostUrl = findPreference<Preference>(PREF_HOST_URL)
        hostUrl?.summary = preferenceScreen.sharedPreferences.getString(PREF_HOST_URL, "")

        val mpesaType = findPreference<Preference>(PREF_MPESA_TYPE)
        mpesaType?.summary =
            preferenceScreen.sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)

        val phoneNumber = findPreference<Preference>(PREF_PHONE_NUMBER)
        phoneNumber?.summary =
            preferenceScreen.sharedPreferences.getString(PREF_PHONE_NUMBER, "+2547xxxxxxxx")

        val simCard = findPreference<Preference>(PREF_SIM_CARD)
        simCard?.summary = preferenceScreen.sharedPreferences.getString(
            PREF_SIM_CARD, "Choose your default sim card to use for the application"
        )

        val printTypes = findPreference<Preference>(PREF_PRINT_TYPE)
        printTypes?.summary = preferenceScreen.sharedPreferences.getString(
            PREF_PRINT_TYPE,
            "Types of mpesa transaction to be printed"
        )
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )
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
            context?.toast("Unpair ${Printooth.getPairedPrinter()?.name}")
        } else {
            context?.toast("Paired with Printer ${Printooth.getPairedPrinter()?.name}")
        }
    }

    private fun startServices(input: String) {
        val serviceIntent = Intent(activity as Activity, AppServices::class.java)
        serviceIntent.putExtra(INPUT_EXTRAS, input)
        ContextCompat.startForegroundService(activity as Activity, serviceIntent)
    }

    private fun stopServices() {
        val serviceIntent = Intent(activity as Activity, AppServices::class.java)
        context?.stopService(serviceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScanningActivity.SCANNING_FOR_PRINTER && resultCode == Activity.RESULT_OK) {
            initPrinter()
            changePairAndUnpair()
        } else {
            val connectPrinter = findPreference<Preference>(PREF_CONNECT_PRINTER)
            connectPrinter?.setDefaultValue(false)
        }
    }

    /**
     * E-mail intent for sending attachment
     */
    private fun sendEmail() {
        Timber.i("Sending Log ...###### ")
        val versionName = BuildConfig.VERSION_NAME

        val emailIntent: Intent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "message/rfc822"
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject, versionName))
        emailIntent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.email_to))
//        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
//        emailIntent.putExtra(Intent.EXTRA_TEXT, BODY_EMAIL)
        try {
            requireContext().startActivity(emailIntent)
        } catch (ex: ActivityNotFoundException) {
            Timber.i("No Intent matcher found")
        }

    }

    private fun initPrinter() {
        if (Printooth.hasPairedPrinter()) {
            printing = Printooth.printer()
        }

        if (printing != null) {
            printing?.printingCallback = this
        }
    }

    /*********************************************************************************************************
     ********************* BLUETOOTH PRINTER CALLBACK METHODS ************************************************
     **********************************************************************************************************/

    override fun connectingWithPrinter() {
        Toast.makeText(activity, "Connecting to printer", Toast.LENGTH_SHORT).show()
    }

    override fun connectionFailed(error: String) {
        Toast.makeText(activity, "Connecting to printer failed $error", Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: String) {
        Toast.makeText(activity, "Error $error", Toast.LENGTH_SHORT).show()
    }

    override fun onMessage(message: String) {
        Toast.makeText(activity, "Message $message", Toast.LENGTH_SHORT).show()
    }

    override fun printingOrderSentSuccessfully() {
        Toast.makeText(activity, "Order sent to printer", Toast.LENGTH_SHORT).show()
    }


    /***************************************************************************************************************************/

}
