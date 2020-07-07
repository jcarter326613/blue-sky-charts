package com.blueskycharts.app.coordinates

import kotlin.math.max;
import kotlin.math.min;

/**
 * The point (0,0) is in the lower right
 */
class BoxWebMercator(topLeftX: Double = 0.0, topLeftY: Double = 0.0, bottomRightX: Double = 0.0, bottomRightY: Double = 0.0) {
    var topLeft: PointWebMercator
        private set;
    var bottomRight: PointWebMercator
        private set;
    val width: Double
        get() = this.bottomRight.x - this.topLeft.x
    val height: Double
        get() = this.topLeft.y - this.bottomRight.y

    init {
        this.topLeft = PointWebMercator(topLeftX, topLeftY);
        this.bottomRight = PointWebMercator(bottomRightX, bottomRightY);
    }

    /**
     * Returns the intersection of the two boxes.  An real number range is allowed.
     * @param o
     */
    fun intersection(o: BoxWebMercator): BoxWebMercator? {
        val newBox = BoxWebMercator();
        newBox.topLeft.x = max(this.topLeft.x, o.topLeft.x);
        newBox.topLeft.y = min(this.topLeft.y, o.topLeft.y);
        newBox.bottomRight.x = min(this.bottomRight.x, o.bottomRight.x);
        newBox.bottomRight.y = max(this.bottomRight.y, o.bottomRight.y);

        if (newBox.bottomRight.x < newBox.topLeft.x || newBox.bottomRight.y > newBox.topLeft.y) {
            return null;
        }

        return newBox;
    }

    fun clone(): BoxWebMercator {
        val newRect = BoxWebMercator()
        newRect.topLeft = this.topLeft.clone()
        newRect.bottomRight = this.bottomRight.clone()
        return newRect
    }

    /**
     * Returns how far along the height and width each corner of the parameter box is relative to the
     * upper left point with x going right and y going down.
     */
    fun overlapPercentageUpperLeft(o: BoxWebMercator): Box2d {
        val topLeftX = (o.topLeft.x - this.topLeft.x) / this.width
        val topLeftY = (this.topLeft.y - o.topLeft.y) / this.height
        val bottomRightX = (o.bottomRight.x - this.topLeft.x) / this.width
        val bottomRightY = (this.topLeft.y - o.bottomRight.y) / this.height
        return Box2d(topLeftX, topLeftY, bottomRightX, bottomRightY)
    }

    fun positionPercentageUpperLeft(o: PointWebMercator): Point2d {
        val x = (o.x - this.topLeft.x) / this.width
        val y = (this.topLeft.y - o.y) / this.height
        return Point2d(x, y)
    }
}