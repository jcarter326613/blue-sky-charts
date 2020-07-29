package com.blueskycharts.app.map

import android.Manifest
import android.app.AlertDialog
import android.content.IntentSender
import android.content.pm.PackageManager
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
import com.blueskycharts.app.R
import com.blueskycharts.app.map.models.OverlayViewModel
import com.blueskycharts.app.map.view.NavigableMap2d
import com.blueskycharts.app.utility.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class NavigableMapFragment: Fragment() {
    private val requestCode: Int = com.blueskycharts.app.Constants.geoLocationRequestCode
    private var mapView: NavigableMap2d? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_navigable_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = this.activity
        if ( activity != null ) {
            val overlayModel = ViewModelProvider(activity).get(OverlayViewModel::class.java)
            mapView = view.findViewById<NavigableMap2d>(R.id.navigableMap2d)

            overlayModel.getOverlayType().observe(viewLifecycleOwner, Observer {
                mapView?.setOverlayType(it)
            })
        } else {
            Log.error(null, "Loaded view without activity.  Can not connect view model.")
        }

        requestLocationPermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val context = this.context ?: return
        when (requestCode) {
            this.requestCode -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLocation()
                } else {
                    val alertDialog = AlertDialog.Builder(context)
                        .setMessage("We have detected that location services permissions were denied for this app.  Your location will not be displayed on the map.")
                        .create()
                    alertDialog.show()
                }
            }
            else -> {
            }
        }
    }

    private fun requestLocationPermissions() {
        val context = this.context
        val activity = this.activity
        if (context == null || activity == null ) {
            return
        }

        when (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                getLocation()
            }
            else -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    val alertDialog = AlertDialog.Builder(context)
                        .setMessage("Your location will not display on the maps unless this app is granted permissions to view your current location.  If you do not want this feature, you may deny location permissions.")
                        .create()
                    alertDialog.show()
                    alertDialog.setOnDismissListener {
                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode )
                    }
                } else {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode )
                }
            }
        }
    }

    private fun getLocation() {
        // Get the current location
        val activity = this.activity ?: return
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    return@addOnSuccessListener
                }
                mapView?.updateCurrentLocation(location)
                setupLocationUpdates()
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
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
                .addOnSuccessListener {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
                    try {
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            object: LocationCallback() {
                                override fun onLocationResult(p0: LocationResult?) {
                                    super.onLocationResult(p0)
                                    p0?.lastLocation?.let { location ->
                                        mapView?.updateCurrentLocation(location)
                                    }
                                }
                            },
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
                    }
                }
        }
    }
}