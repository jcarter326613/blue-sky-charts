package com.blueskycharts.app.map.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.getStringOrThrow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.blueskycharts.app.R
import com.blueskycharts.app.coordinates.*
import com.blueskycharts.app.map.models.BoxGeoModel
import com.blueskycharts.app.map.models.OverlayViewModel
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.resources.DataProvider
import com.blueskycharts.app.map.resources.TileProvider
import com.blueskycharts.app.remoteassests.AssetProvider
import com.blueskycharts.app.remoteassests.Volatility
import java.net.URL
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

class NavigableMap2d(context: Context, attributes: AttributeSet) : View(context, attributes), Map {
    private val tileProvider: TileProvider
    private val shadowTileProvider: TileProvider
    private val dataProvider: DataProvider
    private val redrawTimer = Timer(false)

    // Map state variables
    private var mapBackground: SubMapPosition? = null
    private var mapViews: LinkedList<SubMapPosition>? = null
    private var dataOverlayView: SubMapPosition? = null
    private var origin2d: PointWebMercator
    private var scale: Double
    private var scaleDriver: Float
    private val maxScaleDriver: Float = 12F

    // Mouse event variables
    private var isDragging: Boolean
    private var mouseDownClient: Point2d
    private var mouseDownOrigin2d: Point2d
    private var pinchClientPoint1: Point2d
    private var pinchClientPoint2: Point2d
    private var pinchOriginalScale: Double

    init {
        val mapRoot: String;
        context.theme.obtainStyledAttributes(attributes, R.styleable.NavigableMap2d, 0, 0).apply {
            try {
                mapRoot = getStringOrThrow(R.styleable.NavigableMap2d_mapRoot)
                scaleDriver = getFloat(R.styleable.NavigableMap2d_zoom, 4.25F);
                val originLongitude = getFloat(R.styleable.NavigableMap2d_originLongitude, -98.5795F)
                val originLatitude = getFloat(R.styleable.NavigableMap2d_originLongitude, 39.8283F)
                origin2d = CoordinateConversion.convertToWebMercator(PointGeo(originLongitude.toDouble(), originLatitude.toDouble()));
            } finally {
                recycle()
            }
        }

        // Set all constant and derived defaults
        this.tileProvider = TileProvider("${mapRoot}/sectional", context);
        this.shadowTileProvider = TileProvider("${mapRoot}/world-shadow", context);
        this.dataProvider = DataProvider(context)

        if ( this.scaleDriver > this.maxScaleDriver ) {
            this.scaleDriver = this.maxScaleDriver;
        } else if (this.scaleDriver < 0) {
            this.scaleDriver = 0.0F;
        }
        this.scale = 0.0;
        this.updateScale();

        this.mouseDownClient = Point2d();
        this.mouseDownOrigin2d = Point2d();

        this.isDragging = false;
        this.pinchClientPoint1 = Point2d();
        this.pinchClientPoint2 = Point2d();
        this.pinchOriginalScale = 0.0;

        // Startup the map
        this.setupBackgroundShadow()
        this.retrieveConfiguration(URL("${mapRoot}/metadata.json"))
    }

    override fun requestRedraw() {
        this.postInvalidate()
    }

    fun setOverlayType(type: OverlayTypes): Boolean {
        var success: Boolean
        if (type != OverlayTypes.None) {
            val dataView = MapDataView(this.dataProvider, type, this, context)
            val extent = dataView.initialize(null)
            if ( extent != null ) {
                this.dataOverlayView = SubMapPosition(dataView, extent)
                success = true;
            } else {
                success = false;
            }
        } else {
            val dataOverlayView = this.dataOverlayView
            if ( dataOverlayView != null ) {
                dataOverlayView.subMapView.dispose()
                this.dataOverlayView = null;
            }
            success = true
        }

        this.dataProvider.clearQueue()
        this.requestRedraw()
        return success
    }

    private fun viewportChanged() {
        this.tileProvider.clearQueue()
        this.dataProvider.clearQueue()
    }

    private fun updateScale() {
        this.scale = 1 / (2.0.pow(this.scaleDriver.toDouble()));
    }

