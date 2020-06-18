package com.blueskycharts.app.map.resources

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.Point2d

interface TileReceiver {
    fun receiveTile(location: Point2d, subsection: Box2d, tile: Bitmap?, data: Any?, immediate: Boolean, canvas: Canvas?)
}