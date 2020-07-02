package com.didahdx.smsgatewaysync.presentation.activities

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.utilities.*


class LoginActivity : AppCompatActivity() {
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController)
        setServiceState(this, ServiceState.STOPPED)
    }

    override fun onSupportNavigateUp(): Boolean {
        navController.navigateUp()
        return super.onSupportNavigateUp()
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
            PERMISSION_READ_SMS_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toast("Permission granted to read sms")
                } else {
                    toast("Permission denied to read sms")
                }
            }
            PERMISSION_FOREGROUND_SERVICES_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    toast("Permission denied")
                }
            }

        }
    }

}
