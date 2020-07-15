package com.blueskycharts.app.map.configuration

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.AssetProvider
import com.blueskycharts.app.assests.RemoteAssetDescription
import com.blueskycharts.app.assests.Volatility
import com.blueskycharts.app.map.models.MapMetaDataModelCollection
import java.net.URL

class Inventory(val mapGroups: List<Group>) {
    fun findGroupById(id: Int): Group? {
        return mapGroups.find {
            it.id == id
        }
    }

    class Group(val id: Int, val humanName: String, private val urlRoot: String, private val displayAll: Boolean) {
        fun getConfiguration(callback: (it: MapConfiguration?) -> Unit) {
            var mapConfigurationFile = URL("${urlRoot}/metadata.json")
            val assetProvider = AssetProvider()
            assetProvider.retrieveAsset(RemoteAssetDescription(mapConfigurationFile, Volatility.DayCache)) {
                val reader = it.asJsonReader()
                if ( reader != null ) {
                    val mapPositions = MapConfiguration(MapMetaDataModelCollection.readFromJsonReader(reader), urlRoot, displayAll)
                    callback(mapPositions)
                } else {
                    callback(null)
                }
            }
        }
    }

    companion object {
        private var _instance: Inventory? = null
        val instance: Inventory
            get() {
                if ( _instance == null ) {
                    val groups = mutableListOf<Group>()
                    val worldVfrInventoryGroup = Group(1, "World VFR", "https://blueskycharts.com/maps/world-vfr-mosaic", true)
                    val usSectionalVfrGroup = Group(2, "VFR Sectional", "https://blueskycharts.com/maps/vfr-sectional", false)
                    groups.add(worldVfrInventoryGroup)
                    groups.add(usSectionalVfrGroup)
                    _instance = Inventory(groups)
                }
                return _instance!!
            }
    }
}