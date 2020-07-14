package com.blueskycharts.app.map.models

import android.util.JsonReader

class ProjectionWebMercatorModel(
    val extents: ExtentModel?
) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): ProjectionWebMercatorModel {
            var extents: ExtentModel? = null

            reader.beginObject()
            while ( reader.hasNext() ) {
                when (reader.nextName()) {
                    "extents" -> {
                        extents = ExtentModel.readFromJsonReader(reader)
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject();

            return ProjectionWebMercatorModel(
                extents = extents
            )
        }
    }
}