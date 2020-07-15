package com.blueskycharts.app.map.resources

import android.graphics.Canvas
import com.blueskycharts.app.coordinates.PointGeo
import com.blueskycharts.app.coordinates.PointWebMercator
import com.blueskycharts.app.map.models.WeatherCondition
import com.blueskycharts.app.map.models.WeatherConditionResponse

interface DataReceiver {
    fun receiveData(location: PointGeo, data: WeatherCondition, dataAgeSeconds: Int, immediate: Boolean, canvas: Canvas?, receiverData: Any?)
}