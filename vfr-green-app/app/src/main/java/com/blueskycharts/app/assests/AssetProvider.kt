package com.blueskycharts.app.assests

import android.content.Context
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.URL

/**
 * Provides access to assets whether on disk or remotely stored
 */
open class AssetProvider() {
    open fun retrieveAsset(assetDescription: AssetDescription, callback: ((asset: Asset) -> Unit)) {
        try {
            val newAsset = Asset(assetDescription)
            if ( DiskCacheFactory.instance.retrieveAssetBytes(newAsset) ) {
                callback(newAsset)
                return
            }
        } catch ( e: Throwable ) {
            Log.e(null, "Error processing file for local path ${assetDescription.localPath}")
        }
        assetDescription.retrieveFromSource(callback)
    }
}