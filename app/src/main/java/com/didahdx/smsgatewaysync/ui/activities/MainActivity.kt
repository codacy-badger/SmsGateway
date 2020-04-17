package com.didahdx.smsgatewaysync.ui.activities


import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.App
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.receiver.*
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.ui.*
import com.didahdx.smsgatewaysync.ui.fragments.*
import com.didahdx.smsgatewaysync.utilities.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_layout.*
import java.util.*


class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener, ConnectionReceiver.ConnectionReceiverListener {

    //checks on network connectivity to update the notification bar
    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        val time = Date()
        if (!isConnected) {
//            startServices("No internet connection")
            appLog.writeToLog(this, "\n\n $time \n No internet Connection")
        } else {
//            startServices("${getString(R.string.app_name)} is Running")
            appLog.writeToLog(this, "\n\n $time \n Connected to Internet ")
        }
    }


    val appLog = AppLog()
    lateinit var homeFragment: HomeFragment
    lateinit var settingsFragment: SettingsFragment
    lateinit var logFragment: LogFragment
    lateinit var aboutFragment: AboutFragment
    lateinit var phoneStatusFragment: PhoneStatusFragment
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    var isReadyToPublish: Boolean = false
    var mIMainActivity: IMainActivity? = null
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        AppCenter.start(
            application, "e55e44cf-eac5-49fc-a785-d6956b386176",
            Analytics::class.java, Crashes::class.java
        )

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        setSupportActionBar(toolbar)

        val drawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer_layout, toolbar,
            R.string.open,
            R.string.close
        ) {

        }

        drawerToggle.isDrawerIndicatorEnabled = true
        drawer_layout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        navigation_view.setNavigationItemSelectedListener(this)

        settingUpDefaultFragment()



        App.instance.setConnectionListener(this)
        //registering broadcast receiver for internet connection
        baseContext.registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        //registering broadcast receiver for battery
        baseContext.registerReceiver(
            BatteryReceiver(),
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        //registering broadcast receiver for smsReceiver
        baseContext.registerReceiver(
            SmsReceiver(), IntentFilter(SMS_RECEIVED_INTENT)
        )

        val callFilter=IntentFilter("android.intent.action.NEW_OUTGOING_CALL")
        callFilter.addAction("android.intent.action.PHONE_STATE")

        //registering broadcast receiver for callReceiver
        baseContext.registerReceiver(
            PhoneCallReceiver(), callFilter
        )

        //registering broadcast receiver for callReceiver
        baseContext.registerReceiver(
            CallReceiver(),callFilter
        )

        //saving apps preference
        PreferenceManager.setDefaultValues(this,
            R.xml.preferences, false)

        val headView: View = navigation_view.getHeaderView(0)
        val navUser: TextView = headView.findViewById<TextView>(R.id.text_view_user_loggedIn)
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

    private fun settingUpDefaultFragment() {
        //setting default fragment
        homeFragment = HomeFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, homeFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    //handling navigation drawer events
    override fun onNavigationItemSelected(menu: MenuItem): Boolean {
        when (menu.itemId) {
            R.id.nav_home -> {
                homeFragment =
                    HomeFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, homeFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_about -> {
                aboutFragment =
                    AboutFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, aboutFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_phone_status -> {
                phoneStatusFragment =
                    PhoneStatusFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, phoneStatusFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_log -> {
                logFragment =
                    LogFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, logFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_settings -> {
                settingsFragment =
                    SettingsFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, settingsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                stopServices()
                finish()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
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

//        when (requestCode) {
//            PERMISSION_FOREGROUND_SERVICES_CODE -> {
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//                } else {
//                    toast("Permission denied")
//                }
//            }
//        }
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
    }

}