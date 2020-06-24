package com.blueskycharts.app.map.models

object WeatherConditionResponse {
    var oldestDataAgeSeconds: Int? = null
    var conditions: Collection<WeatherCondition>? = null
}