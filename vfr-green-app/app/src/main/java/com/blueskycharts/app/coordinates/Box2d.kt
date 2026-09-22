package com.blueskycharts.app.coordinates

import kotlin.math.max
import kotlin.math.min

/**
 * Display coordinates.  Upper left is (0,0)
 */
class Box2d(upperLeftX: Double = 0.0, upperLeftY: Double = 0.0, lowerRightX: Double = 0.0, lowerRightY: Double = 0.0) {
    var upperLeft: Point2d
        private set;
    var lowerRight: Point2d
        private set;
    val width: Double
        get() = this.lowerRight.x - this.upperLeft.x
    val height: Double
        get() = this.lowerRight.y - this.upperLeft.y

    init {
        upperLeft = Point2d(upperLeftX, upperLeftY)
        lowerRight = Point2d(lowerRightX, lowerRightY)
    }

    fun clone(): Box2d {
        val newRect = Box2d();
        newRect.upperLeft = this.upperLeft.clone()
        newRect.lowerRight = this.lowerRight.clone()
        return newRect;
    }

    fun shift(p: Point2d, reverse: Boolean): Box2d {
        return if ( reverse ) {
            Box2d(
                this.upperLeft.x - p.x,
                this.upperLeft.y - p.y,
                this.lowerRight.x - p.x,
                this.lowerRight.y - p.y
            )
        } else {
            Box2d(
                this.upperLeft.x + p.x,
                this.upperLeft.y + p.y,
                this.lowerRight.x + p.x,
                this.lowerRight.y + p.y
            )
        }
    }

    fun scale(s: Double): Box2d {
        return Box2d(upperLeft.x * s, upperLeft.y * s, lowerRight.x * s, lowerRight.y * s)
    }

    fun overlapPercentageUpperLeft(o: Box2d): Box2d {
        val topLeftX = (o.upperLeft.x - this.upperLeft.x) / this.width
        val topLeftY = (o.upperLeft.y - this.upperLeft.y) / this.height
        val bottomRightX = (o.lowerRight.x - this.upperLeft.x) / this.width
        val bottomRightY = (o.lowerRight.y - this.upperLeft.y) / this.height
        return Box2d(topLeftX, topLeftY, bottomRightX, bottomRightY)
    }

    fun contains(o: Point2d): Boolean {
        return o.x >= this.upperLeft.x &&
                o.x <= this.lowerRight.x &&
                o.y >= this.upperLeft.y &&
                o.y <= this.lowerRight.y
    }

    /**
     * Returns the intersection of the two boxes.  An real number range is allowed.
     * @param o
     */
    fun intersection(o: Box2d): Box2d? {
        val newBox = Box2d(
            max(this.upperLeft.x, o.upperLeft.x),
            max(this.upperLeft.y, o.upperLeft.y),
            min(this.lowerRight.x, o.lowerRight.x),
            min(this.lowerRight.y, o.lowerRight.y)
        )

        if (newBox.lowerRight.x < newBox.upperLeft.x || newBox.lowerRight.y < newBox.upperLeft.y) {
            return null
        }

        return newBox
    }

    fun union(o: Box2d): Box2d {
        return Box2d(
            min(this.upperLeft.x, o.upperLeft.x),
            min(this.upperLeft.y, o.upperLeft.y),
            max(this.lowerRight.x, o.lowerRight.x),
            max(this.lowerRight.y, o.lowerRight.y)
        )
    }
}