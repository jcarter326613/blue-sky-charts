package com.blueskycharts.app.assests

import com.blueskycharts.app.utility.Log

/**
 * Provides access to assets whether on disk or remotely stored
 */
open class AssetProvider() {
    open fun retrieveAsset(assetDescription: RemoteAssetDescription, callback: ((asset: Asset) -> Unit)) {
        if (DiskCacheFactory.instance.isExpired(assetDescription)) {
            assetDescription.retrieveFromSource {
                if (!it.errorLoading) {
                    callback(it)
                } else {
                    val newAsset = retrieveLocalAsset(assetDescription)
                    callback(newAsset)
                }
            }
        } else {
            try {
                val newAsset = retrieveLocalAsset(assetDescription)
                if (!newAsset.errorLoading) {
                    callback(newAsset)
                    return
                }
            } catch (e: Throwable) {
                Log.e(null, "Error processing file for local path ${assetDescription.localPath}")
            }
            assetDescription.retrieveFromSource(callback)
        }
    }

    open fun retrieveLocalAsset(assetDescription: AssetDescription): Asset {
        val newAsset = Asset(assetDescription)
        if (DiskCacheFactory.instance.retrieveAssetBytes(newAsset)) {
            return newAsset
        }
        newAsset.errorLoading = true
        return newAsset
    }
}