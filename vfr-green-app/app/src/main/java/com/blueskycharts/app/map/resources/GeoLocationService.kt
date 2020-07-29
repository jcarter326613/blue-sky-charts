package com.blueskycharts.app.map.resources

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import kotlin.random.Random


class GeoLocationService: Service() {
    override fun onCreate() {
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}