package com.didahdx.smsgatewaysync


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.receiver.BatteryReceiver
import com.didahdx.smsgatewaysync.receiver.ConnectionReceiver
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.ui.*
import com.didahdx.smsgatewaysync.utilities.AppLog
import com.didahdx.smsgatewaysync.utilities.INPUT_EXTRAS
import com.didahdx.smsgatewaysync.utilities.PERMISSION_FOREGROUND_SERVICES_CODE
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.navigation_header.*


class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener, ConnectionReceiver.ConnectionReceiverListener {

    //checks on network connectivity to update the notification bar
    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        checkForegroundPermission()

        if (!isConnected) {
            startServices("No internet connection")
            appLog.writeToLog(this,"No internet Connection")
            Toast.makeText(this,"no net",Toast.LENGTH_LONG).show()
        } else {
            startServices("${getString(R.string.app_name)} is Running")
            appLog.writeToLog(this,"Connected to Internet")
        }
    }

    val appLog= AppLog()
    lateinit var homeFragment: HomeFragment
    lateinit var settingsFragment: SettingsFragment
    lateinit var logFragment:LogFragment
    lateinit var aboutFragment: AboutFragment
    private var mFirebaseAnalytics: FirebaseAnalytics? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCenter.start(
            application, "e55e44cf-eac5-49fc-a785-d6956b386176",
            Analytics::class.java, Crashes::class.java
        )

        checkForegroundPermission()

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        setSupportActionBar(toolbar)

        val drawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer_layout, toolbar, R.string.open, R.string.close
        ) {

        }

        drawerToggle.isDrawerIndicatorEnabled = true
        drawer_layout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        navigation_view.setNavigationItemSelectedListener(this)

        settingUpDefaultFragment()

        //registering the broadcast receiver for network
        baseContext.registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        //registering broadcast receiver for battery
        baseContext.registerReceiver(BatteryReceiver(),
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        App.instance.setConnectionListener(this)

        //saving apps preference
        PreferenceManager.setDefaultValues(this,R.xml.preferences,false)

        val headView: View =navigation_view.getHeaderView(0)
         val navUser:TextView= headView.findViewById<TextView>(R.id.text_view_user_loggedIn)
       val firebaseUser= FirebaseAuth.getInstance().currentUser
        if(firebaseUser!=null){
            navUser.text=firebaseUser.email
        }


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
                homeFragment = HomeFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, homeFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_about->{
                aboutFragment= AboutFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout,aboutFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_log->{
                logFragment= LogFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout,logFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_settings -> {
                settingsFragment = SettingsFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, settingsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }

            R.id.nav_logout->{
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this,LoginActivity::class.java))
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


    private fun startServices(input: String) {
        val serviceIntent = Intent(this, AppServices::class.java)
        serviceIntent.putExtra(INPUT_EXTRAS, input)
        ContextCompat.startForegroundService(this as Activity, serviceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            PERMISSION_FOREGROUND_SERVICES_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(BatteryReceiver())
        super.onDestroy()
    }
}