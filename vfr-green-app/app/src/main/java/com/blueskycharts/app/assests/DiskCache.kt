package com.blueskycharts.app.assests

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import com.blueskycharts.app.utility.Log

/**
 * Handles manipulation of the on disk cache.
 * Performs all file system direct manipulation and IO
 */
final class DiskCache(private val context: Context) {
    var externalCheckFilePerformed = false
    var externalCheckFileSuccess = false
    val isExternalStorageWritable: Boolean
        get() {
            return false /*
            return if ( Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED ) {
                if ( externalCheckFilePerformed ) {
                    externalCheckFileSuccess
                } else {
                    val externalDirectory = context.getExternalFilesDir(null)
                    val file = File(externalDirectory, "writeTest")
                    externalDirectory?.mkdirs()

                    val outputStream = FileOutputStream(file)
                    outputStream.write("1".toByteArray())
                    outputStream.close()

                    val inputStream = FileInputStream(file)
                    val evidence = String(inputStream.readBytes())
                    inputStream.close()

                    externalCheckFilePerformed = true
                    externalCheckFileSuccess = evidence == "1"
                    file.delete()
                    externalCheckFileSuccess
                }
            } else {
                false
            }*/
        }
    val isExternalStorageReadable: Boolean
        get() {
            return false
            //return Environment.getExternalStorageState() in setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
        }
    private var aliasCollection: PersistentFile<AliasCollection>

    init {
        val aliasFileDescriptor =
            LocalAssetDescription("disk_cache_aliases", Volatility.Indefinite, StorageLocation.Internal)
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

    fun isExpired(assetDescription: AssetDescription): Boolean {
        when(assetDescription.volatility) {
            Volatility.Indefinite -> return false
            Volatility.NeverCache -> return true
            Volatility.DayCache -> {
                val now = Date()
                val aDayAgo = now.time - (24 * 60 * 60 * 1000)
                return try {
                    if ( assetDescription.storageLocation == StorageLocation.External ) {
                        val file = File(context.getExternalFilesDir(null), getFilePathForAsset(assetDescription))
                        file.lastModified() < aDayAgo
                    } else {
                        context.getFileStreamPath(getFilePathForAsset(assetDescription)).lastModified() < aDayAgo
                    }
                } catch (e: Throwable) {
                    true
                }
            }
            else -> throw Error("Unrecognized volatility in isExpired")
        }
    }

    fun retrieveAssetBytes(asset: Asset): Boolean {
        var fileInput: FileInputStream? = null
        return try {
            fileInput = if ( asset.description.storageLocation == StorageLocation.External && isExternalStorageReadable ) {
                val file = File(context.getExternalFilesDir(null), getFilePathForAsset(asset))
                if (file.exists()) {
                    file.inputStream()
                } else {
                    context.openFileInput(getFilePathForAsset(asset))
                }
            } else {
                context.openFileInput(getFilePathForAsset(asset))
            }
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
            val outputStream =
                if ( asset.description.storageLocation == StorageLocation.External && isExternalStorageWritable ) {
                    val externalDirectory = context.getExternalFilesDir(null)
                    val file = File(externalDirectory, getFilePathForAsset(asset))
                    if (!file.exists()) {
                        externalDirectory?.mkdirs()
                    } else {
                        file.delete()
                    }
                    FileOutputStream(file)
                } else {
                    context.openFileOutput(getFilePathForAsset(asset), Context.MODE_PRIVATE)
                }

            // Write the file to disk
            outputStream.write(asset.bytes)
            outputStream.close()
        } catch ( e: Throwable ) {
            Log.e(null, e.message ?: "Error writing asset to disk ${asset.description.localPath}")
        }
    }

    fun deleteAsset(description: AssetDescription) {
        try {
            if (description.storageLocation == StorageLocation.External) {
                val file = File(context.getExternalFilesDir(null), getFilePathForAsset(description))
                if (file.exists()) {
                    file.delete()
                }
            } else {
                context.deleteFile(getFilePathForAsset(description))
            }
        } catch (e: Throwable) {
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

    suspend fun isAlias(description: AssetDescription): Boolean {
        var response: Boolean? = null
        aliasCollection.access {
            response = description.localPath in it.aliasFiles
            false
        }
        return response as Boolean
    }

    fun getFileSize(description: AssetDescription): Long {
        return try {
            if ( description.storageLocation == StorageLocation.External ) {
                val file = File(context.getExternalFilesDir(null), getFilePathForAsset(description))
                file.length()
            } else {
                context.getFileStreamPath(getFilePathForAsset(description)).length()
            }
        } catch (e: Throwable) {
            0
        }
    }

    private fun getFilePathForAsset(asset: Asset) = getFilePathForAsset(asset.description)
    private fun getFilePathForAsset(description: AssetDescription): String {
        return description.localPath.replace("/", "-").replace(":", "_")
    }
}