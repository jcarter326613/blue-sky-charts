package com.blueskycharts.app.coordinates

import kotlin.math.PI
import kotlin.math.sqrt

class PointLcc(override var x: Double = 0.0, override var y: Double = 0.0, val projection: ProjectionLccDescription): Location {
    override fun convertToPointGeo(): PointGeo {
        return projection.createPointGeo(this)
    }

    override fun convertToPoint2d(): Point2d {
        return Point2d(x, y)
    }
}