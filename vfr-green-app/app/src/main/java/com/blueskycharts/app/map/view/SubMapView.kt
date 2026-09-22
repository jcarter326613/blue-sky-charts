package com.blueskycharts.app.map.view

import android.graphics.Canvas
import com.blueskycharts.app.coordinates.RectangularArea
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.map.models.SubMapModel

interface SubMapView {
    /**
     * Projected coordinates of map
     */
    val fileExtent: RectangularArea?

    /**
     * Returns the age of any data beign displayed and resets the age counter prior to rendering
     */
    fun resetRequestedInformationAgeRecord()
    fun getRequestedInformationAgeSeconds(): LongRange?

    /**
     * Draws or queues the drawing of this sub map.  The number of pixels to be drawn are the region width * height * scale
     * @param context
     * @param region The region in relation to the original width and height
     * @param scale All aspects of the region are multiplied by this value
     */
    fun render(canvas: Canvas, region: Box2d, destination: Box2d)

    /**
     * Prevents any further drawing from this view
     */
    fun dispose();

    /**
     * Prevents drawing to the screen until a new render call is issued.  Used for preventing delay loaded assets from
     * drawing with an old context transformation if the map has moved off screen and is not receiving updated render calls.
     */
    fun moveOffscreen();
}