package com.blueskycharts.app.map.models

import android.util.JsonReader

data class WeatherConditionResponse (
    var oldestDataAgeSeconds: Int? = null,
    var conditions: List<WeatherCondition>? = null
) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): WeatherConditionResponse {
            var oldestDataAgeSeconds: Int? = null
            var conditions: MutableList<WeatherCondition>? = null

            reader.beginObject()
            while ( reader.hasNext() ) {
                when (reader.nextName()) {
                    "oldestDataAgeSeconds" -> {
                        oldestDataAgeSeconds = reader.nextInt()
                    }
                    "conditions" -> {
                        conditions = mutableListOf()
                        reader.beginArray()
                        while (reader.hasNext()) {
                            conditions.add(WeatherCondition.readFromJsonReader(reader))
                        }
                        reader.endArray()
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject();

            return WeatherConditionResponse(
                oldestDataAgeSeconds = oldestDataAgeSeconds,
                conditions = conditions
            )
        }
    }
}