    private fun setupBackgroundShadow() {
        val worldShadowView = MapTileView(this.shadowTileProvider, this, context);
        val worldShadowMercatorExtents = BoxWebMercator(0.0, 0.0, PointWebMercator.MAX_X_MERCATOR.toDouble(), PointWebMercator.MAX_Y_MERCATOR.toDouble());
        val shadowBoxGeo = CoordinateConversion.convertBoxMercatorToBoxGeo(worldShadowMercatorExtents);
        val shadowBoxGeoModel = BoxGeoModel(
            topLeft = shadowBoxGeo.topLeft,
            topRight = shadowBoxGeo.topRight,
            bottomLeft = shadowBoxGeo.bottomLeft,
            bottomRight = shadowBoxGeo.bottomRight
        )
        val shadowMapModel = SubMapModel(
            mapBounds = null,
            tileWidth = 256,
            maxZoom = 0,
            fileExtent = shadowBoxGeoModel,
            imageHeight = 256,
            imageWidth = 256,
            imageHeightScale = 1.0,
            imageWidthScale = 1.0,
            version = "1",
            name = "world-shadow"
        )
        worldShadowView.initialize(shadowMapModel)
        this.mapBackground = SubMapPosition(worldShadowView, worldShadowMercatorExtents)
    }

    private fun initializeMapModel(data: Collection<SubMapModel>) {
        // Add the world  VFR charts
        val mapViewList = LinkedList<SubMapPosition>()
        for (subMapModel in data) {
            val subMapView = MapTileView(this.tileProvider, this, context);
            val fileExtent = subMapView.initialize(subMapModel) ?: continue;
            mapViewList.add(SubMapPosition(subMapView, fileExtent));
        }
        this.mapViews = mapViewList
    }

