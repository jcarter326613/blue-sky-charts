package com.blueskycharts.app.map.resources

import android.content.Context
import android.graphics.Canvas
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.floor

open class TileProvider(private val mapRoot: String, private val imageExtension: String, val context: Context) : CachedProvider(0) {
    open fun retrieveTile(mapName: String, mapVersion: String, zoomLevel: Int, location: Point2d, tileDimensions: Point2d,
                     receiver: TileReceiver, data: Any?, canvas: Canvas ) {
        val key = this.createKey(mapName, zoomLevel, location);
        val tileRequest = this.getCachedItem(key);
        if ( tileRequest != null && tileRequest.loaded ) {
            val tileRequestScoped = tileRequest as TileRequest;
            if (tileRequestScoped.loaded) {
                tileRequestScoped.setReceiver(receiver, data);
                tileRequestScoped.broadcastData(true, canvas);
            } else {
                this.findTemporaryTile(mapName, zoomLevel, location, receiver, data, mapVersion, tileDimensions, canvas)
            }
        } else {
            this.findTemporaryTile(mapName, zoomLevel, location, receiver, data, mapVersion, tileDimensions, canvas)
            incrementAwaitingQueueAddition()
            GlobalScope.launch {
                try {
                    val newTileRequest = this@TileProvider.getExistingRequest(key);
                    if (newTileRequest != null && !newTileRequest.inError) {
                        val tileRequestScoped = newTileRequest as TileRequest;
                        tileRequestScoped.setReceiver(receiver, data);
                        if (tileRequestScoped.loaded) {
                            tileRequestScoped.broadcastData(false, canvas);
                        }
                    } else {
                        val url = this@TileProvider.createUrl(mapName, mapVersion, zoomLevel, location)
                        val newRequest = TileRequest(
                            this@TileProvider,
                            receiver,
                            location,
                            tileDimensions,
                            data,
                            url,
                            zoomLevel
                        );
                        this@TileProvider.addRequestToQueue(key, newRequest);
                    }
                } finally {
                    decrementAwaitingQueueAddition()
                }
            }
        }
    }

    private fun createUrl(mapName: String, mapVersion: String, zoomLevel: Int, location: Point2d): String {
        return "${mapRoot}/${mapName}/$mapVersion/$zoomLevel/${location.x.toInt()}_${location.y.toInt()}.${imageExtension}"
    }

    private fun createKey(mapName: String, zoomLevel: Int, location: Point2d): String {
        return "${mapName}|${zoomLevel}|${location.x.toInt()}_${location.y.toInt()}";
    }

    private fun findTemporaryTile(mapName: String, zoomLevel: Int, requestLocation: Point2d, receiver: TileReceiver, data: Any?,
        mapVersion: String, tileDimensions: Point2d, canvas: Canvas) {
        var divisor = 1;
        for ( i in zoomLevel - 1 downTo 0 ) {
            // Get the new cell that we want in the new zoom level
            divisor *= 2;
            val zoomLocation: Point2d = Point2d(floor(requestLocation.x / divisor), floor(requestLocation.y / divisor));

            // See if that cell is available and draw it if it is
            val key = this.createKey(mapName, i, zoomLocation);
            val cachedRequest = this.getCachedItem(key);
            if ( cachedRequest != null && cachedRequest.loaded ) {
                // Get the bounds of that cell in the original zoom cells
                val originalZoomZoomLocation = Point2d(zoomLocation.x * divisor, zoomLocation.y * divisor);
                val subsectionX = (requestLocation.x - originalZoomZoomLocation.x) / divisor;
                val subsectionY = (requestLocation.y - originalZoomZoomLocation.y) / divisor;
                val subsection = Box2d(subsectionX, subsectionY, subsectionX + 1 / divisor.toDouble(), subsectionY + 1 / divisor.toDouble());

                // Get the image and draw it
                val image = (cachedRequest as TileRequest).image;
                if ( image != null ) {
                    receiver.receiveTile(subsection, image, data, true, canvas);
                    return;
                }
            } else if ( cachedRequest == null ) {
                GlobalScope.launch {
                    val url = this@TileProvider.createUrl(mapName, mapVersion, i, zoomLocation)
                    val newRequest = TileRequest(this@TileProvider, receiver, zoomLocation, tileDimensions, data, url, i);
                    this@TileProvider.addRequestToQueue(key, newRequest);
                }
            }
        }
    }
}