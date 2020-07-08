package com.blueskycharts.app.assests

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream

/**
 * Handles manipulation of the on disk cache.
 * Performs all file system direct manipulation and IO
 */
final class DiskCache(private val context: Context) {
    fun retrieveAssetBytes(asset: Asset): Boolean {
        var fileInput: FileInputStream? = null
        return try {
            fileInput = context.openFileInput(getFilePathForAsset(asset))
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
            val outputStream = context.openFileOutput(getFilePathForAsset(asset), Context.MODE_PRIVATE);
            outputStream.write(asset.bytes)
            outputStream.close()
        } catch ( e: Throwable ) {
            Log.e(null, e.message ?: "Error writing asset to disk ${asset.description.localPath}")
        }
    }

    private fun getFilePathForAsset(asset: Asset) = asset.description.localPath.replace("/", "-").replace(":", "_")
}