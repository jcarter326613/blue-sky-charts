package com.blueskycharts.app.map.models

import android.util.JsonReader
import com.blueskycharts.app.coordinates.Point2d
import java.util.*
import kotlin.collections.HashMap

data class MapMetaDataModelCollection (val maps: MutableMap<String, MapMetaDataModel>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapMetaDataModelCollection

        if (maps != other.maps) return false

        return true
    }

    override fun hashCode(): Int {
        return maps.hashCode()
    }

    companion object {
        fun readFromJsonReader(reader: JsonReader): MapMetaDataModelCollection {
            val retVal = MapMetaDataModelCollection(HashMap())

            reader.beginObject()
            while (reader.hasNext()) {
                val mapName = reader.nextName();
                val model = MapMetaDataModel.readFromJsonReader(reader)

                retVal.maps[mapName] = model
            }
            reader.endObject()

            return retVal
        }
    }
}