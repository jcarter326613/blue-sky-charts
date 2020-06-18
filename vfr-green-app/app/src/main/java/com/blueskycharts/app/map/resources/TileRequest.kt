package com.blueskycharts.app.map.resources

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.URL

class TileRequest(private val provider: TileProvider, private var receiver: TileReceiver?, private val location: Point2d?, private val dimensions: Point2d?,
                  private var data: Any?, private val url: String, private val zoomLevel: Int  ) : CachedProviderRequest(provider, zoomLevel) {
    var image: Bitmap? = null
        private set

    override fun sendRequest() {
        GlobalScope.launch {
            try {
                val url = URL(url);
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                this@TileRequest.image = bitmap
                val success = bitmap.width > 1;
                this@TileRequest.completeRequest(success);
            } catch (e: Throwable) {
                this@TileRequest.completeRequest(false);
            }
        }
    }

    override fun broadcastData(immediate: Boolean, canvas: Canvas?) {
        if (this.loaded && !this.inError &&
            this.receiver != null && this.location != null && this.image != null && this.dimensions != null) {

            val region = Box2d(0.0, 0.0, this.dimensions.x, this.dimensions.y);
            this.receiver?.receiveTile(this.location, region, this.image, this.data, immediate, canvas);
            this.receiver = null;
            this.data = null;
        }
    }

    fun setReceiver(receiver: TileReceiver, data: Any?) {
        this.receiver = receiver;
        this.data = data;
    }
}