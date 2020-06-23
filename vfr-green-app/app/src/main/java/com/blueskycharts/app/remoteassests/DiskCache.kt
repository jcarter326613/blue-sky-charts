package com.blueskycharts.app.remoteassests

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream

class DiskCache(private val context: Context) {
    fun retrieveAssetBytes(asset: Asset): Boolean {
        var fileInput: FileInputStream? = null
        return try {
            fileInput = context.openFileInput(getFileForAsset(asset).path.replace("/", "-"))
            asset.bytes = fileInput.readBytes()
            true
        } catch ( e: Throwable ) {
            false
        } finally {
            fileInput?.close()
        }
    }

    fun writeAsset(asset: Asset) {
        try {
            // Write the file to disk
            val outputStream = context.openFileOutput(getFileForAsset(asset).path.replace("/", "-"), Context.MODE_PRIVATE);
            outputStream.write(asset.bytes)
            outputStream.close()
        } catch ( e: Throwable ) {
            Log.e(null, e.message ?: "Error writing asset to disk ${asset.localPath}")
        }
    }

    private fun getFileForAsset(asset: Asset) = File(context.filesDir, asset.localPath)
}