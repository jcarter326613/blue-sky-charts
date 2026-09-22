package com.blueskycharts.app.coordinates

abstract class RectangularArea(
    protected val topLeft: Location,
    protected val bottomRight: Location
) {
    abstract val width: Double
    abstract val height: Double

    abstract fun convertToBox2d(): Box2d
    abstract fun intersection(o: RectangularArea): RectangularArea?
    abstract fun union(o: RectangularArea): RectangularArea
    abstract fun cropPercentageUpperLeft(o: Box2d): RectangularArea
    abstract fun overlapPercentageUpperLeft(o: RectangularArea): Box2d
    abstract fun positionPercentageUpperLeft(p: Location): Point2d
    abstract fun getBoundingBoxGeo(rules: BoundingRules): BoxGeo

    enum class BoundingRules {
        Outside
    }
}