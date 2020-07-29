package com.blueskycharts.app.map.assetmanagement

import android.net.ConnectivityManager
import android.util.Log
import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.Asset
import com.blueskycharts.app.assests.AssetDescription
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.assests.DiskCacheListener
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.map.configuration.MapConfiguration
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.concurrent.timer
import kotlin.math.pow

/**
 * Performs background updates of the local cache by comparing the desired state to the current state
 * and issuing the necessary commands to the asset namespace to make changes
 */
@Suppress("DEPRECATION")
class TilePersistenceManager(private val connectivityManager: ConnectivityManager) {
    private var running = false
    private var runningVersion = 0
    private var requestVersion = 0
    private var firstRun = true
    private var stopRunning = false
    private var singleThreadMutex = Mutex()
    private val shouldStop: Boolean
        get() = stopRunning || runningVersion != requestVersion

    private val statisticsRecords = mutableMapOf<String, MapPersistenceStatistics>()     //Key is groupid-mapname
    private var statisticsRecordsMutex = Mutex()

    private var manifestUpdateStartTimerWaiting = AtomicBoolean(false)

    private val onWifi: Boolean
        get() {
            connectivityManager.networkPreference = ConnectivityManager.TYPE_WIFI
            val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            return (networkInfo?.isConnected ?: false) && !connectivityManager.isActiveNetworkMetered
        }

    init {
        start()
        Preferences.instance.addListener(object: Preferences.Listener {
            override fun preferenceChanged(preferenceName: String) {
                if (preferenceName.startsWith(Preferences.propertyTemplatePrefixProactiveDownload)) {
                    start()
                }
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
                if (firstRun) {
                    addManifestChangeListeners()
                    firstRun = false
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

    private suspend fun addManifestChangeListeners() {
        for (group in Inventory.instance.mapGroups) {
            val config = group.getConfiguration() ?: continue
            for (map in config.mapList) {
                val mapStatistics = getMapStatistics(group.id, map)
                mapStatistics.addListener(object: MapPersistenceStatistics.Listener {
                    override fun statisticsUpdated(downloadedSizeBytes: Long) {
                        //Set a future time to restart so we're not constantly starting and restarting the persistence process
                        if (!this@TilePersistenceManager.manifestUpdateStartTimerWaiting.getAndSet(true)) {
                            GlobalScope.launch {
                                delay(10000L /*10 seconds*/)
                                if (this@TilePersistenceManager.manifestUpdateStartTimerWaiting.getAndSet(false)) {
                                    this@TilePersistenceManager.start()
                                }
                            }
                        }
                    }
                })
            }
        }
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
                    statisticsNotNull = MapPersistenceStatistics(groupId, mapName, 0)
                    var statisticsSet = false

                    val assetProvider = TileAssetProvider.getInstance(groupId)
                    DiskCacheFactory.instance.addListener(assetProvider.getMapAssetDescriptionContainer(mapName), object:DiskCacheListener {
                        override suspend fun totalSizeChanged(totalSize: Long) {
                            statisticsNotNull.setStatistics(totalSize)
                            statisticsSet = true
                        }
                    })

                    // Wait for the statistics to get set since this is a suspend func that expects the value to be set on return
                    while(!statisticsSet) {
                        yield()
                    }

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

    private suspend fun cleanOldNonPersistedTiles() {
        // Figure out how much space we are taking up from un persisted maps
        var usedBytes: Long = 0
        val unPersistedFileRootList = mutableListOf<AssetDescription>()
        for (group in Inventory.instance.mapGroups) {
            val assetProvider = TileAssetProvider.getInstance(group.id)
            val configuration = group.getConfiguration() ?: continue
            for (mapName in configuration.mapList) {
                val proactiveDownload = Preferences.instance.getBooleanValue(
                    Preferences.propertyTemplateMapProactiveDownload(group.id, mapName),
                    Preferences.defaultValueMapProactiveDownload
                )
                if (!proactiveDownload) {
                    val mapStatistics = getMapStatistics(group.id, mapName)
                    usedBytes += mapStatistics.downloadedSizeBytes
                } else {
                    unPersistedFileRootList.add(assetProvider.getMapAssetDescriptionContainer(mapName))
                }
            }
        }

        // Start deleting files until we are down to our un-persisted cache limit
        val unPersistedMaxSpaceBytes = Preferences.instance.getIntValue(
            Preferences.propertyNameMaxUnPersistedTileDiskSpace, Preferences.defaultValueMaxUnPersistedTileDiskSpace
        )

        val topLevelContainer = TileAssetProvider.getMapAssetDescriptionContainer()
        while (unPersistedMaxSpaceBytes < usedBytes) {
            val oldestFile = DiskCacheFactory.instance.getOldest(topLevelContainer, unPersistedFileRootList) ?: break
            val size = oldestFile.size
            DiskCacheFactory.instance.deleteAsset(oldestFile)       //TODO: revisit this when we get aliases working.  If we delete an alias or rename a file to another alias,
                                                                    // we'll be either not reporting the right size change or updating the mod date on the rename
            usedBytes -= size
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

            // For each file
            for ( z in 0..metaData.maxZoom ) {
                for ( x in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                    for ( y in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                        if ( shouldStop ) {
                            return
                        }

                        // Check if we already have the file
                        val targetFileDescription = tileProvider.getTileFileDescription(name, currentMapVersion, z, x, y)
                        val hasTile = DiskCacheFactory.instance.exists(targetFileDescription)
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
                            val existingFileDescription = tileProvider.getTileFileDescription(name, version, z, x, y)
                            val hasAliasTile = DiskCacheFactory.instance.exists(existingFileDescription)
                            if (hasAliasTile) {
                                // If it does and this isn't the current version, create an alias to the current version
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
                        if ( !fileDownloadedOrAliased && onWifi ) {
                            var throttleBoolean = false
                            tileProvider.retrieveTile(name, currentMapVersion, z, x, y) {
                                throttleBoolean = true
                            }
                            while (!throttleBoolean) {
                                yield()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private var instance: TilePersistenceManager? = null

        fun getInstance(cm: ConnectivityManager): TilePersistenceManager {
            var i = instance
            if (i == null) {
                i = TilePersistenceManager(cm)
                instance = i
            }
            return i
        }
    }
}