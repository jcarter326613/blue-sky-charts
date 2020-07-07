package com.blueskycharts.app.map.view

import android.graphics.*
import com.blueskycharts.app.coordinates.*
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.resources.TileProvider
import com.blueskycharts.app.map.resources.TileReceiver
import kotlin.math.*

class MapTileView(private val tileProvider: TileProvider, private val map: Map) : SubMapView, TileReceiver {
    // Metadata
    override var fileExtentMercator: BoxWebMercator? = null
        get() {
            val extent = this.fileExtent ?: return null
            if ( field == null ) {
                field = CoordinateConversion.convertBoxGeoToBoxMercator(extent)
            }
            return field
        }
        private set
    var fileExtent2d: Box2d? = null
        get() {
            val extent = this.fileExtent ?: return null
            if ( field == null ) {
                field = CoordinateConversion.convertBoxGeoToBox2d(extent)
            }
            return field
        }
        private set

    private var fileExtent: BoxGeo? = null
    private var tileWidth: Int = 0
    private var tileHeight: Int = 0
    private val tileDimensionPercentage: Point2d = Point2d(1.0, 1.0)
    private var mapName: String = ""
    private var mapVersion: String = "0"
    private var maxZoom: Int = 0

    // Rendering
    private var isDisposed: Boolean = false

    override fun initialize(name: String, model: SubMapModel?): BoxWebMercator? {
        if (model?.imageHeight == null || model.imageWidth == null || model.tileWidth == null ||
            model.version == null || model.fileExtent == null || model.maxZoom == null)
            return null

        this.tileWidth = model.tileWidth
        this.tileHeight = model.tileWidth
        this.mapName = name
        this.mapVersion = model.version
        this.maxZoom = model.maxZoom

        val fileExtent = model.fileExtent.createBoxGeo()
        this.fileExtent = fileExtent
        val fileExtentMercator = this.fileExtentMercator ?: return null

        if (fileExtentMercator.width > fileExtentMercator.height)
            this.tileHeight = round(this.tileWidth * fileExtentMercator.height / fileExtentMercator.width).toInt()
        else
            this.tileWidth = round(this.tileHeight * fileExtentMercator.width / fileExtentMercator.height).toInt()

        return fileExtentMercator
    }

    override fun dispose() {
        this.isDisposed = true;
    }

    override fun resetRequestedInformationAgeRecord() {
    }

    override fun getRequestedInformationAgeSeconds(): Int? {
        return null;
    }

    override fun receiveTile(
            subsection: Box2d,      // The subsection (in percentage) of the tile being provided that should be drawn.
                                    // This may be less than (0,0,1,1) if a substitute lower zoom tile is being provided
            tile: Bitmap?,          // The actual tile image to draw
            data: Any?,             // A renderData object specifying the location to draw the tile.  This must be
                                    // modified by the subsection
            immediate: Boolean,     // True if this function is being called on the main thread
            canvas: Canvas?         // The canvas to draw on
    ) {
        if ( this.isDisposed || tile == null ||
             !immediate || canvas == null ||
             data == null || data !is RenderData ) {
            this.map.requestRedraw()
            return
        }

        val restoreTo = canvas.save()

        val sourceRect = Rect(
            (subsection.upperLeft.x * tile.width).toInt(),
            (subsection.upperLeft.y * tile.height).toInt(),
            (subsection.lowerRight.x * tile.width).toInt(),
            (subsection.lowerRight.y * tile.height).toInt()
        )
        val destinationRect = RectF(
            data.destination.upperLeft.x.toFloat(),
            data.destination.upperLeft.y.toFloat(),
            data.destination.lowerRight.x.toFloat(),
            data.destination.lowerRight.y.toFloat()
        )
        canvas.drawBitmap(tile, sourceRect, destinationRect, null)

        canvas.restoreToCount(restoreTo);
    }

    /**
     *
     * @param context
     * @param region The region of the map to draw
     * @param destination The actual pixels to draw on (this must be in screen pixel coordinates so zoom level can be determined)
     */
    override fun render(canvas: Canvas, region: BoxWebMercator, destination: Box2d) {
        if (this.isDisposed) {
            return;
        }
        val fileExtentMercator = this.fileExtentMercator ?: return

        // Figure out the Box2d for the full map
        val regionPercentage = fileExtentMercator.overlapPercentageUpperLeft(region)
        val fullMapDestinationWidth = (destination.upperLeft.x - destination.lowerRight.x) / (regionPercentage.upperLeft.x - regionPercentage.lowerRight.x)
        val fullMapDestinationX = destination.upperLeft.x - fullMapDestinationWidth * regionPercentage.upperLeft.x
        val fullMapDestinationHeight = (destination.upperLeft.y - destination.lowerRight.y) / (regionPercentage.upperLeft.y - regionPercentage.lowerRight.y)
        val fullMapDestinationY = destination.upperLeft.y - fullMapDestinationHeight * regionPercentage.upperLeft.y

        // Figure out the zoom level for what we are drawing
        val zoom0TilePixelsPerMercator = this.tileWidth / fileExtentMercator.width
        val desiredPixelsPerMercator = destination.width / region.width
        var zoomLevel = ceil(log2(desiredPixelsPerMercator / zoom0TilePixelsPerMercator)).toInt()
        if (zoomLevel < 0)
            zoomLevel = 0
        if (zoomLevel > this.maxZoom)
            zoomLevel = this.maxZoom

        // Figure out what tiles we need to draw
        val numTilesAcross = 2.0.pow(zoomLevel).toInt()
        var startX = floor(regionPercentage.upperLeft.x * numTilesAcross).toInt()
        var startY = floor(regionPercentage.upperLeft.y * numTilesAcross).toInt()
        var endX = ceil(regionPercentage.lowerRight.x * numTilesAcross).toInt()
        var endY = ceil(regionPercentage.lowerRight.y * numTilesAcross).toInt()

        if ( startX < 0 ) {
            startX = 0
        }
        if ( endX > numTilesAcross ) {
            endX = numTilesAcross
        }
        if ( startY < 0 ) {
            startY = 0
        }
        if ( endY > numTilesAcross ) {
            endY = numTilesAcross
        }

        if (endX - startX > 40 || endY - startY > 40) {
            return
        }

        // Get all the images to draw
        for (x in startX until endX) {
            for (y in startY until endY ) {
                // Figure out where to draw this tile
                val tileDestination = Box2d(
                    fullMapDestinationX + fullMapDestinationWidth * (x / numTilesAcross.toDouble()),
                    fullMapDestinationY + fullMapDestinationHeight * (y / numTilesAcross.toDouble()),
                    fullMapDestinationX + fullMapDestinationWidth * ((x+1) / numTilesAcross.toDouble()),
                    fullMapDestinationY + fullMapDestinationHeight * ((y+1) / numTilesAcross.toDouble()))

                // Get the tile image and request it be drawn
                this.tileProvider.retrieveTile(
                    mapName = this.mapName,
                    mapVersion = this.mapVersion,
                    zoomLevel = zoomLevel,
                    location = Point2d(x.toDouble(), y.toDouble()),     // The tile "id" to draw
                    tileDimensions = this.tileDimensionPercentage,      // Always 1,1.  This is the percentage of the tile to draw to the screen.
                                                                        // Modified by fileTile in the tile provider if the tile is not available but a lower zoom is.
                    receiver = this,
                    canvas = canvas,
                    data = RenderData(tileDestination)    // Describes the place to draw the full tile, not a portion of the tile
                )
            }
        }
    }

    override fun moveOffscreen() {
    }

    private data class RenderData(val destination: Box2d)
}