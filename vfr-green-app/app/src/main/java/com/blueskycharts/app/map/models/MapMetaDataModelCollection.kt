package com.blueskycharts.app.map.models

import android.util.JsonReader
import com.blueskycharts.app.coordinates.Point2d
import java.util.*
import kotlin.collections.HashMap

class MapMetaDataModelCollection (val maps: MutableMap<String, MapMetaDataModel>) {
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