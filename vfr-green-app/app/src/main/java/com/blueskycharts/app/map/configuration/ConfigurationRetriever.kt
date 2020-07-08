package com.blueskycharts.app.map.configuration

import com.blueskycharts.app.Constants
import com.blueskycharts.app.assests.AssetProvider
import com.blueskycharts.app.assests.RemoteAssetDescription
import com.blueskycharts.app.assests.Volatility
import com.blueskycharts.app.map.models.MapMetaDataModelCollection
import java.net.URL

class ConfigurationRetriever {
    fun retrieveConfiguration(callback: (it: MapConfiguration?) -> Unit) {
        var mapConfigurationFile = URL("${Constants.serverRoot}${Constants.serverWorldVfrMosaicSubDirectory}/metadata.json")
        val assetProvider = AssetProvider()
        assetProvider.retrieveAsset(RemoteAssetDescription(mapConfigurationFile, Volatility.DayCache)) {
            val reader = it.asJsonReader()
            if ( reader != null ) {
                val mapPositions = MapConfiguration(MapMetaDataModelCollection.readFromJsonReader(reader))
                callback(mapPositions)
            } else {
                callback(null)
            }
        }
    }

}