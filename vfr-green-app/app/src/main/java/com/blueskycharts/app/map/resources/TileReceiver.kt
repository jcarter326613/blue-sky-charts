package com.blueskycharts.app.map.resources

import android.graphics.drawable.Drawable
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d

interface TileReceiver {
    fun receiveTile(location: Point2d, subsection: Box2d, tile: Drawable?, data: Any?, immediate: Boolean)
}