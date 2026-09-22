package com.blueskycharts.app.map

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.blueskycharts.app.BlueSkyChartsApplication
import com.blueskycharts.app.R
import com.blueskycharts.app.map.models.OverlayViewModel
import com.blueskycharts.app.map.view.NavigableMap2d
import com.blueskycharts.app.utility.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class NavigableMapFragment: Fragment() {
    private val requestCode: Int = com.blueskycharts.app.Constants.geoLocationRequestCode
    var mapView: NavigableMap2d? = null
        private set
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationManager: LocationManager? = null
    private val locationUpdatesCallback = LocationUpdatesCallback(null, null)

    interface LocationUpdateListener {
        fun locationUpdated()
    }

    fun setExtraLocationUpdateListener(newListener: LocationUpdateListener?) {
        locationUpdatesCallback.extraLocationUpdateListener = newListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_navigable_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = this.activity
        if ( activity != null ) {
            val overlayModel = ViewModelProvider(activity).get(OverlayViewModel::class.java)

            overlayModel.getOverlayType().observe(viewLifecycleOwner, Observer {
                mapView?.setOverlayType(it)
            })
        } else {
            Log.error(null, "Loaded view without activity.  Can not connect view model.")
        }
    }

    override fun onResume() {
        super.onResume()

        val mapView = view?.findViewById<NavigableMap2d>(R.id.navigableMap2d)
        this.mapView = mapView
        locationUpdatesCallback.mapView = mapView
        setupLocationUpdatePermissions()
    }

    private fun setupLocationUpdatePermissions() {
        val context = this.context ?: return
        val activity = this.activity ?: return

        when (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
                getLocation()
                setupLocationUpdates()
            }
            else -> {
                val application = activity.application as BlueSkyChartsApplication
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    val alertDialog = AlertDialog.Builder(context, R.style.AlertTheme)
                        .setMessage("Your location will not display on the maps unless this app is granted permissions to view your current location.  If you do not want this feature, you may deny location permissions.")
                        .create()
                    alertDialog.show()
                    alertDialog.setOnDismissListener {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            requestCode
                        )
                        application.alreadyAskedLocationPermission = true
                    }
                } else if (!application.alreadyAskedLocationPermission) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        requestCode
                    )
                    application.alreadyAskedLocationPermission = true
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            setupLocationUpdatePermissions()
        }
    }

    override fun onPause() {
        super.onPause()

        this.mapView = null
        locationUpdatesCallback.mapView = null

        this.fusedLocationClient?.removeLocationUpdates(locationUpdatesCallback)
        locationUpdatesCallback.let{ this.locationManager?.removeUpdates(it) }
    }

    private fun getLocation() {
        // Get the current location
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location == null) {
                    return@addOnSuccessListener
                }
                mapView?.updateCurrentLocation(location)
            }
        } catch (e: SecurityException) {
        }
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationRequest?.let {it ->
            val activity = this.activity ?: return@let
            val builder = LocationSettingsRequest.Builder().addLocationRequest(it)
            val client: SettingsClient = LocationServices.getSettingsClient(activity)
            client.checkLocationSettings(builder.build())
                .addOnSuccessListener {
                    try {
                        fusedLocationClient?.requestLocationUpdates(
                            locationRequest,
                            locationUpdatesCallback,
                            Looper.getMainLooper()
                        )
                    } catch (e: SecurityException) {
                    }
                }
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException){
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            exception.startResolutionForResult(activity, requestCode)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Ignore the error.
                        }
                    } else {
                        if (locationManager == null) {
                            locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        }
                        val gpsEnabled = try {
                            // "Checking for GPS support"
                            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
                        } catch (e: Throwable) {
                            false
                        }

                        if (gpsEnabled)
                        {
                            val context = this.context ?: return@addOnFailureListener

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return@addOnFailureListener
                            }
                            locationUpdatesCallback.let { it ->
                                locationManager?.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    0f,
                                    it
                                )
                            }
                        }
                    }
                }
        }
    }

    private class LocationUpdatesCallback(var mapView: NavigableMap2d?, var extraLocationUpdateListener: LocationUpdateListener?) : LocationCallback(), LocationListener {
        override fun onLocationResult(p0: LocationResult?) {
            super.onLocationResult(p0)
            p0?.lastLocation?.let { location ->
                mapView?.updateCurrentLocation(location)
            }
            extraLocationUpdateListener?.locationUpdated()
        }

        override fun onLocationChanged(p0: Location) {
            p0.let { location ->
                mapView?.updateCurrentLocation(location)
            }
            extraLocationUpdateListener?.locationUpdated()
        }
    }
}