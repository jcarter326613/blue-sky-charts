package com.blueskycharts.app.assests

import android.content.Context
import android.util.AttributeSet
import android.view.View

class AssetProviderFactory {
    companion object {
        private var _instance: AssetProvider? = null
        val instance: AssetProvider
            get() {
                if ( _instance == null ) {
                    _instance = AssetProvider()
                }
                return _instance ?: throw Error("Error trying to access asset provider instance")
            }
    }
}