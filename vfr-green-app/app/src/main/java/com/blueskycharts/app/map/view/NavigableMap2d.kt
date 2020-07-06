package com.blueskycharts.app.map.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.res.getStringOrThrow
import com.blueskycharts.app.R
import com.blueskycharts.app.coordinates.*
import com.blueskycharts.app.map.models.BoxGeoModel
import com.blueskycharts.app.map.models.MapMetaDataModelCollection
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.resources.DataProvider
import com.blueskycharts.app.map.resources.ShadowProvider
import com.blueskycharts.app.map.resources.TileProvider
import com.blueskycharts.app.remoteassests.AssetProvider
import com.blueskycharts.app.remoteassests.Volatility
import java.net.URL
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

class NavigableMap2d(context: Context, attributes: AttributeSet) : Map(context, attributes) {
    private val tileProvider: TileProvider
    private val shadowTileProvider: TileProvider
    private val dataProvider: DataProvider
    private val redrawTimer = Timer(false)
    private val itemTextPaint = Paint()
    private val textHeight: Float
    private val textBackgroundPaint = Paint()
    private val textStrokePaint = Paint()

    // Map state variables
    private var mapBackground: SubMapPosition? = null
    private var mapViews: LinkedList<SubMapPosition>? = null
    private var dataOverlayView: SubMapPosition? = null
    private var originMercator: PointWebMercator
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
                originMercator = CoordinateConversion.convertPointGeoToPointWebMercator(PointGeo(originLongitude.toDouble(), originLatitude.toDouble()));
            } finally {
                recycle()
            }
        }

        //Setup the paint objects
        itemTextPaint.color = Color.BLACK
        itemTextPaint.style = Paint.Style.FILL_AND_STROKE
        itemTextPaint.strokeWidth = convertDipToPixels(1f)
        itemTextPaint.textSize = convertDipToPixels(20f)
        itemTextPaint.textAlign = Paint.Align.CENTER
        val buffer = Rect()
        itemTextPaint.getTextBounds("00000", 0, 5, buffer)
        textHeight = buffer.height().toFloat()

        textBackgroundPaint.color = Color.WHITE
        textBackgroundPaint.style = Paint.Style.FILL

        textStrokePaint.color = Color.BLACK
        textStrokePaint.style = Paint.Style.STROKE
        textStrokePaint.strokeWidth = convertDipToPixels(1f)

        // Set all constant and derived defaults
        this.tileProvider = TileProvider(mapRoot, "jpg", context)
        this.shadowTileProvider = ShadowProvider(context)
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

    fun setOverlayType(type: OverlayTypes): Boolean {
        var success: Boolean
        if (type != OverlayTypes.None) {
            val dataView = MapDataView(this.dataProvider, type, this)
            val extent = dataView.initialize("", null)
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
        val worldShadowView = MapTileView(this.shadowTileProvider, this);
        val worldShadowMercatorExtents = CoordinateConversion.maxMercator
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
            imageHeight = 256.0,
            imageWidth = 256.0,
            imageHeightScale = 1.0,
            imageWidthScale = 1.0,
            version = "1"
        )
        worldShadowView.initialize("world-shadow", shadowMapModel)
        this.mapBackground = SubMapPosition(worldShadowView, worldShadowMercatorExtents)
    }

    private fun initializeMapModel(data: MapMetaDataModelCollection) {
        // Add the world  VFR charts
        val mapViewList = LinkedList<SubMapPosition>()
        val now = Date()
        for (map in data.maps) {
            val mapName = map.key
            val mapData = map.value
            var latestActiveVersion: Date? = null
            var latestActiveVersionMap: SubMapModel? = null
            for (versionEntry in mapData.versions) {
                val versionDate = this.convertVersionStringToDate(versionEntry.key)
                if ( versionDate != null && ( versionDate < now && (latestActiveVersion == null || versionDate > latestActiveVersion)) ) {
                    latestActiveVersion = versionDate
                    latestActiveVersionMap = versionEntry.value
                }
            }
            if ( latestActiveVersionMap != null ) {
                val subMapView = MapTileView(this.tileProvider, this)
                val fileExtent = subMapView.initialize(mapName, latestActiveVersionMap) ?: continue
                mapViewList.add(SubMapPosition(subMapView, fileExtent));
            }
        }
        this.mapViews = mapViewList
    }

    private fun convertVersionStringToDate(dateString: String): Date? {
        if (dateString.length < 19 ) {
            return null
        }
        @Suppress("DEPRECATION")
        return Date(dateString.substring(0, 4).toInt() - 1900, dateString.substring(5, 7).toInt() - 1, dateString.substring(8, 10).toInt(),
                dateString.substring(11, 13).toInt(), dateString.substring(14, 16).toInt(), dateString.substring(17, 19).toInt())
    }

    private fun retrieveConfiguration(mapConfigurationFile: URL) {
        val assetProvider = AssetProvider(context)
        assetProvider.retrieveAsset(mapConfigurationFile, Volatility.DayCache) {
            val reader = it.asJsonReader()
            if ( reader != null ) {
                val mapPositions = MapMetaDataModelCollection.readFromJsonReader(reader);
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

            // Draw the date indicating the oldest data displayed
            if ( this.dataProvider.isLoading() ) {
                canvas.save()
                this.renderInformationAgeBox("Loading weather data...", canvas)
                canvas.restore()
            } else {
                val informationAge = dataOverlay.getRequestedInformationAgeSeconds()
                if ( informationAge != null ) {
                    // Draw the information age
                    canvas.save()
                    val informationAgeLabel = this.getInformationAgeLabel(informationAge)
                    this.renderInformationAgeBox(informationAgeLabel, canvas)
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
        }
    }

    private fun drawSubMap(submap: SubMapPosition, viewport2d: BoxWebMercator, canvas: Canvas) {
        // Calculate the viewport from the perspective of the un modified sub map
        val mapPosition2d: Box2d = CoordinateConversion.convertBoxMercatorToBox2d(submap.location)
        val viewportOverlap2d = mapPosition2d.intersection(CoordinateConversion.convertBoxMercatorToBox2d(viewport2d))
        val origin2d = CoordinateConversion.convertPointMercatorToPoint2d(this.originMercator)
        if (viewportOverlap2d != null) {
            val originalWidth = submap.subMapView.originalWidth2d;
            val originalHeight = submap.subMapView.originalHeight2d;

            val mapMercatorWidth = mapPosition2d.lowerRight.x - mapPosition2d.upperLeft.x;
            val mapMercatorHeight = mapPosition2d.lowerRight.y - mapPosition2d.upperLeft.y;
            val mapScale = (this.width * mapMercatorWidth) / (originalWidth * viewport2d.width);

            val mapShiftX = (mapPosition2d.upperLeft.x - origin2d.x) * this.width / viewport2d.width
            val mapShiftY = (mapPosition2d.upperLeft.y - origin2d.y) * this.height / viewport2d.width

            canvas.translate(this.width / 2.0F, this.height / 2.0F);
            canvas.translate(mapShiftX.toFloat(), mapShiftY.toFloat());

            // Figure out the part of the map we want to draw in 2d coordinates relative to the upper left corner
            val subMapDrawSection = Box2d(
                originalWidth * (viewportOverlap2d.upperLeft.x - mapPosition2d.upperLeft.x) / mapMercatorWidth,
                originalHeight * (viewportOverlap2d.upperLeft.y - mapPosition2d.upperLeft.y) / mapMercatorHeight,
                originalWidth * (viewportOverlap2d.lowerRight.x - mapPosition2d.upperLeft.x) / mapMercatorWidth,
                originalHeight * (viewportOverlap2d.lowerRight.y - mapPosition2d.upperLeft.y) / mapMercatorHeight);

            // Draw the submap
            submap.subMapView.render(canvas, subMapDrawSection, mapScale);
        } else {
            submap.subMapView.moveOffscreen();
        }
    }

    private fun renderInformationAgeBox(informationAgeLabel: String, canvas: Canvas) {
        val lineHeight = this.textHeight
        val textDimensions = this.itemTextPaint.measureText(informationAgeLabel)
        val heightBuffer = this.convertDipToPixels(14f)
        val widthBuffer = this.convertDipToPixels(10f)
        val margin = this.convertDipToPixels(5f)

        val boxRect = Box2d(-(textDimensions + widthBuffer) / 2.0, -(lineHeight + heightBuffer) / 2.0,
            (textDimensions + widthBuffer) / 2.0, (lineHeight + heightBuffer) / 2.0)

        canvas.save()
        canvas.translate(-boxRect.upperLeft.x.toFloat() + margin, -boxRect.upperLeft.y.toFloat() + margin)
        canvas.drawRect(boxRect.upperLeft.x.toFloat(), boxRect.upperLeft.y.toFloat(), boxRect.lowerRight.x.toFloat(), boxRect.lowerRight.y.toFloat(), this.textBackgroundPaint)
        canvas.drawRect(boxRect.upperLeft.x.toFloat(), boxRect.upperLeft.y.toFloat(), boxRect.lowerRight.x.toFloat(), boxRect.lowerRight.y.toFloat(), this.textStrokePaint)
        canvas.drawText(informationAgeLabel, 0f, lineHeight / 2f, this.itemTextPaint)
        canvas.restore()
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
        val origin2d = CoordinateConversion.convertPointMercatorToPoint2d(this.originMercator)
        val viewport = Box2d(origin2d.x - widthBy2, origin2d.y - heightBy2,
            origin2d.x + widthBy2, origin2d.y + heightBy2);
        return CoordinateConversion.convertBox2dToBoxMercator(viewport);
    }

    private fun getViewportDimensions2d(): Point2d {
        val viewportWidth = this.width * this.scale;
        val viewportHeight = this.height * this.scale;
        return Point2d(viewportWidth, viewportHeight);
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
        this.mouseDownOrigin2d = CoordinateConversion.convertPointMercatorToPoint2d(this.originMercator)
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

            this.originMercator = CoordinateConversion.convertPoint2dToPointMercator(
                Point2d(this.mouseDownOrigin2d.x - viewportDimensions.x * percentageClientTraverseX,
                    this.mouseDownOrigin2d.y - viewportDimensions.y * percentageClientTraverseY))

            this.viewportChanged()
            this.requestRedraw()
        }
    }

    private fun mouseUpHelper() {
        isDragging = false
    }
}
