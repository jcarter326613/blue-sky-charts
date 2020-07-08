package com.blueskycharts.app.assests

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.URL

class RemoteAssetDescription(private val url: URL, volatility: Volatility, private var requiresCors: Boolean = false) : AssetDescription(volatility) {
    override val localPath: String
        get() = "${url.host}/${url.path}/${url.query}"

    override fun retrieveFromSource(callback: ((asset: Asset) -> Unit))
    {
        GlobalScope.launch {
            val asset = Asset(this@RemoteAssetDescription)
            try {
                // Download the file to memory
                val connection = url.openConnection()
                if ( requiresCors ) connection.setRequestProperty("origin", "https://blueskycharts.com/")
                val iStream: InputStream = connection.getInputStream()
                val fileContent = iStream.readBytes()
                iStream.close()
                asset.bytes = fileContent

                // Write the file to disk
                when (volatility) {
                    Volatility.DayCache,
                    Volatility.Indefinite -> {
                        DiskCacheFactory.instance.writeAsset(asset)
                    }
                    else -> {
                    }
                }

                // Call the callback
                callback(asset)
            } catch (e: Throwable) {
                Log.e(null, e.message ?: "Error downloading file $url")
                asset.errorLoading = true
                callback(asset)
            }
        }
    }
}