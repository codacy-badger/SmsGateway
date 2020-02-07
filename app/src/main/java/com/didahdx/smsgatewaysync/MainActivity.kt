package com.didahdx.smsgatewaysync


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentTransaction
import com.didahdx.smsgatewaysync.ui.HomeFragment
import com.didahdx.smsgatewaysync.ui.SettingsFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_layout.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var homeFragment: HomeFragment
    lateinit var settingsFragment: SettingsFragment
    private val PERMISSION_RECEIVE_SMS_CODE = 2
    private val PERMISSION_READ_SMS_CODE=100
    private val PERMISSION_WRITE_EXTERNAL_STORAGE_CODE=500


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        checkPermission()

        settingUpDefaultFragment()
    }

    private fun checkPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                PERMISSION_RECEIVE_SMS_CODE
            )
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                PERMISSION_READ_SMS_CODE
            )
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
            R.id.nav_settings -> {
                settingsFragment = SettingsFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, settingsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_RECEIVE_SMS_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   settingUpDefaultFragment()
                }else{
                    Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            PERMISSION_READ_SMS_CODE->{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    settingUpDefaultFragment()
                }else{
                    Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            PERMISSION_WRITE_EXTERNAL_STORAGE_CODE->{
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"Permission granted",Toast.LENGTH_LONG).show()
                }

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
}
