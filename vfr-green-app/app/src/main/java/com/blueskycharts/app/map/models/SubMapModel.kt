package com.blueskycharts.app.map.models

import android.util.JsonReader
import com.blueskycharts.app.coordinates.Point2d
import java.util.*

class SubMapModel ( val tileWidth: Int?,
                    val version: String?,
                    val effectiveDate: Date?,
                    val expirationDate: Date?,
                    val imageWidth: Double?,
                    val imageHeight: Double?,
                    val projectionWebMercator: ProjectionWebMercatorModel?,
                    val projectionLcc: ProjectionLccModel?,
                    val maxZoom: Int?,
                    val changeSet: ChangeSet?
) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): SubMapModel {
            var tileWidth: Int? = null
            var version: String? = null
            var imageWidth: Double? = null
            var imageHeight: Double? = null
            var maxZoom: Int? = null
            var changeSet: ChangeSet? = null
            var projectionLcc: ProjectionLccModel? = null
            var projectionWebMercator: ProjectionWebMercatorModel? = null
            var effectiveDate: Date? = null
            var expirationDate: Date? = null

            reader.beginObject()
            while ( reader.hasNext() ) {
                when (reader.nextName()) {
                    "imageHeight" -> {
                        imageHeight = reader.nextDouble()
                    }
                    "imageWidth" -> {
                        imageWidth = reader.nextDouble()
                    }
                    "maxZoom" -> {
                        maxZoom = reader.nextInt()
                    }
                    "tileWidth" -> {
                        tileWidth = reader.nextInt()
                    }
                    "version" -> {
                        version = reader.nextString()
                    }
                    "changeSet" -> {
                        changeSet = ChangeSet.readFromJsonReader(reader)
                    }
                    "projectionLcc" -> {
                        projectionLcc = ProjectionLccModel.readFromJsonReader(reader)
                    }
                    "projectionWebMercator" -> {
                        projectionWebMercator = ProjectionWebMercatorModel.readFromJsonReader(reader)
                    }
                    "effectiveDate" -> {
                        val effectiveDateString = reader.nextString()
                        effectiveDate = extractDateTime(effectiveDateString)
                    }
                    "expirationDate" -> {
                        val expirationDateString = reader.nextString()
                        expirationDate = extractDateTime(expirationDateString)
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject();

            return SubMapModel(
                tileWidth = tileWidth,
                version = version,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                maxZoom = maxZoom,
                changeSet = changeSet,
                projectionLcc = projectionLcc,
                projectionWebMercator = projectionWebMercator,
                effectiveDate = effectiveDate,
                expirationDate = expirationDate
            )
        }

        @Suppress("DEPRECATION")
        private fun extractDateTime(s: String): Date? {
            var effectiveDate: Date? = null
            if (s.length == 10) {
                effectiveDate = Date(s.substring(0, 4).toInt() - 1900, s.substring(5, 7).toInt() - 1, s.substring(8, 10).toInt())
            } else if (s.length == 16) {
                effectiveDate = Date(s.substring(0, 4).toInt() - 1900, s.substring(5, 7).toInt() - 1, s.substring(8, 10).toInt(), s.substring(11, 13).toInt(), s.substring(14, 16).toInt())
            }

            return effectiveDate
        }
    }

    class ChangeSet(val tiles: ArrayList<Map<Int, ArrayList<Int>>>?) {
        companion object {
            fun readFromJsonReader(reader: JsonReader): ChangeSet {
                var tiles: ArrayList<Map<Int, ArrayList<Int>>>? = null

                reader.beginObject()
                while ( reader.hasNext() ) {
                    when (reader.nextName()) {
                        "tiles" -> {
                            tiles = arrayListOf()
                            reader.beginArray()
                            while ( reader.hasNext() ) {
                                val zoomRecord = mutableMapOf<Int, ArrayList<Int>>()
                                reader.beginObject()
                                while ( reader.hasNext() ) {
                                    val x = reader.nextName().toInt()
                                    reader.beginArray()
                                    val yArray = arrayListOf<Int>()
                                    while ( reader.hasNext() ) {
                                        val y = reader.nextInt()
                                        yArray.add(y)
                                    }
                                    reader.endArray()
                                    zoomRecord[x] = yArray
                                }
                                reader.endObject()
                                tiles.add(zoomRecord)
                            }
                            reader.endArray()
                        }
                        else -> {
                            reader.skipValue()
                        }
                    }
                }
                reader.endObject();

                return ChangeSet(
                    tiles = tiles
                )
            }
        }
    }
}
