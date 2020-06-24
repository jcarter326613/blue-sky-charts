package com.blueskycharts.app.map.view

import android.content.Context
import android.graphics.Canvas
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.BoxWebMercator
import com.blueskycharts.app.map.models.SubMapModel

abstract class SubMapView(private val context: Context) {
    /**
     * Some original width and height in any unit which is used determine the region parameter into the render function.
     */
    abstract val originalWidth: Int
    abstract val originalHeight: Int

    /**
     * Initializes the view
     * @param model
     * @param name
     * @returns A BoxGeo representing the full extent of the map.  undefined if this map should be discarded from view because of a
     *  bad configuration.
     */
    abstract fun initialize(model: SubMapModel?): BoxWebMercator?;

    /**
     * Returns the age of any data beign displayed and resets the age counter prior to rendering
     */
    abstract fun resetRequestedInformationAgeRecord();
    abstract fun getRequestedInformationAgeSeconds(): Int?;

    /**
     * Draws or queues the drawing of this sub map.  The number of pixels to be drawn are the region width * height * scale
     * @param context
     * @param region The region in relation to the original width and height
     * @param scale All aspects of the region are multiplied by this value
     */
    abstract fun render(canvas: Canvas, region: Box2d, scale: Double);

    /**
     * Prevents any further drawing from this view
     */
    abstract fun dispose();

    /**
     * Prevents drawing to the screen until a new render call is issued.  Used for preventing delay loaded assets from
     * drawing with an old context transformation if the map has moved off screen and is not receiving updated render calls.
     */
    abstract fun moveOffscreen();

    /**
     * Converts a dp measure ot pixels
     */
    protected fun convertDipToPixels(dp: Float): Float {
        return dp * context.resources.displayMetrics.density + 0.5f
    }
}