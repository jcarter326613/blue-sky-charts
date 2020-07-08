package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.map.models.MapMetaDataModelCollection
import com.blueskycharts.app.assests.AssetProviderFactory
import com.blueskycharts.app.assests.LocalAssetDescription
import com.blueskycharts.app.assests.Volatility
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class TilePersistenceManager( private val mapsMetaData: MapMetaDataModelCollection ) {
    private var _mapNames: Collection<String>? = null
    val mapNames: Collection<String>
        get() {
            var retVal = _mapNames
            if ( retVal == null ) {
                retVal = LinkedList()
                for (key in mapsMetaData.maps.keys) {
                    if (key.isNotEmpty() && key[0].isLowerCase()) {
                        retVal.add(key[0].toUpperCase() + key.substring(1))
                    } else {
                        retVal.add(key)
                    }
                }
                _mapNames = retVal
            }
            return retVal
        }
    var preferences: HashMap<String, TilePersistencePreferences> = HashMap()

    init {
        for ( name in mapNames ) {
            loadPreferences(name) {
                preferences[name] = it
                enforcePreferences(name)
            }
        }
    }

    private fun loadPreferences(name: String, callback: ((preferences: TilePersistencePreferences) -> Unit)): Unit {
        AssetProviderFactory.instance.retrieveAsset(LocalAssetDescription("tile_preferences/$name", Volatility.Indefinite)) {
            if ( it.bytes == null ) {
                callback(TilePersistencePreferences())
            } else {
                val jsonReader = it.asJsonReader()
                if ( jsonReader == null ) {
                    callback(TilePersistencePreferences())
                } else {
                    val preferences = TilePersistencePreferences.fromJsonReader(jsonReader)
                    callback(preferences)
                }
            }
        }
    }

    private fun enforcePreferences(name: String): Unit {
        val metaData = mapsMetaData.maps[name]
        val preferences = preferences[name]

        preferences?.let { metaData?.let {
            if (preferences.proactiveDownload) {
                GlobalScope.launch {
                    val mapDescription = getLatestVersion(metaData)
                }
            }
        }}
    }

    private fun getLatestVersion(metaData: MapMetaDataModel): SubMapModel {

    }
}