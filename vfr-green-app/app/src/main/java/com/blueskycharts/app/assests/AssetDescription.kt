package com.blueskycharts.app.assests

abstract class AssetDescription( val volatility: Volatility, val storageLocation: StorageLocation ) {
    abstract val localPath: String
    abstract val hasDiskFriendlyLocalPath: Boolean
    abstract val readActsAsModification: Boolean
    var allowExpired: Boolean = false
    var forceExpireDiskCache: Boolean = false
}