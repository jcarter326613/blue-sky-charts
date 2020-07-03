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
    public fun intersection(o: BoxWebMercator): BoxWebMercator? {
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
}