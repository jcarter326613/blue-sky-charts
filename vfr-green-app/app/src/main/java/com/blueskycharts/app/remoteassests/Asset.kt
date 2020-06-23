package com.blueskycharts.app.remoteassests

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.JsonReader
import com.blueskycharts.app.map.models.SubMapModel
import java.io.InputStreamReader
import java.net.URL

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
}