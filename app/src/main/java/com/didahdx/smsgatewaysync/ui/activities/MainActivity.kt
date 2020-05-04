package com.didahdx.smsgatewaysync.ui.activities


import android.Manifest
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.receiver.BatteryReceiver
import com.didahdx.smsgatewaysync.receiver.ConnectionReceiver
import com.didahdx.smsgatewaysync.receiver.PhoneCallReceiver
import com.didahdx.smsgatewaysync.receiver.SmsReceiver
import com.didahdx.smsgatewaysync.services.AppServices
import com.didahdx.smsgatewaysync.ui.IMainActivity

import com.didahdx.smsgatewaysync.utilities.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_layout.*


class MainActivity : AppCompatActivity(){

    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    var isReadyToPublish: Boolean = false
    var mIMainActivity: IMainActivity? = null
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout=findViewById(R.id.drawer_layout)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment2)
        NavigationUI.setupWithNavController(navigation_view, navController)
        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)

//        navigation_view.setNavigationItemSelectedListener(this)
//        toolbar?.setNavigationOnClickListener {
//            navController.navigateUp()
//        }

        AppCenter.start(
            application, "e55e44cf-eac5-49fc-a785-d6956b386176",
            Analytics::class.java, Crashes::class.java
        )

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)


//        val drawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
//            this,
//            drawer_layout, toolbar,
//            R.string.open,
//            R.string.close
//        ) {
//
//        }
//
//        drawerToggle.isDrawerIndicatorEnabled = true
//        drawer_layout.addDrawerListener(drawerToggle)
//        drawerToggle.syncState()


        //registering broadcast receiver for battery
        this.registerReceiver(
            BatteryReceiver(),
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        //registering broadcast receiver for smsReceiver
        this.registerReceiver(
            SmsReceiver(), IntentFilter(SMS_RECEIVED_INTENT)
        )

        //registering broadcast receiver for connection
        this.registerReceiver(
            ConnectionReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        val callFilter = IntentFilter("android.intent.action.NEW_OUTGOING_CALL")
        callFilter.addAction("android.intent.action.PHONE_STATE")

        //registering broadcast receiver for callReceiver
        baseContext.registerReceiver(
            PhoneCallReceiver(), callFilter
        )

        //saving apps preference
        PreferenceManager.setDefaultValues(
            this,
            R.xml.preferences, false
        )

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


//    override fun onBackPressed() {
//        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
//            drawer_layout.closeDrawer(GravityCompat.START)
//        } else {
//            super.onBackPressed()
//        }
//    }


    override fun onSupportNavigateUp(): Boolean {
//        return  Navigation.findNavController(this, R.id.nav_host_fragment2).navigateUp()
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
//        val isServiceRunning = sharedPreferences.getBoolean(PREF_SERVICES_KEY, true)
//        if(isServiceRunning){
//            stopServices()
//        }

//        this.unregisterReceiver(SmsReceiver())
    }

    //used to display alert dialog box
    private fun showDialog(
        title: String, msg: String, postiveLabel: String,
        postiveOnClick: DialogInterface.OnClickListener,
        negativeLabel: String, negativeOnClick: DialogInterface.OnClickListener,
        isCancelable: Boolean
    ): AlertDialog {

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setCancelable(isCancelable)
        builder.setMessage(msg)
        builder.setPositiveButton(postiveLabel, postiveOnClick)
        builder.setNegativeButton(negativeLabel, negativeOnClick)
        val alert = builder.create()
        alert.show()
        return alert;
    }



}