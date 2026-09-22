package com.blueskycharts.app.coordinates

import kotlin.math.pow
import kotlin.math.sqrt

open class Point2d(var x: Double = 0.0, var y: Double = 0.0) {
    public fun clone(): Point2d {
        return Point2d(x, y);
    }

    public fun calculateDistance(o: Point2d): Double {
        return sqrt((this.x - o.x).pow(2) + (this.y - o.y).pow(2));
    }

    fun createBoxAround(size: Point2d): Box2d {
        return Box2d(this.x - (size.x / 2.0), this.y - (size.y / 2.0), this.x + (size.x / 2.0), this.y + (size.y / 2.0))
    }
}
