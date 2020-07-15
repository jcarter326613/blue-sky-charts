package com.blueskycharts.app.preferences

import android.util.JsonReader
import com.blueskycharts.app.assests.PreferredStorage

data class SubMapPersistencePreferences(var storageType: PreferredStorage = PreferredStorage.InternalOnly, var proactiveDownload: Boolean = false) {
    companion object {
        fun fromJsonReader(jsonReader: JsonReader): SubMapPersistencePreferences {
            val retVal =
                SubMapPersistencePreferences()
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