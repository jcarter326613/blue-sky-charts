package com.blueskycharts.app.map.models

import com.blueskycharts.app.coordinates.Point2d

data class SubMapModel ( val mapBounds: Array<Point2d>?,
                         val tileWidth: Int?,
                         val version: String?,
                         val imageWidth: Int?,
                         val imageHeight: Int?,
                         val imageWidthScale: Float?,
                         val imageHeightScale: Float?,
                         val fileExtent: BoxGeoModel?,
                         val maxZoom: Int? ) {

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

        return true
    }

    override fun hashCode(): Int {
        var result = mapBounds?.contentHashCode() ?: 0
        result = 31 * result + (tileWidth ?: 0)
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (imageWidth ?: 0)
        result = 31 * result + (imageHeight ?: 0)
        result = 31 * result + (imageWidthScale?.hashCode() ?: 0)
        result = 31 * result + (imageHeightScale?.hashCode() ?: 0)
        result = 31 * result + (fileExtent?.hashCode() ?: 0)
        result = 31 * result + (maxZoom ?: 0)
        return result
    }
}
