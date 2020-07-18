package com.blueskycharts.app.utility

import android.util.JsonReader
import android.util.JsonWriter

interface JsonSerializable {
    suspend fun write(jsonWriter: JsonWriter)
}