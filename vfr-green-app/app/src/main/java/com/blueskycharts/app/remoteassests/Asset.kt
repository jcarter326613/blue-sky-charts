package com.blueskycharts.app.remoteassests

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.JsonReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStreamReader
import java.net.URL

class Asset( val url: URL, val volatility: Volatility ) {
    val localPath: String
        get() = "${url.host}/${url.path}/${url.query}"
    var bytes: ByteArray? = null
    var errorLoading: Boolean = false
    var requiresCors: Boolean = false

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

    inline fun <reified T: Any> asJsonObject(): T? {
        val toParse = bytes?.toString(kotlin.text.charset("UTF_8"))
        if ( toParse != null ) {
            val mapper = jacksonObjectMapper()
            return mapper.readValue<T>(toParse)
        }
        return null
    }
}