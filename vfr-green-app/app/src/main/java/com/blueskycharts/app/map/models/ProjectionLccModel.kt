package com.blueskycharts.app.map.models

import android.util.JsonReader

class ProjectionLccModel(
    val lat0: Double?,
    val lat1: Double?,
    val lat2: Double?,
    val lon0: Double?,
    val x0: Double?,
    val y0: Double?,
    val extents: ExtentModel?
) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): ProjectionLccModel {
            var lat0: Double? = null
            var lat1: Double? = null
            var lat2: Double? = null
            var lon0: Double? = null
            var x0: Double? = null
            var y0: Double? = null
            var extents: ExtentModel? = null

            reader.beginObject()
            while ( reader.hasNext() ) {
                when (reader.nextName()) {
                    "lat0" -> {
                        lat0 = reader.nextDouble()
                    }
                    "lat1" -> {
                        lat1 = reader.nextDouble()
                    }
                    "lat2" -> {
                        lat2 = reader.nextDouble()
                    }
                    "lon0" -> {
                        lon0 = reader.nextDouble()
                    }
                    "x0" -> {
                        x0 = reader.nextDouble()
                    }
                    "y0" -> {
                        y0 = reader.nextDouble()
                    }
                    "extents" -> {
                        extents = ExtentModel.readFromJsonReader(reader)
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject();

            return ProjectionLccModel(
                lat0 = lat0,
                lat1 = lat1,
                lat2 = lat2,
                lon0 = lon0,
                x0 = x0,
                y0 = y0,
                extents = extents
            )
        }
    }
}