    private fun retrieveConfiguration(mapConfigurationFile: URL) {
        val assetProvider = AssetProvider(context)
        assetProvider.retrieveAsset(mapConfigurationFile, Volatility.DayCache) {
            val reader = it.asJsonReader()
            if ( reader != null ) {
                val mapPositions = SubMapModel.readFromJsonReader(reader);
                this@NavigableMap2d.initializeMapModel(mapPositions)
                this@NavigableMap2d.postInvalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return;

        val viewport2d = this.calculateViewport();

        // Draw the solid background color
        val paint = Paint()
        paint.color = Color.parseColor("#323232")
        canvas.drawRect(Rect(0, 0, this.width, this.height), paint)

        // Draw the shadow
        if ( this.mapBackground != null ) {
            val restoreCount = canvas.save()
            this.drawSubMap(this.mapBackground!!, viewport2d, canvas)
            canvas.restoreToCount(restoreCount)
        }

        // Draw each sub map
        if ( this.mapViews != null ) {
            val mapViews = this.mapViews!!
            for (submap in mapViews) {
                val restoreCount = canvas.save()
                this.drawSubMap(submap, viewport2d, canvas)
                canvas.restoreToCount(restoreCount)
            }
        }

        val dataOverlayView = this.dataOverlayView
        if ( dataOverlayView != null ) {
            val dataOverlay = dataOverlayView.subMapView

            dataOverlay.resetRequestedInformationAgeRecord()
            canvas.save()
            this.drawSubMap(dataOverlayView, viewport2d, canvas)
            canvas.restore()

            /*
            // Draw the date indicating the oldest data displayed
            if ( this.dataProvider.isLoading() ) {
                canvas.save();
                this.renderInformationAgeBox("Loading weather data...")
                canvas.restore();
            } else {
                val informationAge = dataOverlay.getRequestedInformationAgeSeconds()
                if ( informationAge != null ) {
                    // Draw the information age
                    canvas.save()
                    val informationAgeLabel = this.getInformationAgeLabel(informationAge)
                    this.renderInformationAgeBox(informationAgeLabel)
                    canvas.restore()

                    // Trigger a refresh for when the information age needs to be updated
                    val secondsToSleep =
                        when (informationAge) {
                            60 -> 1
                            0 -> 61
                            else -> (60 - (informationAge % 60)) + 1
                        }

                    val task: TimerTask = timerTask {
                        this@NavigableMap2d.requestRedraw()
                    }
                    redrawTimer.schedule(task, secondsToSleep * 1000L)
                }
            }

             */
        }
    }

    private fun drawSubMap(submap: SubMapPosition, viewport2d: BoxWebMercator, canvas: Canvas) {
        // Calculate the viewport from the perspective of the un modified sub map
        val mapPosition2d: BoxWebMercator = submap.location;
        val viewportOverlap2d = mapPosition2d.union(viewport2d);
        if (viewportOverlap2d != null) {
            val originalWidth = submap.subMapView.originalWidth;
            val originalHeight = submap.subMapView.originalHeight;

            val mapMercatorWidth = mapPosition2d.bottomRight.x - mapPosition2d.topLeft.x;
            val mapMercatorHeight = mapPosition2d.bottomRight.y - mapPosition2d.topLeft.y;
            val mapScale = (this.width * mapMercatorWidth) / (originalWidth * viewport2d.getWidth());

            val mapShiftX = (mapPosition2d.topLeft.x - this.origin2d.x) * this.width / viewport2d.getWidth()
            val mapShiftY = (mapPosition2d.topLeft.y - this.origin2d.y) * this.height / viewport2d.getHeight()

            canvas.translate(this.width / 2.0F, this.height / 2.0F);
            canvas.translate(mapShiftX.toFloat(), mapShiftY.toFloat());

            // Figure out the part of the map we want to draw in 2d coordinates relative to the upper left corner
            val subMapDrawSection = Box2d(
                originalWidth * (viewportOverlap2d.topLeft.x - mapPosition2d.topLeft.x) / mapMercatorWidth,
                originalHeight * (viewportOverlap2d.topLeft.y - mapPosition2d.topLeft.y) / mapMercatorHeight,
                originalWidth * (viewportOverlap2d.bottomRight.x - mapPosition2d.topLeft.x) / mapMercatorWidth,
                originalHeight * (viewportOverlap2d.bottomRight.y - mapPosition2d.topLeft.y) / mapMercatorHeight);

            // Draw the submap
            submap.subMapView.render(canvas, subMapDrawSection, mapScale);
        } else {
            submap.subMapView.moveOffscreen();
        }
    }

    private fun renderInformationAgeBox(informationAgeLabel: String) {
        /*
        if (this.context == null)
            return;

        // Figure out where we need to draw
        let oldFont = this.context.font;
        this.context.font = "20px Arial";
        let lineHeight = this.context.measureText('M').width * 1.2;
        let textDimensions = this.context.measureText(informationAgeLabel);
        let heightBuffer = 10;
        let widthBuffer = 6;
        let textRect = new Box2d(-textDimensions.width / 2, -lineHeight / 2, textDimensions.width / 2, lineHeight / 2);
        let boxRect = new Box2d(-textDimensions.width / 2 - widthBuffer / 2, -lineHeight / 2 - heightBuffer / 2,
            textDimensions.width / 2 + widthBuffer / 2, lineHeight / 2 + heightBuffer / 10)

        // Reposition the axis
        let offsetX = 5;
        let offsetY = 5;
        this.context.translate(-boxRect.getUpperLeft().x + offsetX, -boxRect.getUpperLeft().y + offsetY);

        // Draw the box
        this.context.lineWidth = 1;
        this.context.strokeStyle = "rgb(0,0,0)";
        this.context.fillStyle = "rgb(255,255,255)";
        this.context.beginPath();
        this.context.rect(boxRect.getUpperLeft().x, boxRect.getUpperLeft().y, boxRect.getDimensions().x, boxRect.getDimensions().y);
        this.context.fill();
        this.context.stroke();

        // Draw the text
        this.context.fillStyle = "rgb(0,0,0)";
        let oldAlign = this.context.textAlign;
        this.context.textAlign = "center";
        this.context.fillText(informationAgeLabel, 0, textRect.getLowerRight().y - 5);
        this.context.font = oldFont;
        this.context.textAlign = oldAlign;

         */
    }

    private fun getInformationAgeLabel(ageSeconds: Int): String {
        if ( ageSeconds < 60 ) {
            return "Age 1 minute";
        }
        val displaySeconds = ceil(ageSeconds / 60.0).toInt()
        return "Age $displaySeconds minutes"
    }

    /**
     * Returns the viewport in unscaled coordinates.
     */
    private fun calculateViewport(): BoxWebMercator {
        val viewportDimensions2d = this.getViewportDimensions2d();
        val widthBy2 = viewportDimensions2d.x / 2;
        val heightBy2 = viewportDimensions2d.y / 2;
        val viewport = BoxWebMercator(this.origin2d.x - widthBy2, this.origin2d.y - heightBy2,
            this.origin2d.x + widthBy2, this.origin2d.y + heightBy2);
        return viewport;
    }

    private fun getViewportDimensions2d(): PointWebMercator {
        val viewportWidth = this.width * this.scale;
        val viewportHeight = this.height * this.scale;
        return PointWebMercator(viewportWidth, viewportHeight);
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if ( event == null ) {
            return false;
        }
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                return touchStart(event)
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(event)
                return true;
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                touchEnd(event)
                return true;
            }
        }
        return false;
    }

