package com.blueskycharts.app.map

import android.Manifest
import android.R.attr.name
import android.R.id
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blueskycharts.app.Constants
import com.blueskycharts.app.R
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManager
import com.google.firebase.analytics.FirebaseAnalytics


class MapViewActivity : AppCompatActivity() {
    private val requestCode: Int = Constants.geoLocationRequestCode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)

        TilePersistenceManager.getInstance(getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)

        requestLocationPermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            this.requestCode -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    val alertDialog = AlertDialog.Builder(this)
                        .setMessage("You were just given permissions")
                        .create()
                    alertDialog.show()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    val alertDialog = AlertDialog.Builder(this)
                        .setMessage("Permissions were just taken away")
                        .create()
                    alertDialog.show()
                }
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun requestLocationPermissions() {
        when (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                val alertDialog = AlertDialog.Builder(this)
                    .setMessage("You have permissions already")
                    .create()
                alertDialog.show()
            }
            else -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    val alertDialog = AlertDialog.Builder(this)
                        .setMessage("We want permissions pretty please.")
                        .create()
                    alertDialog.show()
                    alertDialog.setOnDismissListener {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode )
                    }
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode )
                }
            }
        }
    }
}