package com.blueskycharts.app.assests

abstract class AssetDescription( val volatility: Volatility, val storageLocation: StorageLocation ) {
    var enforceLocationPreference: Boolean = false

    abstract val localPath: String
    abstract fun retrieveFromSource(callback: ((asset: Asset) -> Unit))
}