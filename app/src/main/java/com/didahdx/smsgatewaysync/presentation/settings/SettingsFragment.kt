package com.didahdx.smsgatewaysync.presentation.settings


import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.didahdx.smsgatewaysync.BuildConfig
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.utilities.*
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener
    , Preference.OnPreferenceClickListener {

    lateinit var onSharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener
    lateinit var onPreferenceClickListener: Preference.OnPreferenceClickListener


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

            PREF_SERVICES_KEY -> {
                val isServiceOn = preferenceScreen.sharedPreferences
                    .getBoolean(PREF_SERVICES_KEY, true)
                if (isServiceOn) {
                    startServices("$APP_NAME connecting to server")
                } else {
                    stopServices()
                }
            }

            PREF_IMPORTANT_SMS_NOTIFICATION -> {
                val importantSmsNotification: Preference? = findPreference<Preference>(key)
                importantSmsNotification?.summary =
                    preferenceScreen.sharedPreferences.getString(
                        PREF_IMPORTANT_SMS_NOTIFICATION,
                        "None"
                    )
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
            PREF_PRINT_TYPE, "Types of mpesa transaction to be printed"
        )

        val importantSmsNotification = findPreference<Preference>(PREF_IMPORTANT_SMS_NOTIFICATION)
        importantSmsNotification?.summary =
            preferenceScreen.sharedPreferences.getString(PREF_IMPORTANT_SMS_NOTIFICATION, "None")
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )
    }

    private fun startServices(input: String) {
        if (ServiceState.STOPPED == context?.let { getServiceState(it) }) {
            val serviceIntent = Intent(activity as Activity, AppServices::class.java)
            serviceIntent.action = AppServiceActions.START.name
            serviceIntent.putExtra(INPUT_EXTRAS, input)
            ContextCompat.startForegroundService(activity as Activity, serviceIntent)
        }
    }

    private fun stopServices() {
        context?.let { SpUtil.setPreferenceString(it, PREF_STATUS_MESSAGE, "$APP_NAME is disabled") }
        context?.let { SpUtil.setPreferenceString(it, PREF_STATUS_COLOR, RED_COLOR) }

//        if (ServiceState.STOPPED != context?.let{ getServiceState(it)} ) {
        val serviceIntent = Intent(activity as Activity, AppServices::class.java)
        setRestartServiceState(activity as Activity, false)
        serviceIntent.action = AppServiceActions.STOP.name
        serviceIntent.putExtra(INPUT_EXTRAS, "$APP_NAME is stopped")
//            context?.stopService(serviceIntent)
        ContextCompat.startForegroundService(activity as Activity, serviceIntent)
//        }
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

}
