package com.blueskycharts.app.map.models

import android.util.JsonReader
import com.blueskycharts.app.coordinates.Point2d
import java.util.*

data class SubMapModel ( val mapBounds: Array<Point2d>?,
                         val tileWidth: Int?,
                         val version: String?,
                         val imageWidth: Double?,
                         val imageHeight: Double?,
                         val imageWidthScale: Double?,
                         val imageHeightScale: Double?,
                         val fileExtent: BoxGeoModel?,
                         val maxZoom: Int?,
                         val name: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubMapModel

        if (mapBounds != null) {
            if (other.mapBounds == null) return false
            if (!mapBounds.contentEquals(other.mapBounds)) return false
        } else if (other.mapBounds != null) return false
        if (tileWidth != other.tileWidth) return false
        if (version != other.version) return false
        if (imageWidth != other.imageWidth) return false
        if (imageHeight != other.imageHeight) return false
        if (imageWidthScale != other.imageWidthScale) return false
        if (imageHeightScale != other.imageHeightScale) return false
        if (fileExtent != other.fileExtent) return false
        if (maxZoom != other.maxZoom) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mapBounds?.contentHashCode() ?: 0
        result = 31 * result + (tileWidth ?: 0)
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (imageWidth?.hashCode() ?: 0)
        result = 31 * result + (imageHeight?.hashCode() ?: 0)
        result = 31 * result + (imageWidthScale?.hashCode() ?: 0)
        result = 31 * result + (imageHeightScale?.hashCode() ?: 0)
        result = 31 * result + (fileExtent?.hashCode() ?: 0)
        result = 31 * result + (maxZoom ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun readFromJsonReader(reader: JsonReader): Collection<SubMapModel> {
            val retVal = LinkedList<SubMapModel>()

            reader.beginObject()
            while (reader.hasNext()) {
                val mapBounds: Array<Point2d>? = null
                var tileWidth: Int? = null
                var version: String? = null
                var imageWidth: Double? = null
                var imageHeight: Double? = null
                var imageWidthScale: Double? = null
                var imageHeightScale: Double? = null
                var fileExtent: BoxGeoModel? = null
                var maxZoom: Int? = null
                val mapName = reader.nextName();

                reader.beginObject()
                while ( reader.hasNext() ) {
                    when (reader.nextName()) {
                        "fileExtent" -> {
                            fileExtent = BoxGeoModel.readFromJsonReader(reader)
                        }
                        "imageHeight" -> {
                            imageHeight = reader.nextDouble()
                        }
                        "imageHeightScale" -> {
                            imageHeightScale = reader.nextDouble()
                        }
                        "imageWidth" -> {
                            imageWidth = reader.nextDouble()
                        }
                        "imageWidthScale" -> {
                            imageWidthScale = reader.nextDouble()
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
                    }
                }
                reader.endObject();

                val mapModel = SubMapModel(
                    mapBounds = mapBounds,
                    tileWidth = tileWidth,
                    version = version,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    imageWidthScale = imageWidthScale,
                    imageHeightScale = imageHeightScale,
                    fileExtent = fileExtent,
                    maxZoom = maxZoom,
                    name = mapName
                )
                retVal.add(mapModel)
            }
            reader.endObject()

            return retVal
        }
    }
}
