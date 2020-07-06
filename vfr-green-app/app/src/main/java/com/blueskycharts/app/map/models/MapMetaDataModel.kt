package com.blueskycharts.app.map.models

import android.util.JsonReader

data class MapMetaDataModel ( val versions: HashMap<String, SubMapModel>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapMetaDataModel

        if (versions != other.versions) return false

        return true
    }

    override fun hashCode(): Int {
        return versions.hashCode()
    }

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