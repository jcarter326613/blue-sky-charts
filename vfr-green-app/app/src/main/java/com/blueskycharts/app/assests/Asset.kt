package com.blueskycharts.app.assests

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.JsonReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStreamReader
import java.net.URL

class Asset( val description: AssetDescription ) {
    var bytes: ByteArray? = null
    val numBytes: Int
        get() = bytes?.size ?: 0
    var errorLoading: Boolean = false

    fun asJsonReader(): JsonReader? {
        val bytes = this.bytes
        if ( !errorLoading && bytes != null ) return JsonReader(InputStreamReader(bytes.inputStream()))
        return null
    }

    fun asBitmap(): Bitmap? {
        val bytes = this.bytes
        if ( !errorLoading && bytes != null ) return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return null
    }

    /**
     * Assumes the content is \n delimited
     */
    fun asStringArray(): Array<String>? {
        val bytes = this.bytes
        if ( !errorLoading && bytes != null ) return String(bytes).split("\n").toTypedArray()
        return null
    }

    inline fun <reified T: Any> asJsonObject(): T? {
        val toParse = bytes?.toString(kotlin.text.charset("UTF_8"))
        if ( toParse != null ) {
            val mapper = jacksonObjectMapper()
            return mapper.readValue<T>(toParse)
        }
        return null
    }
}