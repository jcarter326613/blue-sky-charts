package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.Asset
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.map.configuration.MapConfiguration
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.pow

/**
 * Performs background updates of the local cache by comparing the desired state to the current state
 * and issuing the necessary commands to the asset namespace to make changes
 */
class TilePersistenceManager {
    private var running = false
    private var runningVersion = 0
    private var requestVersion = 0
    private var stopRunning = false
    private var singleThreadMutex = Mutex()
    private val shouldStop: Boolean
        get() = stopRunning || runningVersion != requestVersion

    private val statisticsRecords = mutableMapOf<String, MapPersistenceStatistics>()     //Key is groupid-mapname
    private var statisticsRecordsLoaded = false
    private var statisticsRecordsMutex = Mutex()

    init {
        start()
        Preferences.instance.addListener(object: Preferences.Listener {
            override fun preferenceChanged(preferenceName: String) {
                start()
            }
        })
    }

    fun start() {
        requestVersion++
        stopRunning = false
        GlobalScope.launch {    //ok1
            var iShouldRun: Boolean = !running && runningVersion != requestVersion

            if ( iShouldRun ) {
                singleThreadMutex.withLock {
                    iShouldRun = !running && runningVersion != requestVersion
                    if (iShouldRun) {
                        running = true
                        runningVersion = requestVersion
                    }
                }
            }
            if (iShouldRun) {
                if (!statisticsRecordsLoaded) {
                    compileExistingStatistics()
                    statisticsRecordsLoaded = true
                }
                cleanOldNonPersistedTiles()
                enforceAllPreferences()
                if ( requestVersion != runningVersion && !stopRunning ) {
                    start()
                }
            }
        }
    }

    fun stop() {
        stopRunning = true
    }

    suspend fun getMapStatistics(groupId: Int, mapName: String): MapPersistenceStatistics {
        val lookupKey = getMapKey(groupId, mapName)
        val statistics = statisticsRecords[lookupKey]
        if ( statistics == null ) {
            // Need to add the statistics object to the dictionary
            val statisticsIn: MapPersistenceStatistics?
            val statisticsNotNull: MapPersistenceStatistics
            statisticsRecordsMutex.withLock {
                statisticsIn = statisticsRecords[lookupKey]
                if ( statisticsIn == null ) {
                    statisticsNotNull = MapPersistenceStatistics(groupId, mapName, 0, 0, 0)
                    statisticsRecords[lookupKey] = statisticsNotNull
                } else {
                    statisticsNotNull = statisticsIn
                }
            }
            return statisticsNotNull
        } else {
            return statistics
        }
    }

    private suspend fun compileExistingStatistics() {
        for ( group in Inventory.instance.mapGroups ) {
            val assetProvider = TileAssetProvider.getInstance(group)
            val metadata = group.getConfiguration()
            if (metadata != null) {
                for (map in metadata.mapList) {
                    val manifest = TileAssetProvider.getReadOnlyManifest(group.id, map)
                    val currentMapVersionMetadata = metadata.getCurrentVersion(map)
                    val zoomMap = manifest?.versionList?.get(currentMapVersionMetadata?.version)?.zoomMap
                    var filesLoaded = 0
                    var fileSize = 0
                    if ( zoomMap != null ) {
                        for ( zPair in zoomMap ) {
                            for ( xPair in zPair.value ) {
                                for ( yPair in xPair.value ) {
                                    filesLoaded++
                                    fileSize += yPair.value
                                }
                            }
                        }
                    }
                    val mapStatistics = this@TilePersistenceManager.getMapStatistics(group.id, map)
                    val maxZoom = currentMapVersionMetadata?.maxZoom
                    var totalTiles = 0
                    if (maxZoom != null) {
                        for (zoom in maxZoom downTo 0) {
                            totalTiles += 2.0.pow(zoom).pow(2).toInt()
                        }
                    }
                    mapStatistics.setStatistics(totalTiles, filesLoaded, fileSize)
                }
            }
        }
    }

