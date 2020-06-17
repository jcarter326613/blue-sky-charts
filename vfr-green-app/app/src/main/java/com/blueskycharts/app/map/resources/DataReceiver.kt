package com.blueskycharts.app.map.resources

import com.blueskycharts.app.coordinates.PointWebMercator

interface DataReceiver {
    fun receiveData(location: PointWebMercator, data: Any, dataAgeSeconds: Int, immediate: Boolean)
}