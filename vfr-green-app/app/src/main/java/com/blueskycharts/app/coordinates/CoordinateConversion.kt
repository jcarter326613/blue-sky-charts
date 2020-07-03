package com.blueskycharts.app.coordinates

import kotlin.math.*

object CoordinateConversion {
    const val MAX_LONGITUDE: Double = 180.0
    const val MIN_LONGITUDE: Double = -180.0
    const val MAX_LATITUDE: Double = 85.0
    const val MIN_LATITUDE: Double = -85.0

    private var maxMercator: BoxWebMercator? = null

    /**
     * Converts the given longitude and latitude to a Web Mercator projection where the upper left is (0,0) and the lower right is (256, 256)
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     * @param geoPoint
     */
    fun convertPointGeoToPointWebMercator(geoPoint: PointGeo): PointWebMercator {
        val newPoint = PointWebMercator();

        var longitude = geoPoint.longitude
        var latitude = geoPoint.latitude

        if ( latitude > CoordinateConversion.MAX_LATITUDE ) {
            latitude = CoordinateConversion.MAX_LATITUDE
        } else if ( latitude < CoordinateConversion.MIN_LATITUDE ) {
            latitude = CoordinateConversion.MIN_LATITUDE
        }

        if ( longitude > CoordinateConversion.MAX_LONGITUDE ) {
            longitude = CoordinateConversion.MAX_LONGITUDE
        } else if ( longitude < CoordinateConversion.MIN_LONGITUDE ) {
            longitude = CoordinateConversion.MIN_LONGITUDE
        }

        newPoint.x = longitude * 20037508.34 / 180.0
        newPoint.y = log(tan((90.0 + latitude) * PI / 360.0), E) * (20037508.34 / PI)

        return newPoint
    }

    /**
     * Converts the given x,y coordinates to latitude and longitude.  Min x and y are (0,0) and max is (256, 256).
     * @param point2d
     */
    fun convertPointWebMercatorToPointGeo(point2d: PointWebMercator): PointGeo {
        var longitude = point2d.x * 180.0 / 20037508.34
        var latitude = atan(exp(point2d.y * PI / 20037508.34)) * 360.0 / PI - 90.0

        if ( latitude > CoordinateConversion.MAX_LATITUDE ) {
            latitude = CoordinateConversion.MAX_LATITUDE
        } else if ( latitude < CoordinateConversion.MIN_LATITUDE ) {
            latitude = CoordinateConversion.MIN_LATITUDE
        }

        if ( longitude > CoordinateConversion.MAX_LONGITUDE ) {
            longitude = CoordinateConversion.MAX_LONGITUDE
        } else if ( longitude < CoordinateConversion.MIN_LONGITUDE ) {
            longitude = CoordinateConversion.MIN_LONGITUDE
        }

        return PointGeo(longitude, latitude)
    }

    private fun getMaxMercator(): BoxWebMercator {
        var maxMercator = CoordinateConversion.maxMercator
        if ( maxMercator == null ) {
            maxMercator = CoordinateConversion.convertBoxGeoToBoxMercator(BoxGeo(
                PointGeo(CoordinateConversion.MIN_LONGITUDE, CoordinateConversion.MAX_LATITUDE),
                PointGeo(CoordinateConversion.MAX_LONGITUDE, CoordinateConversion.MIN_LATITUDE)))
            CoordinateConversion.maxMercator = maxMercator
        }

        return maxMercator
    }

    fun convertPointMercatorToPoint2d(pointMercator: PointWebMercator): Point2d {
        val maxMercator = CoordinateConversion.getMaxMercator()
        return Point2d(pointMercator.x, maxMercator.topLeft.y - pointMercator.y)
    }

    fun convertPoint2dToPointMercator(point2d: Point2d): PointWebMercator {
        val maxMercator = CoordinateConversion.getMaxMercator()
        return PointWebMercator(point2d.x, maxMercator.topLeft.y - point2d.y)
    }

    fun convertBoxMercatorToBoxGeo(boxMercator: BoxWebMercator): BoxGeo {
        val boxGeo = BoxGeo(
            convertPointWebMercatorToPointGeo(boxMercator.topLeft),
            convertPointWebMercatorToPointGeo(boxMercator.bottomRight)
        );
        return boxGeo
    }

    fun convertBoxMercatorToBox2d(boxMercator: BoxWebMercator): Box2d {
        val topLeft = CoordinateConversion.convertPointMercatorToPoint2d(boxMercator.topLeft)
        val bottomRight = CoordinateConversion.convertPointMercatorToPoint2d(boxMercator.bottomRight)
        return Box2d(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    fun convertBox2dToBoxMercator(box2d: Box2d): BoxWebMercator {
        val topLeft = CoordinateConversion.convertPoint2dToPointMercator(box2d.upperLeft)
        val bottomRight = CoordinateConversion.convertPoint2dToPointMercator(box2d.lowerRight)
        return BoxWebMercator(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    /**
     * Assumes the 2d box is actually web mercator coordinates with origin in the upper left
     * @param boxMercator
     */
    fun convertBox2dToBoxGeo(box2d: Box2d): BoxGeo {
        return this.convertBoxMercatorToBoxGeo(this.convertBox2dToBoxMercator(box2d))
    }

    fun convertBoxGeoToBoxMercator(boxGeo: BoxGeo): BoxWebMercator {
        val topLeft = convertPointGeoToPointWebMercator(boxGeo.topLeft);
        val bottomRight = convertPointGeoToPointWebMercator(boxGeo.bottomRight);
        val boxMercator = BoxWebMercator(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
        return boxMercator;
    }
}