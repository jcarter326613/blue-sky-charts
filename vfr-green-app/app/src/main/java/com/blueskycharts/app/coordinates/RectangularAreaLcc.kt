package com.blueskycharts.app.coordinates

import kotlin.math.max
import kotlin.math.min

class RectangularAreaLcc(topLeftX: Double = 0.0, topLeftY: Double = 0.0, bottomRightX: Double = 0.0, bottomRightY: Double = 0.0, val projectionDescription: ProjectionLccDescription):
    RectangularArea(PointLcc(topLeftX, topLeftY), PointLcc(bottomRightX, bottomRightY)) {
    override val width: Double
        get() = this.bottomRight.x - this.topLeft.x
    override val height: Double
        get() = this.bottomRight.y - this.topLeft.y

    override fun convertToBox2d(): Box2d {
        return Box2d(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    override fun intersection(o: RectangularArea): RectangularArea? {
        o as RectangularAreaLcc

        val newBox = RectangularAreaLcc(
            max(topLeft.x, o.topLeft.x),
            max(topLeft.y, o.topLeft.y),
            min(bottomRight.x, o.bottomRight.x),
            min(bottomRight.y, o.bottomRight.y),
            projectionDescription
        )

        if (newBox.bottomRight.x < newBox.topLeft.x || newBox.bottomRight.y < newBox.topLeft.y) {
            return null;
        }

        return newBox
    }

    override fun union(o: RectangularArea): RectangularArea {
        o as RectangularAreaLcc

        return RectangularAreaLcc(
            min(topLeft.x, o.topLeft.x),
            min(topLeft.y, o.topLeft.y),
            max(bottomRight.x, o.bottomRight.x),
            max(bottomRight.y, o.bottomRight.y),
            projectionDescription
        )
    }

    override fun cropPercentageUpperLeft(o: Box2d): RectangularArea {
        return RectangularAreaLcc(
            this.topLeft.x + this.width * o.upperLeft.x,
            this.topLeft.y + this.height * o.upperLeft.y,
            this.topLeft.x + this.width * o.lowerRight.x,
            this.topLeft.y + this.height * o.lowerRight.y,
            projectionDescription
        )
    }

    override fun overlapPercentageUpperLeft(o: RectangularArea): Box2d {
        o as RectangularAreaLcc
        val topLeftX = (o.topLeft.x - this.topLeft.x) / this.width
        val topLeftY = (o.topLeft.y - this.topLeft.y) / this.height
        val bottomRightX = (o.bottomRight.x - this.topLeft.x) / this.width
        val bottomRightY = (o.bottomRight.y - this.topLeft.y) / this.height
        return Box2d(topLeftX, topLeftY, bottomRightX, bottomRightY)
    }

    override fun positionPercentageUpperLeft(p: Location): Point2d {
        p as PointLcc
        val x = (p.x - this.topLeft.x) / this.width
        val y = (p.y - this.topLeft.y) / this.height
        return Point2d(x, y)
    }

    override fun getBoundingBoxGeo(rules: RectangularArea.BoundingRules): BoxGeo {
        // TODO: need to find highest latitude at lon0.  Lowest is at one of the bottom corners
        throw Error("Not implemented")
    }
}