package com.blueskycharts.app.map.assetmanagement

import android.util.JsonReader
import com.blueskycharts.app.assests.PreferredStorage

data class TilePersistencePreferences(var storageType: PreferredStorage = PreferredStorage.InternalOnly, var proactiveDownload: Boolean = false) {
    companion object {
        fun fromJsonReader(jsonReader: JsonReader): TilePersistencePreferences {
            val retVal = TilePersistencePreferences()
            jsonReader.beginObject()
            while ( jsonReader.hasNext() ) {
                when ( jsonReader.nextName() ) {
                    "storageType" -> {
                        retVal.storageType = PreferredStorage.valueOf(jsonReader.nextString())
                    }
                    else -> {
                        jsonReader.skipValue()
                    }
                }
            }
            jsonReader.endObject()
            return retVal
        }
    }
}