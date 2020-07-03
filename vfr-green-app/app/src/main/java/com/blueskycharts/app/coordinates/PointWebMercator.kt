package com.blueskycharts.app.coordinates

import kotlin.math.floor;
import kotlin.math.pow;
import kotlin.math.sqrt

class PointWebMercator(var x: Double = 0.0, var y: Double = 0.0) {
    public fun clone(): PointWebMercator {
        return PointWebMercator(x, y);
    }
}