package com.blueskycharts.app.coordinates

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp

class PointWebMercator(override var x: Double = 0.0, override var y: Double = 0.0): Location {
    fun clone(): PointWebMercator {
        return PointWebMercator(x, y);
    }

    override fun convertToPointGeo(): PointGeo {
        var longitude = x * 180.0 / 20037508.34
        var latitude = atan(exp(y * PI / 20037508.34)) * 360.0 / PI - 90.0

        if ( latitude > PointGeo.maxLatitude ) {
            latitude = PointGeo.maxLatitude
        } else if ( latitude < PointGeo.minLatitude ) {
            latitude = PointGeo.minLatitude
        }

        if ( longitude > PointGeo.maxLongitude ) {
            longitude = PointGeo.maxLongitude
        } else if ( longitude < PointGeo.minLongitude ) {
            longitude = PointGeo.minLongitude
        }

        return PointGeo(longitude, latitude)
    }

    override fun convertToPoint2d(): Point2d {
        return RectangularAreaWebMercator.convertPointWebMercatorToPoint2d(this)
    }
}