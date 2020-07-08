package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.assests.AssetProvider
import com.blueskycharts.app.assests.LocalAssetDescription
import com.blueskycharts.app.assests.Volatility
import com.blueskycharts.app.map.configuration.MapConfiguration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Math.pow
import kotlin.math.pow

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

        if (preferences == null || metaData == null || metaData.maxZoom == null ) {
            return
        }

        if (preferences.proactiveDownload) {
            GlobalScope.launch {
                // Create a map of all tiles for the map so that we can start to the ones that have been identified as
                // cached or copied to the cache from local sources
                val assetProvider = TileAssetProvider.getInstance("world-vfr")
                val manifest = assetProvider.getManifest("world-vfr", name) ?: return@launch

                // For each file
                for ( z in 0..metaData.maxZoom ) {
                    for ( x in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                        for ( y in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                            // For this version and descending into the past for each version
                            val listOfVersions = mapsMetaData.getPastSortedVersionList(name, includeCurrent = true)
                            /*
                            for ( version in listOfVersions ) {
                                // Check if the manifest has the needed file
                                val fileProof = manifest.versionList[version]?.xMap?.get(x)
                                if ( fileProof == null )
                                    // If it does and this isn't the current version, create an alias to the current version
                                    // Remove the file from the needed list
                                // Else
                                    // Check if the file is in the changeset for this version
                                        // If they are, download them
                            }
*/
                            // If the file was not downloaded, download it
                        }
                    }
                }
            }
        }
    }
}