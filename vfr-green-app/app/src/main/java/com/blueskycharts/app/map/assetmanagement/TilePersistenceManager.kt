package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.map.configuration.MapConfiguration
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
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
        GlobalScope.launch {
            var iShouldRun: Boolean

            singleThreadMutex.withLock {
                iShouldRun = !running && runningVersion != requestVersion
                if (iShouldRun) {
                    running = true
                    runningVersion = requestVersion
                }
            }
            if (iShouldRun) {
                if (!statisticsRecordsLoaded) {
                    compileExistingStatistics()
                    statisticsRecordsLoaded = true
                }
                enforceAllPreferences()
            }
        }
    }

    fun stop() {
        stopRunning = true
    }

    fun getMapStatistics(groupId: Int, mapName: String, callback: (MapPersistenceStatistics) -> Unit) {
        val lookupKey = getMapKey(groupId, mapName)
        val statistics = statisticsRecords[lookupKey]
        if ( statistics == null ) {
            // Need to add the statistics object to the dictionary
            GlobalScope.launch {
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
                callback(statisticsNotNull)
            }
        } else {
            callback(statistics)
        }
    }

    private suspend fun compileExistingStatistics() {
        var numThreadsToAwait = AtomicInteger(0)

        for ( group in Inventory.instance.mapGroups ) {
            val assetProvider = TileAssetProvider.getInstance(group)
            numThreadsToAwait.incrementAndGet()
            group.getConfiguration { metadata ->
                if (metadata != null) {
                    numThreadsToAwait.incrementAndGet()
                    GlobalScope.launch {
                        for (map in metadata.mapList) {
                            val manifest = assetProvider.getManifest(group.id, map)
                            val currentMapVersionMetadata = metadata.getCurrentVersion(map)
                            val zoomMap = manifest?.versionList?.get(currentMapVersionMetadata?.version)?.zoomMap
                            var filesLoaded = 0
                            if ( zoomMap != null ) {
                                for ( zPair in zoomMap ) {
                                    val z = zPair.key
                                    for ( xPair in zPair.value ) {
                                        val x = xPair.key
                                        for ( y in xPair.value ) {
                                            filesLoaded++
                                        }
                                    }
                                }
                            }
                            numThreadsToAwait.incrementAndGet()
                            this@TilePersistenceManager.getMapStatistics(group.id, map) {
                                val maxZoom = currentMapVersionMetadata?.maxZoom
                                var totalTiles = 0
                                if (maxZoom != null) {
                                    for (zoom in maxZoom downTo 0) {
                                        totalTiles += 2.0.pow(zoom).pow(2).toInt()
                                    }
                                }
                                it.setStatistics(totalTiles, filesLoaded, 0)
                                numThreadsToAwait.decrementAndGet()
                            }
                        }
                        numThreadsToAwait.decrementAndGet()
                    }
                }
                numThreadsToAwait.decrementAndGet()
            }
        }

        // Wait for them all to complete
        while ( numThreadsToAwait.get() > 0 ) {
            yield()
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
            group.getConfiguration {
                if ( it != null ) {
                    GlobalScope.launch {
                        singleThreadMutex.withLock {
                            try {
                                if (!shouldStop) {
                                    for (name in it.mapList) {
                                        enforcePreferences(group, name, it)
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
            val manifest = tileProvider.getManifest(mapsMetaData.groupId, name)

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
                            tileProvider.retrieveTile(name, currentMapVersion, z, x, y) {
                                if ( !it.errorLoading ) {
                                    getMapStatistics(mapsMetaData.groupId, name) { it2 ->
                                        it2.downloadedFiles.increment()
                                    }
                                }
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