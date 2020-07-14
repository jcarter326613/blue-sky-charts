package com.blueskycharts.app.map.assetmanagement

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.DiskCacheFactory
import com.blueskycharts.app.map.configuration.MapConfiguration
import com.blueskycharts.app.preferences.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * Performs background updates of the local cache by comparing the desired state to the current state
 * and issuing the necessary commands to the asset namespace to make changes
 */
class TilePersistenceManager( private val mapsMetaData: MapConfiguration ) {
    private val mapNames: List<String>
        get() = mapsMetaData.mapList
    private var destroyed = false

    init {
        for ( name in mapNames ) {
            enforcePreferences(name)
        }
    }

    private fun destroy() {
        this.destroyed = true
    }

    private fun enforcePreferences(name: String) {
        val metaData = mapsMetaData.getCurrentVersion(name)
        if (metaData?.maxZoom == null) {
            return
        }

        if (Preferences.instance.getBooleanValue(Preferences.propertyTemplateMapProactiveDownload(name), Preferences.defaultTemplateMapProactiveDownload)) {
            GlobalScope.launch {
                val currentMapVersion = metaData.version ?: return@launch

                // Create a map of all tiles for the map so that we can start to the ones that have been identified as
                // cached or copied to the cache from local sources
                val assetProvider = TileAssetProvider.getInstance(Constants.worldVfrMosaicMapName)      //TODO: Do something about this confusingness of the multiple providers with different constructor strings
                val manifest = assetProvider.getManifest(Constants.worldVfrMosaicMapName, name)
                val tileProvider = TileAssetProvider.getInstance(Constants.serverRoot + Constants.serverWorldVfrMosaicSubDirectory)

                // For each file
                for ( z in 0..metaData.maxZoom ) {
                    for ( x in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                        for ( y in 0 until 2.0.pow(z.toDouble()).toInt() ) {
                            if ( destroyed ) {
                                return@launch
                            }

                            // Check if we already have the file
                            val targetFileDescription = tileProvider.getTileFileDescription(name, currentMapVersion, z, x, y)
                            val hasTile = manifest?.versionList?.get(currentMapVersion)?.zoomMap?.get(z)?.get(x)?.contains(y) ?: false
                            if ( hasTile ) {
                                continue
                            }

                            // Descending into the past for each version
                            var fileDownloadedOrAliased = false
                            val listOfVersions = mapsMetaData.getPastSortedVersionList(name)
                            for ( version in listOfVersions ) {
                                if ( destroyed ) {
                                    return@launch
                                }
                                // Check if the manifest has the needed file
                                val fileProof = manifest?.versionList?.get(version)?.zoomMap?.get(z)?.get(x)?.contains(y)
                                if ( fileProof != null && fileProof ) {
                                    // If it does and this isn't the current version, create an alias to the current version
                                    val existingFileDescription = tileProvider.getTileFileDescription(name, version, z, x, y)
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
                            if ( destroyed ) {
                                return@launch
                            }
                            if ( !fileDownloadedOrAliased ) {
                                tileProvider.retrieveTile(name, currentMapVersion, z, x, y) {}
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        var instance: TilePersistenceManager? = null

        fun initializeOrRefresh() {
            val configurationRetriever = ConfigurationRetriever()
            configurationRetriever.retrieveConfiguration {
                if ( it != null ) {
                    instance?.destroy()
                    instance = TilePersistenceManager(it)
                }
            }
        }
    }
}