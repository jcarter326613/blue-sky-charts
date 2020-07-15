package com.blueskycharts.app.map.resources

import android.graphics.Bitmap
import android.graphics.Canvas
import com.blueskycharts.app.coordinates.Box2d

interface TileReceiver {
    fun receiveTile(subsection: Box2d, tile: Bitmap?, data: Any?, immediate: Boolean, canvas: Canvas?)
}