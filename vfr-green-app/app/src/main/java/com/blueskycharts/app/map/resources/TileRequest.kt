package com.blueskycharts.app.map.resources

import android.graphics.Bitmap
import android.graphics.Canvas
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d

class TileRequest(private val provider: TileProvider, private var receiver: TileReceiver?, private var data: Any?,
                  private val location: Point2d, private val dimensions: Point2d,
                  private val zoomLevel: Int, private val mapName: String, private val mapVersion: String ) : CachedProviderRequest(provider, zoomLevel) {
    var image: Bitmap? = null
        private set

    override fun sendRequest() {
        val provider = provider.assetProvider
        provider.retrieveTile(mapName, mapVersion, zoomLevel, location.x.toInt(), location.y.toInt() ) {
            try {
                val bitmap = it.asBitmap()
                if ( bitmap != null ) {
                    this@TileRequest.image = bitmap
                    val success = bitmap.width > 1;
                    this@TileRequest.completeRequest(success);
                } else {
                    this@TileRequest.completeRequest(false)
                }
            } catch (e: Throwable) {
                this@TileRequest.completeRequest(false)
                throw e
            }
        }
    }

    override fun broadcastData(immediate: Boolean, canvas: Canvas?) {
        if (this.loaded && this.receiver != null && this.image != null) {
            val region = Box2d(0.0, 0.0, this.dimensions.x, this.dimensions.y);
            this.receiver?.receiveTile(region, this.image, this.data, immediate, canvas);
            this.receiver = null;
            this.data = null;
        }
    }

    fun setReceiver(receiver: TileReceiver, data: Any?) {
        this.receiver = receiver;
        this.data = data;
    }
}