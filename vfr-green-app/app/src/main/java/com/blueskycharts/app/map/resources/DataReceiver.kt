package com.blueskycharts.app.map.resources

import com.blueskycharts.app.coordinates.PointWebMercator
import com.blueskycharts.app.map.models.WeatherConditionResponse

interface DataReceiver {
    fun receiveData(location: PointWebMercator, data: WeatherConditionResponse, dataAgeSeconds: Int, immediate: Boolean)
}