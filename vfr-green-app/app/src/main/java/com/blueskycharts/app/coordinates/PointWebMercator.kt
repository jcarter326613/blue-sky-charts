package com.blueskycharts.app.coordinates

class PointWebMercator(var x: Double = 0.0, var y: Double = 0.0) {
    fun clone(): PointWebMercator {
        return PointWebMercator(x, y);
    }

    fun createBoxAround(size: PointWebMercator): BoxWebMercator {
        return BoxWebMercator(this.x - (size.x / 2.0), this.y + (size.y / 2.0), this.x + (size.x / 2.0), this.y - (size.y / 2.0))
    }
}