    private fun touchStart(event: MotionEvent): Boolean {
        if ( event.actionMasked == MotionEvent.ACTION_DOWN ) {
            return this.mouseDownHelper(event.x, event.y)
        } else if ( event.pointerCount == 2 ) {
            this.isDragging = false;
            this.pinchClientPoint1 = Point2d(event.getX(0).toDouble(), event.getY(0).toDouble())
            this.pinchClientPoint2 = Point2d(event.getX(1).toDouble(), event.getY(1).toDouble())
            this.pinchOriginalScale = this.scale
            return true
        }
        return false
    }

    private fun touchMove(event: MotionEvent) {
        if ( event.pointerCount == 1 ) {
            this.mouseMoveHelper(event.x, event.y);
        } else if ( event.pointerCount == 2 ) {
            val newPoint1 = Point2d(event.getX(0).toDouble(), event.getY(0).toDouble());
            val newPoint2 = Point2d(event.getX(1).toDouble(), event.getY(1).toDouble());

            val originalDistance: Double = this.pinchClientPoint1.calculateDistance(this.pinchClientPoint2);
            val thisDistance = newPoint1.calculateDistance(newPoint2);

            val targetScale = this.pinchOriginalScale / (thisDistance / originalDistance)
            this.scaleDriver = log2(1 / targetScale).toFloat()

            if ( this.scaleDriver > this.maxScaleDriver ) {
                this.scaleDriver = this.maxScaleDriver
            } else if ( this.scaleDriver < 0 ) {
                this.scaleDriver = 0F;
            }

            this.updateScale()
            this.viewportChanged()
            this.requestRedraw()
        }
    }

    private fun touchEnd(event: MotionEvent) {
        if ( event.actionMasked == MotionEvent.ACTION_UP ) {
            this.mouseUpHelper()
        } else {
            this.touchStart(event)
        }
    }

    private fun mouseDownHelper(offsetX: Float, offsetY: Float): Boolean {
        // Detect if the mouse event is outside the canvas
        if (offsetX < 0 || this.width < offsetX ||
            offsetY < 0 || this.height < offsetY) {
            return false
        }

        // Continue with starting the drag state
        this.mouseDownClient = Point2d(offsetX.toDouble(), offsetY.toDouble())
        this.mouseDownOrigin2d = this.origin2d.clone()
        this.isDragging = true

        return true
    }

    private fun mouseMoveHelper(offsetX: Float, offsetY: Float) {
        if (this.isDragging) {
            val xDifference = offsetX - this.mouseDownClient.x
            val yDifference = offsetY - this.mouseDownClient.y
            val viewportDimensions = this.getViewportDimensions2d()

            val percentageClientTraverseX = xDifference / this.width
            val percentageClientTraverseY = yDifference / this.height

            this.origin2d = PointWebMercator(this.mouseDownOrigin2d.x - viewportDimensions.x * percentageClientTraverseX,
                this.mouseDownOrigin2d.y - viewportDimensions.y * percentageClientTraverseY)

            this.viewportChanged()
            this.requestRedraw()
        }
    }

    private fun mouseUpHelper() {
        isDragging = false
    }
}
