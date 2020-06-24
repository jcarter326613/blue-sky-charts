package com.blueskycharts.app.remoteassests

import android.content.Context
import android.util.JsonReader
import android.util.Log
import com.blueskycharts.app.map.models.SubMapModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

class AssetProvider(private val context: Context) {
    private val diskCache = DiskCache(context)

    fun retrieveAsset(url: URL, volatility: Volatility, requiresCors: Boolean = false, callback: ((asset: Asset) -> Unit)) {
        var asset = Asset(url, volatility)
        asset.requiresCors = requiresCors
        if ( diskCache.retrieveAssetBytes(asset) ) {
            callback(asset)
            return
        }
        download(asset, callback)
    }

    private fun download(asset: Asset, callback: ((asset: Asset) -> Unit)) {
        GlobalScope.launch {
            try {
                // Download the file to memory
                val connection = asset.url.openConnection()
                if ( asset.requiresCors ) connection.setRequestProperty("origin", "https://blueskycharts.com/")
                val iStream: InputStream = connection.getInputStream()
                val fileContent = iStream.readBytes()
                iStream.close()
                asset.bytes = fileContent

                // Write the file to disk
                when (asset.volatility) {
                    Volatility.DayCache,
                    Volatility.Indefinite,
                    Volatility.ScheduledLifetime -> {
                        diskCache.writeAsset(asset)
                    }
                    else -> {
                    }
                }

                // Call the callback
                callback(asset)
            } catch (e: Throwable) {
                Log.e(null, e.message ?: "Error downloading file ${asset.url}")
                asset.errorLoading = true
                callback(asset)
            }
        }
    }
}