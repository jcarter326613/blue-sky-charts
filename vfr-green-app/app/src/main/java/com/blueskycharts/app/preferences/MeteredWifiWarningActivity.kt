package com.blueskycharts.app.preferences

import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import android.widget.TextView
import com.blueskycharts.app.BlueSkyChartsActivity

open class MeteredWifiWarningActivity(private val wifiWarningViewId: Int) : BlueSkyChartsActivity(false, true) {

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        val wifiWarningView = findViewById<TextView>(wifiWarningViewId)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.networkPreference = ConnectivityManager.TYPE_WIFI
        val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val onWifi = (networkInfo?.isConnected ?: false) && !connectivityManager.isActiveNetworkMetered

        if (onWifi) {
            wifiWarningView?.visibility = View.GONE
        } else {
            wifiWarningView?.visibility = View.VISIBLE
        }
    }
}