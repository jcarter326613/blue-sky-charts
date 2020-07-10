package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.Constants
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
    private val manifestDescription: AssetDescription
    private var manifest: PersistentFile<Manifest>? = null

    private val assetProvider = AssetProvider()

    init {
        val manifestLocation = "tileAssetProvider/$mapRoot/manifest"
        manifestDescription = LocalAssetDescription(manifestLocation, Volatility.Indefinite)

        assetProvider.retrieveAsset(manifestDescription) {
            manifest = PersistentFile(if ( it.errorLoading ) {
                    Manifest()
                } else {
                    val reader = it.asJsonReader()
                    if (reader == null) {
                        Manifest()
                    } else {
                        Manifest.readFromJsonReader(reader)
                    }
                }, manifestDescription)
        }
    }

    fun retrieveTile(mapName: String, mapVersion: String, zoom: Int, x: Int, y: Int, callback: ((asset: Asset) -> Unit)) {
        GlobalScope.launch {
            // Make sure the manifest is loaded before requesting any tiles
            var manifest: PersistentFile<Manifest>? = this@TileAssetProvider.manifest
            while ( manifest == null ) {
                yield()
                manifest = this@TileAssetProvider.manifest
            }

            // Request the tile from the base class
            val assetDescription = getTileFileDescription(mapName, mapVersion, zoom, x, y)
            assetProvider.retrieveAsset(assetDescription) {
                GlobalScope.launch {
                    manifest.access { manifestContents ->
                        // Make sure the file is added to the manifest
                        val defaultMapGroup = Constants.worldVfrMosaicMapName
                        var mapList = manifestContents.mapGroups[defaultMapGroup]
                        if (mapList == null) {
                            mapList = Manifest.MapList()
                            manifestContents.mapGroups[defaultMapGroup] = mapList
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
                        var zoomMap = version.zoomMap[zoom]
                        if (zoomMap == null) {
                            zoomMap = mutableMapOf()
                            version.zoomMap[zoom] = zoomMap
                        }
                        var xMap = zoomMap[x]
                        if (xMap == null) {
                            xMap = mutableSetOf()
                            zoomMap[x] = xMap
                        }
                        return@access xMap.add(y)
                    }
                }

                // Tell the caller their file has been loaded
                callback(it)
            }
        }
    }

    fun getTileFileDescription(mapName: String, mapVersion: String, zoom: Int, x: Int, y: Int): AssetDescription {
        val tileUrl = "${mapRoot}/${mapName}/$mapVersion/$zoom/${x}_${y}.${imageExtension}"
        return RemoteAssetDescription(URL(tileUrl), Volatility.Indefinite)
    }

    suspend fun getManifest(mapGroup: String, map: String): Manifest.MapList.MapVersionList? {
        // Ensure the manifest is loaded
        var manifest: PersistentFile<Manifest>? = this@TileAssetProvider.manifest
        while ( manifest == null ) {
            yield()
            manifest = this@TileAssetProvider.manifest
        }

        var retVal: Manifest.MapList.MapVersionList? = null
        manifest.access {
            retVal = it.mapGroups[mapGroup]?.mapList?.get(map)?.copy()
            return@access false
        }
        return retVal
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