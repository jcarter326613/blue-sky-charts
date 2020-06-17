package com.blueskycharts.app.map.view

import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.BoxWebMercator
import com.blueskycharts.app.map.models.SubMapModel

interface SubMapView {
    /**
     * Initializes the view
     * @param model
     * @param name
     * @returns A BoxGeo representing the full extent of the map.  undefined if this map should be discarded from view because of a
     *  bad configuration.
     */
    fun initialize(model: SubMapModel, name: String): BoxWebMercator?;

    /**
     * Some original width and height in any unit which is used determine the region parameter into the render function.
     */
    fun getOriginalWidth(): Int;
    fun getOriginalHeight(): Int;

    /**
     * Returns the age of any data beign displayed and resets the age counter prior to rendering
     */
    fun resetRequestedInformationAgeRecord();
    fun getRequestedInformationAgeSeconds(): Int?;

    /**
     * Draws or queues the drawing of this sub map.  The number of pixels to be drawn are the region width * height * scale
     * @param context
     * @param region The region in relation to the original width and height
     * @param scale All aspects of the region are multiplied by this value
     */
    fun render(context: CanvasRenderingContext2D, region: Box2d, scale: Float);

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