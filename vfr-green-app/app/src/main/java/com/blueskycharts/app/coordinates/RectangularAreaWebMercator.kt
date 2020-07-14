package com.blueskycharts.app.coordinates

import kotlin.math.max;
import kotlin.math.min;

/**
 * The point (0,0) is in the lower right
 */
class RectangularAreaWebMercator(topLeftX: Double = 0.0, topLeftY: Double = 0.0, bottomRightX: Double = 0.0, bottomRightY: Double = 0.0):
    RectangularArea(PointWebMercator(topLeftX, topLeftY), PointWebMercator(bottomRightX, bottomRightY)) {
    override val width: Double
        get() = this.bottomRight.x - this.topLeft.x
    override val height: Double
        get() = this.topLeft.y - this.bottomRight.y

    override fun convertToBox2d(): Box2d {
        val topLeft = Point2d(topLeft.x, maxMercator.topLeft.y - topLeft.y)
        val bottomRight = Point2d(bottomRight.x, maxMercator.bottomRight.y - bottomRight.y)
        return Box2d(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    /**
     * Returns the intersection of the two boxes.  An real number range is allowed.
     * @param o
     */
    override fun intersection(o: RectangularArea): RectangularArea? {
        o as RectangularAreaWebMercator

        val newBox = RectangularAreaWebMercator(
            max(this.topLeft.x, o.topLeft.x),
            min(this.topLeft.y, o.topLeft.y),
            min(this.bottomRight.x, o.bottomRight.x),
            max(this.bottomRight.y, o.bottomRight.y)
        )

        if (newBox.bottomRight.x < newBox.topLeft.x || newBox.bottomRight.y > newBox.topLeft.y) {
            return null
        }

        return newBox
    }

    /*
    fun clone(): RectangularAreaWebMercator {
        return RectangularAreaWebMercator(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }
     */

    /**
     * Returns how far along the height and width each corner of the parameter box is relative to the
     * upper left point with x going right and y going down.
     */
    override fun overlapPercentageUpperLeft(o: RectangularArea): Box2d {
        o as RectangularAreaWebMercator
        val topLeftX = (o.topLeft.x - this.topLeft.x) / this.width
        val topLeftY = (this.topLeft.y - o.topLeft.y) / this.height
        val bottomRightX = (o.bottomRight.x - this.topLeft.x) / this.width
        val bottomRightY = (this.topLeft.y - o.bottomRight.y) / this.height
        return Box2d(topLeftX, topLeftY, bottomRightX, bottomRightY)
    }

    override fun positionPercentageUpperLeft(p: Location): Point2d {
        p as PointWebMercator
        val x = (p.x - this.topLeft.x) / this.width
        val y = (this.topLeft.y - p.y) / this.height
        return Point2d(x, y)
    }

    override fun getBoundingBoxGeo(rules: RectangularArea.BoundingRules): BoxGeo {
        throw Error("Not implemented")
    }

    companion object {
        private var _maxMercator: RectangularAreaWebMercator? = null
        val maxMercator: RectangularAreaWebMercator
            get() {
                if ( _maxMercator == null ) {
                    val topLeftGeo = PointGeo(PointGeo.minLongitude, PointGeo.maxLatitude)
                    val bottomRightGeo = PointGeo(PointGeo.maxLongitude, PointGeo.minLatitude)
                    val topLeftMercator = topLeftGeo.convertToPointWebMercator()
                    val bottomRightMercator = bottomRightGeo.convertToPointWebMercator()
                    _maxMercator = RectangularAreaWebMercator(topLeftMercator.x, topLeftMercator.y, bottomRightMercator.x, bottomRightMercator.y)
                }

                return _maxMercator!!
            }
    }
}