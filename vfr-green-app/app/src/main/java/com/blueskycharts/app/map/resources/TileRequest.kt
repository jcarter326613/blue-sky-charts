package com.blueskycharts.app.map.resources

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d
import java.io.InputStream
import java.net.URL

class TileRequest(private val provider: TileProvider, private var receiver: TileReceiver?, private val location: Point2d?, private val dimensions: Point2d?,
                  private var data: Any?, private val url: String, private val zoomLevel: Int  ) : CachedProviderRequest(provider, zoomLevel) {
    private var tileImage: Drawable? = null

    override fun sendRequest() {
        try {
            val url = URL(url);
            val bitmap = BitmapDrawable(url.openStream())
            this.tileImage = bitmap
            val success = bitmap.intrinsicWidth > 1;
            this.completeRequest(success);
        } catch (e: Throwable) {
            this.completeRequest(false);
        }
    }

    override fun broadcastData(immediate: Boolean) {
        if (this.loaded && !this.inError &&
            this.receiver != null && this.location != null && this.tileImage != null && this.dimensions != null) {

            val region = Box2d(0.0, 0.0, this.dimensions.x, this.dimensions.y);
            this.receiver?.receiveTile(this.location, region, this.tileImage, this.data, immediate);
            this.receiver = null;
            this.data = null;
        }
    }

    fun setReceiver(receiver: TileReceiver, data: Any?) {
        this.receiver = receiver;
        this.data = data;
    }

    fun getImage(): Drawable? {
        return this.tileImage;
    }
}