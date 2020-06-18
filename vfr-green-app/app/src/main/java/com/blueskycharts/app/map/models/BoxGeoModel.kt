package com.blueskycharts.app.map.models

import android.util.JsonReader
import com.blueskycharts.app.coordinates.BoxGeo
import com.blueskycharts.app.coordinates.PointGeo

data class BoxGeoModel( val topLeft: PointGeo?,
                        val topRight: PointGeo?,
                        val bottomLeft: PointGeo?,
                        val bottomRight: PointGeo? ) {

    public fun createBoxGeo(): BoxGeo {
        return BoxGeo(topLeft ?: PointGeo(), bottomRight ?: PointGeo(), topRight, bottomLeft);
    }

    companion object {
        fun readFromJsonReader(reader: JsonReader): BoxGeoModel {
            var topLeft: PointGeo? = null
            var topRight: PointGeo? = null
            var bottomLeft: PointGeo? = null
            var bottomRight: PointGeo? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when ( reader.nextName() ) {
                    "bottomLeft" -> {
                        bottomLeft = readPointGeoFromJsonReader(reader)
                    }
                    "bottomRight" -> {
                        bottomRight = readPointGeoFromJsonReader(reader)
                    }
                    "topLeft" -> {
                        topLeft = readPointGeoFromJsonReader(reader)
                    }
                    "topRight" -> {
                        topRight = readPointGeoFromJsonReader(reader)
                    }
                }
            }
            reader.endObject()

            return BoxGeoModel(topLeft, topRight, bottomLeft, bottomRight)
        }

        private fun readPointGeoFromJsonReader(reader: JsonReader): PointGeo? {
            var latitude: Double? = null
            var longitude: Double? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when ( reader.nextName() ) {
                    "latitude" -> {
                        latitude = reader.nextDouble()
                    }
                    "longitude" -> {
                        longitude = reader.nextDouble()
                    }
                }
            }
            reader.endObject()

            if ( latitude == null || longitude == null ) {
                return null;
            }
            return PointGeo(longitude, latitude)
        }
    }
};
