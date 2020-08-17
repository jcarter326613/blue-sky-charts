package com.blueskycharts.app.map.assetmanagement

import android.net.ConnectivityManager
import com.blueskycharts.app.assests.DiskCache

class TilePersistenceManagerFactory(cm: ConnectivityManager) {
    init {
        if ( _instance == null ) {
            _instance = TilePersistenceManager(cm)
        }
    }

    companion object {
        private var _instance: TilePersistenceManager? = null
        val instance: TilePersistenceManager
            get() {
                return _instance ?: throw Error("Error trying to access disk cache instance prior to initialization")
            }
        val ready: Boolean
            get() = _instance != null
    }
}