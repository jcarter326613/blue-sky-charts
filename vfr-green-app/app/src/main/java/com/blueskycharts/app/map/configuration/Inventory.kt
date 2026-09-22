package com.blueskycharts.app.map.configuration

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.*
import com.blueskycharts.app.map.models.MapMetaDataModelCollection
import kotlinx.coroutines.yield
import java.net.URL

class Inventory(val mapGroups: List<Group>) {
    fun findGroupById(id: Int): Group? {
        return mapGroups.find {
            it.id == id
        }
    }

    class Group(val id: Int, val humanName: String, val urlRoot: String, val displayAll: Boolean) {
        var config: MapConfiguration? = null
        var configRead = false

        init {
            val mapConfigurationFile = URL("${urlRoot}/metadata.json")
            val assetProvider = AssetProvider()
            val assetDescription = RemoteAssetDescription(mapConfigurationFile, Volatility.DayCache, StorageLocation.Internal)
            assetDescription.allowExpired = true
            assetProvider.retrieveAsset(assetDescription) {
                config = if (!it.errorLoading) {
                    val reader = it.asJsonReader()
                    if (reader != null) {
                        val mapPositions = MapConfiguration(
                            MapMetaDataModelCollection.readFromJsonReader(reader),
                            urlRoot,
                            displayAll,
                            id
                        )
                        mapPositions
                    } else {
                        null
                    }
                } else {
                    null
                }
                configRead = true
            }
        }

        suspend fun getConfiguration(): MapConfiguration? {
            while ( !configRead ) {
                yield()
            }
            return config
        }
    }

    companion object {
        val topLevelMapUrl = "https://blueskycharts.com/maps"

        private var _instance: Inventory? = null
        val instance: Inventory
            get() {
                if ( _instance == null ) {
                    val groups = mutableListOf<Group>()
                    val worldVfrInventoryGroup = Group(1, "World VFR", "$topLevelMapUrl/world-vfr-mosaic", true)
                    val usSectionalVfrGroup = Group(2, "VFR Sectional", "$topLevelMapUrl/vfr-sectional", false)
                    val usTerminalVfrGroup = Group(3, "VFR Terminal", "$topLevelMapUrl/vfr-terminal", false)
                    groups.add(worldVfrInventoryGroup)
                    groups.add(usSectionalVfrGroup)
                    groups.add(usTerminalVfrGroup)
                    _instance = Inventory(groups)
                }
                return _instance!!
            }
    }
}