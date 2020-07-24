package com.blueskycharts.app.assests

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.URL
import com.blueskycharts.app.utility.Log

class RemoteAssetDescription(private val url: URL, volatility: Volatility, storageLocation: StorageLocation, private val requiresCors: Boolean = false, private val isFolder: Boolean = false) :
    AssetDescription(volatility, storageLocation) {
    override val localPath: String
        get() {
            return if (isFolder) {
                "${url.host}${url.path}/"
            } else {
                "${url.host}${url.path}/${url.query}"
            }
        }
    override val hasDiskFriendlyLocalPath: Boolean = false

    fun retrieveFromSource(callback: ((asset: Asset) -> Unit))
    {
        GlobalScope.launch {    //ok1
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
                try {
                    callback(asset)
                } catch ( e: Throwable ) {
                    Log.e(null, e.message ?: "Unknown error after successfully writing asset to disk after retrieval")
                    asset.errorLoading = true
                    callback(asset)
                }
            } catch (e: Throwable) {
                Log.e(null, e.message ?: "Error downloading file $url")
                asset.errorLoading = true
                callback(asset)
            }
        }
    }
}