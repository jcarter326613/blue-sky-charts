package com.blueskycharts.app.map.models

import android.util.JsonReader

class MapMetaDataModel ( val versions: MutableMap<String, SubMapModel>) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): MapMetaDataModel {
            val retVal = MapMetaDataModel(HashMap())

            reader.beginObject()
            while (reader.hasNext()) {
                when( reader.nextName() ) {
                    "versions" -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val versionName = reader.nextName()
                            val subMap = SubMapModel.readFromJsonReader(reader)
                            retVal.versions[versionName] = subMap
                        }
                        reader.endObject()
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject()

            return retVal
        }
    }
}