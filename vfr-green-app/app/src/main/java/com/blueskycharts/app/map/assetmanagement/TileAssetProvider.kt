package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.*
import com.blueskycharts.app.map.configuration.Inventory
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
 * Manages the expiration of non persisted tiles using the manifest which should contain last access info for each tile in a heap
 */
class TileAssetProvider private constructor(private val group: Inventory.Group) {
    private val imageExtension = "jpg"
    private val manifestDescription: AssetDescription
    private var manifest: PersistentFile<Manifest>

    private val assetProvider = AssetProvider()

    init {
        val manifestLocation = "tileAssetProvider/${group.id}/manifest"
        manifestDescription = LocalAssetDescription(manifestLocation, Volatility.Indefinite)

        val asset = assetProvider.retrieveLocalAsset(manifestDescription)
        manifest = PersistentFile(if (asset.errorLoading) {
                Manifest()
            } else {
                val reader = asset.asJsonReader()
                if (reader == null) {
                    Manifest()
                } else {
                    Manifest.readFromJsonReader(reader)
                }
            }, manifestDescription)
    }

    fun retrieveTile(mapName: String, mapVersion: String, zoom: Int, x: Int, y: Int, callback: ((asset: Asset) -> Unit)) {
        // Make sure the manifest is loaded before requesting any tiles
        val manifest = this@TileAssetProvider.manifest

        // Request the tile from the base class
        val assetDescription = getTileFileDescription(mapName, mapVersion, zoom, x, y)
        val localAsset = assetProvider.retrieveLocalAsset(assetDescription)
        if ( localAsset.errorLoading ) {
            assetProvider.retrieveAsset(assetDescription) {
                GlobalScope.launch {    //ok1
                    manifest.access { manifestContents ->
                        // Make sure the file is added to the manifest
                        var mapList = manifestContents.mapGroups[group.id]
                        if (mapList == null) {
                            mapList = Manifest.MapList()
                            manifestContents.mapGroups[group.id] = mapList
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
                            xMap = mutableMapOf()
                            zoomMap[x] = xMap
                        }
                        val fileSize = xMap[y]
                        val actualBytes = it.numBytes
                        if (fileSize == null || actualBytes != fileSize) {
                            xMap[y] = actualBytes
                            return@access true
                        }
                        return@access false
                    }
                }

                // Tell the caller their file has been loaded
                callback(it)
            }
        } else {
            callback(localAsset)
        }
    }

    fun getTileFileDescription(mapName: String, mapVersion: String, zoom: Int, x: Int, y: Int): AssetDescription {
        val tileUrl = "${group.urlRoot}/${mapName}/$mapVersion/$zoom/${x}_${y}.${imageExtension}"
        return RemoteAssetDescription(URL(tileUrl), Volatility.Indefinite)
    }

    suspend fun getManifest(mapGroupId: Int, map: String): Manifest.MapList.MapVersionList? {
        // Ensure the manifest is loaded
        var manifest: PersistentFile<Manifest>? = this@TileAssetProvider.manifest
        while ( manifest == null ) {
            yield()
            manifest = this@TileAssetProvider.manifest
        }

        var retVal: Manifest.MapList.MapVersionList? = null
        manifest.access {
            retVal = it.mapGroups[mapGroupId]?.mapList?.get(map)?.copy()
            return@access false
        }
        return retVal
    }

    companion object {
        private var instances: MutableMap<Int, TileAssetProvider> = mutableMapOf()

        init {
            for ( group in Inventory.instance.mapGroups ) {
                instances[group.id] = TileAssetProvider(group)
            }
        }

        fun getInstance(group: Inventory.Group): TileAssetProvider {
            return instances[group.id]?: throw Error("Asset provider not created for group ${group.id}")
        }
    }
}