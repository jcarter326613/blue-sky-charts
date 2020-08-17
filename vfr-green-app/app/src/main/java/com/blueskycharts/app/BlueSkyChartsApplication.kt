package com.blueskycharts.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.map.assetmanagement.TilePersistenceManagerFactory
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.subscription.SubscriptionChecker

class BlueSkyChartsApplication : SubscriptionChecker() {
    override fun onCreate() {
        super.onCreate()

        DiskCacheFactory(applicationContext)

        Inventory.instance

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        TilePersistenceManagerFactory(connectivityManager)
    }
}