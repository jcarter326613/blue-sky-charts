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
import com.blueskycharts.app.map.view.NavigableMap2d
import com.blueskycharts.app.subscription.SubscriptionActivity
import com.blueskycharts.app.subscription.SubscriptionChecker
import com.google.android.gms.location.LocationServices
import com.google.firebase.analytics.FirebaseAnalytics


class MapViewActivity : SubscriptionChecker(false, true) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)

        TilePersistenceManager.getInstance(getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
    }
}