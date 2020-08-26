package com.blueskycharts.app.map.models

import android.util.JsonReader

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
    var windGustDifference: Int?,
    var temperatureCelcius: Int?,
    var dewpointSpreadCelcius: Int?,
    var flightCategory: String?
) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): WeatherCondition {
            var longitude: Float? = null
            var latitude: Float? = null
            var issueAgeSeconds: Long? = null
            var ceiling: Int? = null
            var visibility: Int? = null
            var cloudCover: String? = null
            var windSpeed: Int? = null
            var windDirection: String? = null
            var windGust: Int? = null
            var windGustDifference: Int? = null
            var temperatureCelcius: Int? = null
            var dewpointSpreadCelcius: Int? = null
            var flightCategory: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "longitude" -> {
                        longitude = reader.nextDouble().toFloat()
                    }
                    "latitude" -> {
                        latitude = reader.nextDouble().toFloat()
                    }
                    "issueAgeSeconds" -> {
                        issueAgeSeconds = reader.nextLong()
                    }
                    "ceiling" -> {
                        ceiling = reader.nextInt()
                    }
                    "visibility" -> {
                        visibility = reader.nextInt()
                    }
                    "cloudCover" -> {
                        cloudCover = reader.nextString()
                    }
                    "windSpeed" -> {
                        windSpeed = reader.nextInt()
                    }
                    "windDirection" -> {
                        windDirection = reader.nextString()
                    }
                    "windGust" -> {
                        windGust = reader.nextInt()
                    }
                    "windGustDifference" -> {
                        windGustDifference = reader.nextInt()
                    }
                    "temperatureCelcius" -> {
                        temperatureCelcius = reader.nextInt()
                    }
                    "dewpointSpreadCelcius" -> {
                        dewpointSpreadCelcius = reader.nextInt()
                    }
                    "flightCategory" -> {
                        flightCategory = reader.nextString()
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject();

            return WeatherCondition(
                longitude = longitude,
                latitude = latitude,
                issueAgeSeconds = issueAgeSeconds,
                ceiling = ceiling,
                visibility = visibility,
                cloudCover = cloudCover,
                windSpeed = windSpeed,
                windDirection = windDirection,
                windGust = windGust,
                windGustDifference = windGustDifference,
                temperatureCelcius = temperatureCelcius,
                dewpointSpreadCelcius = dewpointSpreadCelcius,
                flightCategory = flightCategory
            )
        }
    }
}