    private suspend fun cleanOldNonPersistedTiles() {
        // Figure out how much space we are taking up from un persisted maps
        var usedBytes: Long = 0
        for (group in Inventory.instance.mapGroups) {
            val groupTileProvider = TileAssetProvider.getInstance(group)
            val configuration = group.getConfiguration() ?: continue
            for (mapName in configuration.mapList) {
                val proactiveDownload = Preferences.instance.getBooleanValue(
                    Preferences.propertyTemplateMapProactiveDownload(group.id, mapName),
                    Preferences.defaultValueMapProactiveDownload
                )
                if (!proactiveDownload) {
                    // This is a non persistent map, so count the size of the files in the manifest
                    val manifest = TileAssetProvider.getReadOnlyManifest(group.id, mapName) ?: continue
                    for (version in manifest.versionList) {
                        for (zoomMap in version.value.zoomMap) {
                            for (xMap in zoomMap.value) {
                                for (yMap in xMap.value) {
                                    val tileDescription = groupTileProvider.getTileFileDescription(mapName, version.key, zoomMap.key, xMap.key, yMap.key)
                                    if (!DiskCacheFactory.instance.isAlias(tileDescription)) {
                                        usedBytes += yMap.value
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Start deleting files until we are down to our un-persisted cache limit
        val unPersistedMaxSpaceBytes = Preferences.instance.getIntValue(
            Preferences.propertyNameMaxUnPersistedTileDiskSpace, Preferences.defaultValueMaxUnPersistedTileDiskSpace
        )
        while (unPersistedMaxSpaceBytes < usedBytes) {
            val oldestFile = TileAssetProvider.popOldestUnPersistedManifestFile() ?: break
            val oldestFileGroup = Inventory.instance.findGroupById(oldestFile.groupId) ?: continue
            val tileProvider = TileAssetProvider.getInstance(oldestFileGroup)

            val properManifest = TileAssetProvider.getReadOnlyManifest(oldestFile.groupId, oldestFile.mapName) ?: continue
            var firstLoop = true
            for ( version in properManifest.versionList.keys ) {
                val oldestFileDescriptor = tileProvider.getTileFileDescription(
                    oldestFile.mapName,
                    version,
                    oldestFile.z,
                    oldestFile.x,
                    oldestFile.y
                )
                if (firstLoop) {
                    usedBytes -= DiskCacheFactory.instance.getFileSize(oldestFileDescriptor)
                    firstLoop = false
                }
                DiskCacheFactory.instance.deleteAsset(oldestFileDescriptor)
            }
        }
    }

    private suspend fun enforceAllPreferences() {
        if (shouldStop) {
            singleThreadMutex.withLock {
                running = false
            }
            return
        }
        val threadCount = Inventory.instance.mapGroups.size
        var completedThreads = 0
        for ( group in Inventory.instance.mapGroups ) {
            val configuration = group.getConfiguration()
            if ( configuration != null ) {
                singleThreadMutex.withLock {
                    try {
                        if (!shouldStop) {
                            for (name in configuration.mapList) {
                                enforcePreferences(group, name, configuration)
                            }
                        }
                    } finally {
                        completedThreads++
                        if (completedThreads == threadCount) {
                            running = false
                        }
                    }
                }
            }
        }
    }

    private fun getMapKey(groupId: Int, mapName: String) = "$groupId-$mapName"

    private suspend fun enforcePreferences(group: Inventory.Group, name: String, mapsMetaData: MapConfiguration) {
        val metaData = mapsMetaData.getCurrentVersion(name)
        if (metaData?.maxZoom == null || shouldStop) {
            return
        }

        if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(mapsMetaData.groupId, name), Preferences.defaultValueMapProactiveDownload)) {
            val currentMapVersion = metaData.version ?: return

            // Create a map of all tiles for the map so that we can start to the ones that have been identified as
            // cached or copied to the cache from local sources
            val tileProvider = TileAssetProvider.getInstance(group)
            val manifest = TileAssetProvider.getReadOnlyManifest(mapsMetaData.groupId, name)

            // For each file
            for ( z in 0..metaData.maxZoom ) {
                for ( x in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                    for ( y in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                        if ( shouldStop ) {
                            return
                        }

                        // Check if we already have the file
                        val targetFileDescription = tileProvider.getTileFileDescription(name, currentMapVersion, z, x, y)
                        val hasTile = manifest?.versionList?.get(currentMapVersion)?.zoomMap?.get(z)?.get(x)?.contains(y) ?: false
                        if ( hasTile ) {
                            continue
                        }

                        // Descending into the past for each version
                        var fileDownloadedOrAliased = false
                        val listOfVersions = mapsMetaData.getPastSortedVersionList(name)
                        for ( version in listOfVersions ) {
                            if ( shouldStop ) {
                                return
                            }
                            // Check if the manifest has the needed file
                            val fileProof = manifest?.versionList?.get(version)?.zoomMap?.get(z)?.get(x)?.contains(y)
                            if ( fileProof != null && fileProof ) {
                                // If it does and this isn't the current version, create an alias to the current version
                                val existingFileDescription = tileProvider.getTileFileDescription(name, version, z, x, y)
                                DiskCacheFactory.instance.createAlias(existingFileDescription, targetFileDescription)
                                fileDownloadedOrAliased = true
                                break
                            } else {
                                // Check if the file is in the changeset for this version
                                val changeSet = mapsMetaData.getVersion(name, version)
                                val fileInChangeSetEvidence = changeSet?.changeSet?.tiles?.get(z)?.get(x)?.get(y)
                                if ( fileInChangeSetEvidence != null ) {
                                    // If they are, download them
                                    tileProvider.retrieveTile(name, currentMapVersion, z, x, y) {}
                                    fileDownloadedOrAliased = true
                                    break
                                }
                            }
                        }

                        // If the file was not downloaded, download it
                        if ( shouldStop ) {
                            return
                        }
                        if ( !fileDownloadedOrAliased ) {
                            var asset: Asset? = null
                            tileProvider.retrieveTile(name, currentMapVersion, z, x, y) {
                                asset = it
                            }
                            while (asset == null) {
                                yield()
                            }
                            val assetStatic = asset
                            if ( assetStatic != null && !assetStatic.errorLoading ) {
                                val mapStatistics = getMapStatistics(mapsMetaData.groupId, name)
                                mapStatistics.downloadedFiles.increment()
                                mapStatistics.downloadedSizeBytes.add(assetStatic.numBytes)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val instance = TilePersistenceManager()
    }
}