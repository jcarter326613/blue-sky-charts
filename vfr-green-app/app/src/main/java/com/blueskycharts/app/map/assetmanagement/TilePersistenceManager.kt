package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.assests.AssetProvider
import com.blueskycharts.app.assests.LocalAssetDescription
import com.blueskycharts.app.assests.Volatility
import com.blueskycharts.app.map.configuration.MapConfiguration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Performs background updates of the local cache by comparing the desired state to the current state
 * and issuing the necessary commands to the asset namespace to make changes
 */
class TilePersistenceManager( private val mapsMetaData: MapConfiguration ) {
    val mapNames: List<String>
        get() = mapsMetaData.mapList
    var preferences: MutableMap<String, TilePersistencePreferences> = mutableMapOf()

    init {
        for ( name in mapNames ) {
            loadPreferences(name) {
                preferences[name] = it
                enforcePreferences(name)
            }
        }
    }

    private fun loadPreferences(name: String, callback: ((preferences: TilePersistencePreferences) -> Unit)) {
        AssetProvider().retrieveAsset(LocalAssetDescription("tile_preferences/$name", Volatility.Indefinite)) {
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

    private fun enforcePreferences(name: String) {
        val metaData = mapsMetaData.getCurrentVersion(name)
        val preferences = preferences[name]

        preferences?.let { metaData?.let {
            if (preferences.proactiveDownload) {
                GlobalScope.launch {
                    // Create a map of all tiles for the map so that we can start to the ones that have been identified as
                    // cached or copied to the cache from local sources
                    val assetProvider = TileAssetProvider.getInstance("world-vfr")
                    val manifest = assetProvider.getManifest("world-vfr", name)


                    // For each file
                        // For this version and descending into the past for each version
                            // Check if the manifest has the needed file
                                // If it does and this isn't the current version, create an alias to the current version
                                // Remove the file from the needed list
                            // Else
                                // Check if the file is in the changeset for this version
                                    // If they are, download them

                    // For each remaining file, download them
                }
            }
        }}
    }
}