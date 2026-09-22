package com.blueskycharts.app.coordinates

interface Location {
    val x: Double
    val y: Double

    fun convertToPointGeo(): PointGeo
    fun convertToPoint2d(): Point2d
}