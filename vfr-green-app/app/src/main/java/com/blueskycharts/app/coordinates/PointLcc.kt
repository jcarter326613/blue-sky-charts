package com.blueskycharts.app.coordinates

class PointLcc(override var x: Double = 0.0, override var y: Double = 0.0): Location {
    override fun convertToPointGeo(): PointGeo {
        throw Error("Not implemented")
    }
}