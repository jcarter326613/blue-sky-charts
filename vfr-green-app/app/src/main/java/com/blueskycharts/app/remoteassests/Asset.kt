package com.blueskycharts.app.remoteassests

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.JsonReader
import com.beust.klaxon.Klaxon
import java.io.InputStreamReader
import java.net.URL
import kotlin.reflect.KClass

class Asset( val url: URL, val volatility: Volatility ) {
    val localPath: String
        get() = "${url.host}/${url.path}"
    var bytes: ByteArray? = null
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

    inline fun <reified T: Any> asJsonObject(): T? {
        return Klaxon().parse<T>(bytes.toString())
    }
}