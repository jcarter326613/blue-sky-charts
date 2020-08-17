package com.blueskycharts.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManagerFactory

class BlueSkyChartsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        DiskCacheFactory(applicationContext)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        TilePersistenceManagerFactory(connectivityManager)
    }
}