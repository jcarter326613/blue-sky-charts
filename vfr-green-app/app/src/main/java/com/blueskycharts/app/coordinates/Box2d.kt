package com.blueskycharts.app.coordinates

import kotlin.math.max;
import kotlin.math.min;
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
        upperLeft = Point2d(upperLeftX, upperLeftY);
        lowerRight = Point2d(lowerRightX, lowerRightY);
    }

    public fun clone(): Box2d {
        val newRect = Box2d();
        newRect.upperLeft = this.upperLeft.clone();
        newRect.lowerRight = this.lowerRight.clone();
        return newRect;
    }

    /**
     * Returns the intersection of the two boxes.  An real number range is allowed.
     * @param o
     */
    public fun intersection(o: Box2d): Box2d? {
        val newBox = Box2d();
        newBox.upperLeft.x = max(this.upperLeft.x, o.upperLeft.x);
        newBox.upperLeft.y = max(this.upperLeft.y, o.upperLeft.y);
        newBox.lowerRight.x = min(this.lowerRight.x, o.lowerRight.x);
        newBox.lowerRight.y = min(this.lowerRight.y, o.lowerRight.y);

        if (newBox.lowerRight.x < newBox.upperLeft.x || newBox.lowerRight.y < newBox.upperLeft.y) {
            return null;
        }

        return newBox;
    }

    public fun getDimensions(): Point2d {
        return Point2d(this.width, this.height)
    }
}