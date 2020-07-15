package com.blueskycharts.app.map.models

import android.util.JsonReader

class ExtentModel(
    val left: Double?,
    val top: Double?,
    val right: Double?,
    val bottom: Double?
) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): ExtentModel {
            var left: Double? = null
            var top: Double? = null
            var right: Double? = null
            var bottom: Double? = null

            reader.beginObject()
            while ( reader.hasNext() ) {
                when (reader.nextName()) {
                    "left" -> {
                        left = reader.nextDouble()
                    }
                    "top" -> {
                        top = reader.nextDouble()
                    }
                    "right" -> {
                        right = reader.nextDouble()
                    }
                    "bottom" -> {
                        bottom = reader.nextDouble()
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject();

            return ExtentModel(
                left = left,
                top = top,
                right = right,
                bottom = bottom
            )
        }
    }
}