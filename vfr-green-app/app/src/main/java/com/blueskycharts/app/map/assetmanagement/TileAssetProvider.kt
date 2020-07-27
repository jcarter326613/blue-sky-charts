package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.*
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.Dispatchers
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
    fun retrieveTile(mapName: String, mapVersion: String, zoom: Int, x: Int, y: Int, callback: ((asset: Asset) -> Unit)) {
        // Request the tile from the base class
        val assetDescription = getTileFileDescription(mapName, mapVersion, zoom, x, y)
        val localAsset = assetProvider.retrieveLocalAsset(assetDescription)
        if ( localAsset.errorLoading ) {
            assetProvider.retrieveAsset(assetDescription, callback)
        } else {
            try {
                callback(localAsset)
            } catch (e: Throwable) {
                assetProvider.retrieveAsset(assetDescription, callback)
            }
        }
    }

    fun getTileFileDescription(mapName: String, mapVersion: String, zoom: Int, x: Int, y: Int): RemoteAssetDescription {
        val tileUrl = "${group.urlRoot}/${mapName}/$mapVersion/$zoom/${x}_${y}.${imageExtension}"
        val storage = if (Preferences.instance.getBooleanValue(Preferences.propertyNameStoreMapsExternally, Preferences.defaultValueStoreMapsExternally)) {
            StorageLocation.External
        } else {
            StorageLocation.Internal
        }
        val description = RemoteAssetDescription(URL(tileUrl), Volatility.Indefinite, storage)
        description.readActsAsModification = true
        description.allowExpired = true
        return description
    }

    fun getMapAssetDescriptionContainer(mapName: String): AssetDescription {
        val tileUrl = "${group.urlRoot}/${mapName}"
        val storage = if (Preferences.instance.getBooleanValue(Preferences.propertyNameStoreMapsExternally, Preferences.defaultValueStoreMapsExternally)) {
            StorageLocation.External
        } else {
            StorageLocation.Internal
        }
        return RemoteAssetDescription(URL(tileUrl), Volatility.Indefinite, storage, requiresCors = false, isFolder = true)
    }

    companion object {
        private var instances: MutableMap<Int, TileAssetProvider> = mutableMapOf()
        private const val imageExtension = "jpg"
        private val assetProvider = AssetProvider()

        init {
            for ( group in Inventory.instance.mapGroups ) {
                instances[group.id] = TileAssetProvider(group)
            }
        }

        fun getInstance(group: Inventory.Group): TileAssetProvider {
            return getInstance(group.id)
        }

        fun getInstance(groupId: Int): TileAssetProvider {
            return instances[groupId]?: throw Error("Asset provider not created for group ${groupId}")
        }

        fun getMapAssetDescriptionContainer(): AssetDescription {
            val tileUrl = Inventory.topLevelMapUrl
            val storage = if (Preferences.instance.getBooleanValue(Preferences.propertyNameStoreMapsExternally, Preferences.defaultValueStoreMapsExternally)) {
                StorageLocation.External
            } else {
                StorageLocation.Internal
            }
            return RemoteAssetDescription(URL(tileUrl), Volatility.Indefinite, storage, requiresCors = false, isFolder = true)
        }
    }
}