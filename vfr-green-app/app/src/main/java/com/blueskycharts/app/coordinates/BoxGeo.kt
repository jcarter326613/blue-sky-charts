package com.blueskycharts.app.coordinates

import kotlin.math.max;
import kotlin.math.min;

class BoxGeo(var topLeft: PointGeo = PointGeo(), var bottomRight: PointGeo = PointGeo()) {
    val width: Double
        get() = this.bottomRight.longitude - this.topLeft.longitude
    val height: Double
        get() = this.topLeft.latitude - this.bottomRight.latitude
}