package com.blueskycharts.app.coordinates

import kotlin.math.*

data class PointGeo(var longitude: Double = 0.0, var latitude: Double = 0.0) {
    /**
     * Converts the given longitude and latitude to a Web Mercator projection where the upper left is (0,0) and the lower right is (256, 256)
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     */
    fun convertToPointWebMercator(): PointWebMercator {
        val newPoint = PointWebMercator();

        if ( latitude > maxLatitude ) {
            latitude = maxLatitude
        } else if ( latitude < minLatitude ) {
            latitude = minLatitude
        }

        if ( longitude > maxLongitude ) {
            longitude = maxLongitude
        } else if ( longitude < minLongitude ) {
            longitude = minLongitude
        }

        newPoint.x = longitude * 20037508.34 / 180.0
        newPoint.y = log(tan((90.0 + latitude) * PI / 360.0), E) * (20037508.34 / PI)

        return newPoint
    }

    //https://github.com/vraida/Lambert-projection/blob/master/python_implementation/lib/lambert.py
    fun convertToPointLcc(projection: ProjectionLccDescription): PointLcc {
        return projection.createPointLcc(this)
    }

    companion object {
        const val maxLongitude: Double = 180.0
        const val minLongitude: Double = -180.0
        const val maxLatitude: Double = 85.0
        const val minLatitude: Double = -85.0
    }
}