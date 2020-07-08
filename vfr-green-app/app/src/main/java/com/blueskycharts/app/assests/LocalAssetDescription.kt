package com.blueskycharts.app.assests

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LocalAssetDescription( private val path: String, volatility: Volatility ) : AssetDescription(volatility) {
    override val localPath: String
        get() = path

    override fun retrieveFromSource(callback: ((asset: Asset) -> Unit)) {
        callback(Asset(this))
    }
}