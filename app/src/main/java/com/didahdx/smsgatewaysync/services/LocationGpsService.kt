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
import androidx.core.app.ActivityCompat
import com.didahdx.smsgatewaysync.utilities.*
import timber.log.Timber

class LocationGpsService : Service() {
    private var locationListener: LocationListener? = null
    private var locationManager: LocationManager? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_REDELIVER_INTENT
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onLowMemory() { //Send broadcast to the Activity to kill this service and restart it.
        super.onLowMemory()
    }


    override fun onCreate() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val intent = Intent(LOCATION_UPDATE_INTENT)
                intent.putExtra(LONGITUDE_EXTRA, location.longitude.toString())
                intent.putExtra(LATITUDE_EXTRA, location.latitude.toString())
                intent.putExtra(ALTITUDE_EXTRA, location.altitude.toString())

                Timber.d("Gps Location ${location.latitude} ${location.longitude}   ${location.altitude} ")
                println("Gps Location  ${location.latitude}  ${location.longitude} ${location.altitude}  ")
                sendBroadcast(intent)
            }

            override fun onStatusChanged(
                provider: String,
                status: Int,
                extras: Bundle
            ) {

                Timber.d("Location status changed $provider $status $extras")
            }

            override fun onProviderEnabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    locationManager?.removeUpdates(locationListener)
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, MINIMUM_TIME,
                        0f,
                        locationListener
                    )
                }
                Timber.d("Location provider Enabled $provider")
            }

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    locationManager?.removeUpdates(locationListener)
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                    locationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, MINIMUM_TIME,
                        0f,
                        locationListener
                    )

                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, MINIMUM_TIME,
                        0f,
                        locationListener
                    )
                }

//                Timber.d("Location provider Disabled $provider")
            }
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


        locationManager?.let { locationManager ->
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            Timber.d("GPS Enabled $isGPSEnabled Network Enable $isNetworkEnabled")


            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, MINIMUM_TIME,
                0f,
                locationListener
            )

            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, MINIMUM_TIME,
                0f,
                locationListener
            )

            if (!isGPSEnabled && !isNetworkEnabled){
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }


    }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener)
    }

}
