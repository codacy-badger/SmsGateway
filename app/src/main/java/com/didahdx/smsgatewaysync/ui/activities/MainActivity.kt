package com.didahdx.smsgatewaysync.ui.activities


import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.receiver.BatteryReceiver
import com.didahdx.smsgatewaysync.receiver.ConnectionReceiver
import com.didahdx.smsgatewaysync.receiver.PhoneCallReceiver
import com.didahdx.smsgatewaysync.receiver.SmsReceiver
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.ui.IMainActivity
import com.didahdx.smsgatewaysync.utilities.INPUT_EXTRAS
import com.didahdx.smsgatewaysync.utilities.PERMISSION_FOREGROUND_SERVICES_CODE
import com.didahdx.smsgatewaysync.utilities.PREF_SERVICES_KEY
import com.didahdx.smsgatewaysync.utilities.SMS_RECEIVED_INTENT
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.navigation_header.view.*


class MainActivity : AppCompatActivity() {

    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    var isReadyToPublish: Boolean = false
    var mIMainActivity: IMainActivity? = null
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment2)
        NavigationUI.setupWithNavController(navigation_view, navController)
        NavigationUI.setupActionBarWithNavController(this, navController, drawer_layout)

        AppCenter.start(
            application, "e55e44cf-eac5-49fc-a785-d6956b386176",
            Analytics::class.java, Crashes::class.java
        )

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        registerReceiver(SmsReceiver(), IntentFilter(SMS_RECEIVED_INTENT))

        //registering broadcast receiver for battery
        registerReceiver(BatteryReceiver(), IntentFilter(Intent.ACTION_BATTERY_CHANGED))


        //registering broadcast receiver for connection
        registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        val callFilter = IntentFilter("android.intent.action.NEW_OUTGOING_CALL")
        callFilter.addAction("android.intent.action.PHONE_STATE")

        //registering broadcast receiver for callReceiver
        registerReceiver(PhoneCallReceiver(), callFilter)

        //saving apps preference
        PreferenceManager.setDefaultValues(
            this,
            R.xml.preferences, false
        )

        val headView: View = navigation_view.getHeaderView(0)
        val navUser: TextView = headView.text_view_user_loggedIn
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            navUser.text = firebaseUser.email
        }


        checkForegroundPermission()
    }

    //used to check foreground services permission on android 9+
    private fun checkForegroundPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                    PERMISSION_FOREGROUND_SERVICES_CODE
                )
            }
        }
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            Navigation.findNavController(this, R.id.nav_host_fragment2), drawer_layout
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val fragments: List<Fragment> = supportFragmentManager.fragments
        if (fragments != null) {
            for (fragment in fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }

        when (requestCode) {
            PERMISSION_FOREGROUND_SERVICES_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
//                    toast("Permission denied")
                }
            }
        }
    }


    private fun startServices(input: String) {
        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
        if (isServiceRunning) {
            val serviceIntent = Intent(this, AppServices::class.java)
            serviceIntent.putExtra(INPUT_EXTRAS, input)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private fun stopServices() {
        val serviceIntent = Intent(this, AppServices::class.java)
        stopService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
        if (isServiceRunning) {
            stopServices()
        }

    }

    override fun onResume() {
        super.onResume()
        //registering broadcast receiver for smsReceiver

    }

    override fun onPause() {
        super.onPause()
//        unregisterReceiver(SmsReceiver())
    }
}