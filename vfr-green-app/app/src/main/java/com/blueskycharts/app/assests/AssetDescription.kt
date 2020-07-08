package com.blueskycharts.app.assests

abstract class AssetDescription( val volatility: Volatility ) {
    abstract val localPath: String
    abstract fun retrieveFromSource(callback: ((asset: Asset) -> Unit))
}