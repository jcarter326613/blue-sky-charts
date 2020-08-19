package com.blueskycharts.app.map.models

import android.util.JsonReader

class MapMetaDataModel (
    val humanName: String?,
    val versions: MutableMap<String, SubMapModel>
) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): MapMetaDataModel {
            val versionMap = mutableMapOf<String, SubMapModel>()
            var humanName: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when( reader.nextName() ) {
                    "humanName" -> {
                        humanName = reader.nextString()
                    }
                    "versions" -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val versionName = reader.nextName()
                            val subMap = SubMapModel.readFromJsonReader(reader)
                            versionMap[versionName] = subMap
                        }
                        reader.endObject()
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject()

            return MapMetaDataModel(humanName, versionMap)
        }
    }
}