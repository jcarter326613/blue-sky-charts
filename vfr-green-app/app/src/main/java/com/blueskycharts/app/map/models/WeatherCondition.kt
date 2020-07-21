package com.blueskycharts.app.map.models

data class WeatherCondition (
    var longitude: Float?,
    var latitude: Float?,
    var issueAgeSeconds: Long?,
    var ceiling: Int?,
    var visibility: Int?,
    var cloudCover: String?,
    var windSpeed: Int?,
    var windDirection: String?,
    var windGust: Int?,
    var temperatureCelcius: Int?,
    var dewpointSpreadCelcius: Int?,
    var flightCategory: String?
)