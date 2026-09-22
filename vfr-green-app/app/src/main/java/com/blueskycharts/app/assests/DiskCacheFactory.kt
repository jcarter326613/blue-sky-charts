package com.blueskycharts.app.assests

import android.content.Context
import android.util.AttributeSet
import android.view.View

class DiskCacheFactory(context: Context) {
    init {
        if ( _instance == null ) {
            _instance = DiskCache(context.applicationContext)
        }
    }

    companion object {
        private var _instance: DiskCache? = null
        val instance: DiskCache
            get() {
                return _instance ?: throw Error("Error trying to access disk cache instance prior to initialization")
            }
        val ready: Boolean
            get() = _instance != null
    }
}