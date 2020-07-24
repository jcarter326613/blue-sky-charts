package com.blueskycharts.app.assests

import java.io.File

class DiskAssetDescription(private val file: File): AssetDescription(Volatility.Indefinite, StorageLocation.Internal) {
    override val localPath: String
        get() = file.name
    override val hasDiskFriendlyLocalPath: Boolean = true
    val size: Long
        get() = file.length()
    val modDate: Long
        get() = file.lastModified()
}