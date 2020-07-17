package com.blueskycharts.app.map.resources

import android.graphics.Canvas
import com.blueskycharts.app.coordinates.Point2d

interface TileProviderInterface {
    fun retrieveTile(mapName: String, mapVersion: String, zoomLevel: Int, location: Point2d, tileDimensions: Point2d,
                     receiver: TileReceiver, data: Any?, canvas: Canvas
    )
}