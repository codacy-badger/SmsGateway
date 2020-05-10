package com.didahdx.smsgatewaysync.services

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.didahdx.smsgatewaysync.utilities.ALTITUDE_EXTRA
import com.didahdx.smsgatewaysync.utilities.LATITUDE_EXTRA
import com.didahdx.smsgatewaysync.utilities.LOCATION_UPDATE_INTENT
import com.didahdx.smsgatewaysync.utilities.LONGITUDE_EXTRA

class LocationGpsService : Service() {
    private var locationListener: LocationListener? = null
    private var locationManager: LocationManager? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onCreate() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val intent = Intent(LOCATION_UPDATE_INTENT)
                intent.putExtra(LONGITUDE_EXTRA, location.longitude.toString())
                intent.putExtra(LATITUDE_EXTRA, location.latitude.toString())
                intent.putExtra(ALTITUDE_EXTRA, location.altitude.toString())

                Log.d("locaitiion", "${location.longitude}  ${location.latitude}")
                println("locaitiion ${location.longitude}  ${location.latitude}")
                sendBroadcast(intent)
            }

            override fun onStatusChanged(
                provider: String,
                status: Int,
                extras: Bundle
            ) {
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 0,
            0f,
            locationListener
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener)
    }
}
