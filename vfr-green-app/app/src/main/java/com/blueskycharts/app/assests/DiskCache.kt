package com.blueskycharts.app.assests

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import com.blueskycharts.app.utility.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles manipulation of the on disk cache.
 * Performs all file system direct manipulation and IO
 */
final class DiskCache(private val context: Context) {
    // External storage stuff
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

    // Alias stuff
    private var aliasCollection: PersistentFile<AliasCollection>

    // Disk statistics stuff
    private val statisticsListeners = mutableListOf<StatisticsListener>()
    private val statisticsListenersMutex = Mutex()

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

    fun exists(description: AssetDescription): Boolean {
        val file = getFile(description)
        return file?.exists() ?: false
    }

    fun retrieveAssetBytes(asset: Asset): Boolean {
        var fileInput: FileInputStream? = null
        try {
            val file = getFile(asset.description) ?: return false
            file.setLastModified(Date().time)
            fileInput = file.inputStream()
            asset.bytes = fileInput.readBytes()
            return true
        } catch ( e: Throwable ) {
            return false
        } finally {
            fileInput?.close()
        }
    }

    suspend fun writeAsset(asset: Asset) {
        try {
            statisticsListenersMutex.withLock {
                // Get the file object to the destination
                val file = getFile(asset.description) ?: throw Error("Could not create file object for asset ${asset.description.localPath}")

                // If the file exists, record how much we aren't increasing disk usage by
                var existingFileSize = 0L
                if (file.exists()) {
                    existingFileSize = file.length()
                }

                // Write the file to disk
                val outputStream = file.outputStream()
                outputStream.write(asset.bytes)
                outputStream.close()

                // Tell the appropriate listeners
                for ( listener in statisticsListeners ) {
                    if ( file.name.startsWith(listener.prefix) ) {
                        listener.diskUsage += file.length() - existingFileSize
                        listener.listener.totalSizeChanged(listener.diskUsage)
                    }
                }
            }
        } catch ( e: Throwable ) {
            Log.e(null, e.message ?: "Error writing asset to disk ${asset.description.localPath}")
        }
    }

    fun deleteAsset(description: AssetDescription) {
        try {
            // TODO: make sure when we do deletion of aliases and renaming the file, we don't update the mod date on the file
            val file = getFile(description) ?: return
            val fileSize = file.length()
            if (file.delete()) {
                // Tell the appropriate listeners
                GlobalScope.launch {
                    statisticsListenersMutex.withLock {
                        for ( listener in statisticsListeners ) {
                            if ( file.name.startsWith(listener.prefix) ) {
                                listener.diskUsage -= fileSize
                                listener.listener.totalSizeChanged(listener.diskUsage)
                            }
                        }
                    }
                }
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

    fun addListener(parentDescription: AssetDescription, newListener: DiskCacheListener) {
        GlobalScope.launch {
            statisticsListenersMutex.withLock {
                val existingFiles = getAssetDescriptionsIn(parentDescription)
                var fileSize = 0L
                for ( file in existingFiles ) {
                    fileSize += file.size
                }
                statisticsListeners.add(StatisticsListener(getFilePathForAsset(parentDescription), newListener, fileSize))
                newListener.totalSizeChanged(fileSize)
            }
        }
    }

    suspend fun getOldest(areIn: AssetDescription, notIn: List<AssetDescription>): DiskAssetDescription? {
        val allFiles = getAssetDescriptionsIn(areIn)
        var oldestCandidate: DiskAssetDescription? = null
        for ( file in allFiles ) {
            var isInNotIn = file.localPath.contains("metadata.json")
            if (!isInNotIn) {
                for (notInCandidate in notIn) {
                    if (file.localPath.startsWith(notInCandidate.localPath)) {
                        isInNotIn = true
                        break
                    }
                }
            }
            if (!isInNotIn) {
                if (oldestCandidate == null || oldestCandidate.modDate > file.modDate) {
                    oldestCandidate = file
                }
            }
        }
        return oldestCandidate
    }

    private suspend fun getAssetDescriptionsIn(description: AssetDescription): Collection<DiskAssetDescription> {
        val pathStart = getFilePathForAsset(description)
        val retList = mutableListOf<DiskAssetDescription>()
        val fileList = context.filesDir.listFiles { _: File?, s: String? ->
            s?.startsWith(pathStart) ?: false
        } ?: return retList

        for (file in fileList) {
            retList.add(DiskAssetDescription(file))
        }
        return retList
    }

    private fun getFile(description: AssetDescription): File? {
        try {
            if ( description.storageLocation == StorageLocation.External && isExternalStorageReadable ) {
                val externalDirectory = context.getExternalFilesDir(null)
                if ( externalDirectory != null ) {
                    if (!externalDirectory.exists()) {
                        try {
                            externalDirectory.mkdirs()
                        } catch (e: Throwable) {
                            Log.e(null, "Could not create external directory ${externalDirectory.absolutePath}")
                        }
                    }
                    return File(externalDirectory, getFilePathForAsset(description))
                }
                return context.getFileStreamPath(getFilePathForAsset(description))
            } else {
                return context.getFileStreamPath(getFilePathForAsset(description))
            }
        } catch ( e: Throwable ) {
            return null
        }
    }

    private fun getFilePathForAsset(description: AssetDescription): String {
        if (description.hasDiskFriendlyLocalPath) {
            return description.localPath
        }
        return description.localPath.replace("/", "-").replace(":", "_")
    }

    private class StatisticsListener(val prefix: String, val listener: DiskCacheListener, var diskUsage: Long)
}