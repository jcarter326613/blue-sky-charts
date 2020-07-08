package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.assests.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.net.URL

/**
 * Provides access to map tile assets whether on disk or remotely stored.
 * Provides record keeping of what files have been stored locally and what map/version they belong to
 * TODO: Choose one of:
 *  Listens for deletion of files which are in the manifest and removes them
 *  Provides a method for deletion of files which are in the manifest and removes them
 */
class TileAssetProvider private constructor(private val mapRoot: String) {
    private val imageExtension = "jpg"
    private val manifestLocation: String
    private val manifestDescription: AssetDescription
    private var manifest: Manifest? = null
    private var manifestVersion = 0
    private var manifestWrittenVersion = 0
    private val manifestMutex: Mutex = Mutex()
    private val manifestSerializationMutex: Mutex = Mutex()

    private val assetProvider = AssetProvider()

    init {
        manifestLocation = "tileAssetProvider/$mapRoot/manifest" //TODO: Make this change based on map root so we can have seperate manifests per tile provider
        manifestDescription = LocalAssetDescription(manifestLocation, Volatility.Indefinite)

        assetProvider.retrieveAsset(manifestDescription) {
            manifest = if ( it.errorLoading ) {
                Manifest()
            } else {
                val reader = it.asJsonReader()
                if (reader == null) {
                    Manifest()
                } else {
                    Manifest.readFromJsonReader(reader)
                }
            }
        }
    }

    fun retrieveTile(mapName: String, mapVersion: String, zoom: Int, x: Int, y: Int, callback: ((asset: Asset) -> Unit)) {
        GlobalScope.launch {
            // Make sure the manifest is loaded before requesting any tiles
            var manifest: Manifest? = this@TileAssetProvider.manifest
            while ( manifest == null ) {
                yield()
                manifest = this@TileAssetProvider.manifest
            }

            // Request the tile from the base class
            val tileUrl = "${mapRoot}/${mapName}/$mapVersion/$zoom/${x}_${y}.${imageExtension}"
            val assetDescription = RemoteAssetDescription(URL(tileUrl), Volatility.Indefinite)
            assetProvider.retrieveAsset(assetDescription) {
                GlobalScope.launch {
                    manifestMutex.withLock {
                        // Make sure the file is added to the manifest
                        val defaultMapGroup = "world-vfr"
                        var mapList = manifest.mapGroups[defaultMapGroup]
                        if (mapList == null) {
                            mapList = Manifest.MapList()
                            manifest.mapGroups[defaultMapGroup] = mapList
                        }
                        var map = mapList.mapList[mapName]
                        if (map == null) {
                            map = Manifest.MapList.MapVersionList()
                            mapList.mapList[mapName] = map
                        }
                        var version = map.versionList[mapVersion]
                        if (version == null) {
                            version = Manifest.MapList.MapVersionList.MapVersion()
                            map.versionList[mapVersion] = version
                        }
                        var xMap = version.xMap[x]
                        if (xMap == null) {
                            xMap = mutableSetOf()
                            version.xMap[x] = xMap
                        }
                        val changeMade = xMap.add(y)

                        // Write the new manifest out if there were changes made
                        if (changeMade) {
                            manifestVersion++
                            serializeManifest()
                        }
                    }
                }

                // Tell the caller their file has been loaded
                callback(it)
            }
        }
    }

    suspend fun getManifest(mapGroup: String, map: String): Manifest.MapList.MapVersionList? {
        // Ensure the manifest is loaded
        var manifest: Manifest? = this@TileAssetProvider.manifest
        while ( manifest == null ) {
            yield()
            manifest = this@TileAssetProvider.manifest
        }

        manifestMutex.withLock {
            return manifest.mapGroups[mapGroup]?.mapList?.get(map)?.copy()
        }
    }

    /**
     * Writes out the manifest in a seperate thread.  This can be called multiple times from many threads and will only
     * run once if those requests pile up faster than than the file can be written to disk
     */
    private fun serializeManifest() {
        GlobalScope.launch {
            manifestSerializationMutex.withLock {
                if (manifestVersion > manifestWrittenVersion) {
                    var versionToWrite: Int
                    val manifestAsset = Asset(manifestDescription)
                    manifestMutex.withLock {
                        versionToWrite = manifestVersion
                        manifestAsset.bytes = manifest?.jsonString?.toByteArray()
                    }
                    DiskCacheFactory.instance.writeAsset(manifestAsset)
                    manifestWrittenVersion = versionToWrite
                }
            }
        }
    }

    companion object {
        private var instances: MutableMap<String, TileAssetProvider> = mutableMapOf()

        fun getInstance(mapRoot: String): TileAssetProvider {
            var instance = instances[mapRoot]
            if ( instance == null ) {
                instance = TileAssetProvider(mapRoot)
                instances[mapRoot] = instance
            }
            return instance
        }
    }
}