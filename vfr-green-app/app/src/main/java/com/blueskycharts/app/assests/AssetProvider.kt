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
            if (DiskCacheFactory.instance.retrieveAssetBytes(newAsset)) {
                try {
                    callback(newAsset)
                } catch (e: Throwable) {
                    Log.e(null, "Unknown error in AssetProvider after retrieving an asset from disk successfully.")
                }
                return
            }
        } catch (e: Throwable) {
            Log.e(null, "Error processing file for local path ${assetDescription.localPath}")
        }
        assetDescription.retrieveFromSource(callback)
    }

    open fun retrieveLocalAsset(assetDescription: LocalAssetDescription): Asset {
        val newAsset = Asset(assetDescription)
        if (DiskCacheFactory.instance.retrieveAssetBytes(newAsset)) {
            return newAsset
        }
        newAsset.errorLoading = true
        return newAsset
    }
}