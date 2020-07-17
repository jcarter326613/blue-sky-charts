package com.blueskycharts.app.assests

import android.content.Context
import android.util.Log
import com.blueskycharts.app.map.assetmanagement.Manifest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream

/**
 * Handles manipulation of the on disk cache.
 * Performs all file system direct manipulation and IO
 */
final class DiskCache(private val context: Context) {
    private var aliasCollection: PersistentFile<AliasCollection>

    init {
        val aliasFileDescriptor =
            LocalAssetDescription("disk_cache_aliases", Volatility.Indefinite)
        val aliasAsset = Asset(aliasFileDescriptor)
        aliasCollection = if ( retrieveAssetBytes(aliasAsset) ) {
            val reader = aliasAsset.asJsonReader()
            if ( reader != null ) {
                val collection = AliasCollection.readFromJsonReader(reader)
                PersistentFile(collection, aliasFileDescriptor)
            } else {
                PersistentFile(AliasCollection(), aliasFileDescriptor)
            }
        } else {
            PersistentFile(AliasCollection(), aliasFileDescriptor)
        }
    }

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

    fun createAlias(existingObject: AssetDescription, newAlias: AssetDescription) {
        GlobalScope.launch {    //ok1
            aliasCollection.access {
                // Get the paths out of the parameters
                val existingLocation = existingObject.localPath
                val newLocation = newAlias.localPath

                // Determine if the existing object is actually aliased and points elsewhere
                var actualOriginal = it.aliasFiles[existingLocation]
                if (actualOriginal == null) {
                    actualOriginal = existingLocation
                }

                // Update the alias pointers and add the new item
                it.aliasFiles[newLocation] = actualOriginal
                var actualFilesSet = it.actualFiles[actualOriginal]
                if (actualFilesSet == null) {
                    actualFilesSet = mutableSetOf()
                    it.actualFiles[actualOriginal] = actualFilesSet
                }
                return@access actualFilesSet.add(newLocation)
            }
        }
    }

    private fun getFilePathForAsset(asset: Asset) = asset.description.localPath.replace("/", "-").replace(":", "_")
}