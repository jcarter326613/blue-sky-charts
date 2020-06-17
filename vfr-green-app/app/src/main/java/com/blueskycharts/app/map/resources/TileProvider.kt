package com.blueskycharts.app.map.resources

import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d
import kotlin.math.floor

class TileProvider(private val mapRoot: String ) : CachedProvider(0) {
    fun retrieveTile(mapName: String, mapVersion: String, zoomLevel: Int, location: Point2d, tileDimensions: Point2d,
                     receiver: TileReceiver, data: Any) {
        val key = this.createKey(mapName, zoomLevel, location);
        val tileRequest = this.getExistingRequest(key);

        if ( tileRequest != null && !tileRequest.inError ) {
            val tileRequestScoped = tileRequest as TileRequest;
            if (tileRequest.loaded) {
                tileRequestScoped.setReceiver(receiver, data);
                tileRequestScoped.broadcastData(true);
            } else {
                tileRequestScoped.setReceiver(receiver, data);
                this.findTemporaryTile(mapName, zoomLevel, location, receiver, data, mapVersion, tileDimensions);
            }
        } else {
            val url = "${mapRoot}/${mapName}_SEC_$mapVersion/$zoomLevel/${location.x}_${location.y}.png";
            val newRequest = TileRequest(this, receiver, location, tileDimensions, data, url, zoomLevel);
            this.addRequestToQueue(key, newRequest);
            this.findTemporaryTile(mapName, zoomLevel, location, receiver, data, mapVersion, tileDimensions);
        }
    }

    private fun createKey(mapName: String, zoomLevel: Int, location: Point2d): String {
        return "${mapName}|${zoomLevel}|${location.x}_${location.y}";
    }

    private fun findTemporaryTile(mapName: String, zoomLevel: Int, requestLocation: Point2d, receiver: TileReceiver, data: Any,
        mapVersion: String, tileDimensions: Point2d) {
        var divisor = 1;
        for ( i in zoomLevel - 1 downTo 0 ) {
            // Get the new cell that we want in the new zoom level
            divisor *= 2;
            val zoomLocation: Point2d = Point2d(floor(requestLocation.x / divisor), Math.floor(requestLocation.y / divisor));

            // See if that cell is available and draw it if it is
            val key = this.createKey(mapName, i, zoomLocation);
            val cachedRequest = this.getExistingRequest(key);
            if ( cachedRequest != null && cachedRequest.loaded ) {
                // Get the bounds of that cell in the original zoom cells
                val originalZoomZoomLocation = Point2d(zoomLocation.x * divisor, zoomLocation.y * divisor);
                val subsectionX = (requestLocation.x - originalZoomZoomLocation.x) / divisor;
                val subsectionY = (requestLocation.y - originalZoomZoomLocation.y) / divisor;
                val subsection = Box2d(subsectionX, subsectionY, subsectionX + 1 / divisor, subsectionY + 1 / divisor);

                // Get the image and draw it
                val image = (cachedRequest as TileRequest).getImage();
                if ( image != null ) {
                    receiver.receiveTile(requestLocation, subsection, image, data, true);
                    return;
                }
            } else if ( cachedRequest == null ) {
                val url = "${mapRoot}/${mapName}_SEC_${mapVersion}/${i}/${zoomLocation.x}_${zoomLocation.y}.png";
                val newRequest = TileRequest(this, receiver, zoomLocation, tileDimensions, data, url, i);
                this.addRequestToQueue(key, newRequest);
            }
        }
    }
}