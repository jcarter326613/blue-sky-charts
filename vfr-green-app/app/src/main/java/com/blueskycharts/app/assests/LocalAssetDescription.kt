package com.blueskycharts.app.assests

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class LocalAssetDescription( private val path: String, volatility: Volatility, storageLocation: StorageLocation ) : AssetDescription(volatility, storageLocation) {
    override val localPath: String
        get() = path
    override val hasDiskFriendlyLocalPath: Boolean = false
}