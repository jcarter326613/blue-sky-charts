package com.blueskycharts.app.map.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import com.blueskycharts.app.R
import com.blueskycharts.app.coordinates.Box2d
import com.blueskycharts.app.coordinates.BoxWebMercator
import com.blueskycharts.app.coordinates.CoordinateConversion
import com.blueskycharts.app.coordinates.Point2d
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.resources.TileProvider
import com.blueskycharts.app.map.resources.TileReceiver
import kotlin.math.*
import androidx.core.graphics.drawable.toDrawable as toDrawable1

class MapTileView(private val tileProvider: TileProvider, private val map: Map) : SubMapView, TileReceiver {
    // Metadata
    override var originalWidth: Double = 0.0
        private set
    override var originalHeight: Double = 0.0
        private set
    private var imageWidthScale: Double = 1.0;
    private var imageHeightScale: Double = 1.0;
    private var tileWidth: Int = 0;
    private var tileHeight: Int = 0;
    private val tileDimensionPercentage: Point2d = Point2d(1.0, 1.0);
    private var mapName: String = "";
    private var mapVersion: String = "0";
    private var maxZoom: Int = 0;

    // Rendering
    private var scaledTileWidth: Double = 0.0;
    private var isDisposed: Boolean = false;

    override fun initialize(model: SubMapModel?): BoxWebMercator? {
        if (model == null ||
            model.imageHeight == null || model.imageWidth == null || model.tileWidth == null ||
            model.version == null || model.fileExtent == null || model.maxZoom == null ||
            model.name == null)
            return null

        if ( model.imageWidthScale != null ) {
            this.imageWidthScale = model.imageWidthScale;
        }
        if ( model.imageHeightScale != null ) {
            this.imageHeightScale = model.imageHeightScale;
        }

        this.originalWidth = model.imageWidth;
        this.originalHeight = model.imageHeight;
        this.tileWidth = model.tileWidth;
        this.tileHeight = model.tileWidth;
        this.mapName = model.name;
        this.mapVersion = model.version;
        this.maxZoom = model.maxZoom;

        if (this.originalWidth > this.originalHeight)
            this.tileHeight = round(this.tileWidth * this.originalHeight / this.originalWidth.toDouble()).toInt()
        else
            this.tileWidth = round(this.tileHeight * this.originalWidth / this.originalHeight.toDouble()).toInt()

        val fileExtent = model.fileExtent.createBoxGeo()
        return CoordinateConversion.convertBoxGeoToBoxMercator(fileExtent)
    }

    override fun dispose() {
        this.isDisposed = true;
    }

    override fun resetRequestedInformationAgeRecord() {
    }

    override fun getRequestedInformationAgeSeconds(): Int? {
        return null;
    }

    override fun receiveTile(location: Point2d, subsection: Box2d, tile: Bitmap?, data: Any?, immediate: Boolean, canvas: Canvas?) {
        if ( this.isDisposed || tile == null ) {
            return
        }

        if ( !immediate || canvas == null ) {
            this.map.requestRedraw()
            return
        }

        val tileX = location.x * this.scaledTileWidth * this.imageWidthScale;
        val tileWidth = this.scaledTileWidth * this.imageWidthScale;
        val scaledTileHeight = this.scaledTileWidth * this.tileHeight / this.tileWidth
        val tileY = location.y * scaledTileHeight * this.imageHeightScale;
        val tileHeight = scaledTileHeight * this.imageHeightScale;

        val restoreTo = canvas.save();
        val upperLeft = subsection.upperLeft;
        val dimensions = subsection.getDimensions();

        val sourceRect = Rect(
            (upperLeft.x * tile.width).toInt(),
            (upperLeft.y * tile.height).toInt(),
            ((upperLeft.x + dimensions.x) * tile.width).toInt(),
            ((upperLeft.y + dimensions.y) * tile.height).toInt()
        )
        val destinationRect = RectF(
            tileX.toFloat(),
            tileY.toFloat(),
            (tileX + tileWidth).toFloat(),
            (tileY + tileHeight).toFloat()
        )
        canvas.drawBitmap(tile, sourceRect, destinationRect, null)

        canvas.restoreToCount(restoreTo);
    }

    /**
     *
     * @param context
     * @param region The region of the map to draw relative to the original size of the map image
     * @param scale The scale to draw the map at.  Point (0,0) is the center of the map.
     */
    override fun render(canvas: Canvas, region: Box2d, scale: Double) {
        if (this.isDisposed) {
            return;
        }

        // Validate the region
        val reg = region.clone();
        if ( reg.upperLeft.x < 0 )
            reg.upperLeft.x = 0.0;
        else if ( reg.upperLeft.x >= this.originalWidth )
            reg.upperLeft.x = this.originalWidth.toDouble();
        if ( reg.upperLeft.y < 0 )
            reg.upperLeft.y = 0.0;
        else if ( reg.upperLeft.y >= this.originalHeight )
            reg.upperLeft.y = this.originalHeight.toDouble();

        if ( reg.lowerRight.x < 0 )
            reg.lowerRight.x = 0.0;
        else if ( reg.lowerRight.x >= this.originalWidth )
            reg.lowerRight.x = this.originalWidth.toDouble();
        if ( reg.lowerRight.y < 0 )
            reg.lowerRight.y = 0.0;
        else if ( reg.lowerRight.y >= this.originalHeight )
            reg.lowerRight.y = this.originalHeight.toDouble();

        // Figure out the size of what we are drawing
        val m = this.originalWidth * scale
        var zoomLevel = ceil(log2(m / this.tileWidth)).toInt()
        if (zoomLevel < 0)
            zoomLevel = 0;
        if (zoomLevel > this.maxZoom)
            zoomLevel = this.maxZoom;
        val numTilesAcross = 2.0.pow(zoomLevel)
        this.scaledTileWidth = m / numTilesAcross;
        val originalImageTileWidth = this.originalWidth / numTilesAcross
        val originalImageTileHeight = this.originalHeight / numTilesAcross

        // Ensure we aren't looping too much
        val startX = floor(reg.upperLeft.x / originalImageTileWidth).toInt()
        val endX = ceil(reg.lowerRight.x / originalImageTileWidth).toInt()
        val startY = floor(reg.upperLeft.y / originalImageTileHeight).toInt()
        val endY = ceil(reg.lowerRight.y / originalImageTileHeight).toInt()

        if (endX - startX > 40 || endY - startY > 40) {
            return;
        }

        // Get all the images to draw
        for (x in startX until endX) {
            for (y in startY until endY ) {
                // Get the tile image and request it be drawn
                this.tileProvider.retrieveTile(
                    mapName = this.mapName,
                    mapVersion = this.mapVersion,
                    zoomLevel = zoomLevel,
                    location = Point2d(x.toDouble(), y.toDouble()),
                    tileDimensions = this.tileDimensionPercentage,
                    receiver = this,
                    canvas = canvas,
                    data = null)
            }
        }
    }

    override fun moveOffscreen() {
        //this.context = null;
    }
}