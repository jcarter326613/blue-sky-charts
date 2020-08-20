package com.blueskycharts.app.map.view

import android.content.Context
import android.graphics.*
import android.location.Location
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.text.toSpannable
import com.blueskycharts.app.R
import com.blueskycharts.app.coordinates.*
import com.blueskycharts.app.map.configuration.Inventory
import com.blueskycharts.app.map.configuration.MapConfiguration
import com.blueskycharts.app.map.models.ExtentModel
import com.blueskycharts.app.map.models.ProjectionWebMercatorModel
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.resources.DataProvider
import com.blueskycharts.app.map.resources.ShadowProvider
import com.blueskycharts.app.map.resources.TileProvider
import com.blueskycharts.app.preferences.Preferences
import com.blueskycharts.app.utility.Log
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timerTask
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow


class NavigableMap2d(context: Context, attributes: AttributeSet) :
    Map(context, attributes) {
    private var tileProvider: TileProvider? = null
    private var shadowTileProvider: ShadowProvider? = null
    private val dataProvider: DataProvider
    private val itemTextPaint = Paint()
    private val textHeight: Float
    private val textBackgroundPaint = Paint()
    private val textStrokePaint = Paint()
    private val mapExpirationPaint = Paint()
    private val currentLocationCenterPaint = Paint()
    private val currentLocationCenterOldPaint = Paint()
    private val currentLocationEdgePaint = Paint()
    private val currentLocationBorderPaint = Paint()
    private val currentLocationCenterRect: RectF
    private val currentLocationEdgeRect: RectF
    private val positionUpdatePending = AtomicBoolean(false)

    // Map state variables
    private var mapBackground: SubMapPosition? = null
    private var mapViews: LinkedList<SubMapPosition>? = null
    private var dataOverlayView: SubMapPosition? = null
    var isExpired: Boolean = false
    val isCurrentLocationInMap: Boolean
        get() {
            val currentLocation2d = this.currentLocation2d ?: return false
            val rectangularAreaBounds = this.rectangularAreaBounds ?: return false
            return rectangularAreaBounds.convertToBox2d().contains(currentLocation2d)
        }
    private var origin2d: Point2d? = null
        get() {
            return if (trackCurrentLocation) {
                if (isCurrentLocationInMap) {
                    currentLocation2d
                } else {
                    field
                }
            } else {
                field
            }
        }
    private var currentLocation2d: Point2d? = null
    var trackCurrentLocation: Boolean = true
        set(value) {
            field = value
            requestRedraw()
        }
    private var currentLocationTime: Long = 0
    private val currentLocationOldAgeThresholdMs: Long = 30 * 1000 /* 30 seconds */
    private var scale: Double
    private var scaleDriver: Float = 0F
    private val maxScaleDriver: Float = 12F
    private var drawnBounds: Box2d? = null
    private var rectangularAreaBounds: RectangularArea? = null
    private var mapPositionPropertyName: String = ""
    private var activeOverlayType: OverlayTypes = OverlayTypes.None

    // Mouse event variables
    private var isDragging: Boolean
    private var mouseDownClient: Point2d
    private var mouseDownOrigin2d: Point2d
    private var pinchClientPoint1: Point2d
    private var pinchClientPoint2: Point2d
    private var pinchOriginalScale: Double

    init {
        //Setup the paint objects
        itemTextPaint.color = Color.BLACK
        itemTextPaint.style = Paint.Style.FILL_AND_STROKE
        itemTextPaint.strokeWidth = convertDipToPixels(1f)
        itemTextPaint.textSize = convertSdipToPixels(20f)
        itemTextPaint.textAlign = Paint.Align.LEFT
        val buffer = Rect()
        itemTextPaint.getTextBounds("00000", 0, 5, buffer)
        textHeight = buffer.height().toFloat()

        textBackgroundPaint.color = Color.WHITE
        textBackgroundPaint.style = Paint.Style.FILL

        textStrokePaint.color = Color.BLACK
        textStrokePaint.style = Paint.Style.STROKE
        textStrokePaint.strokeWidth = convertDipToPixels(1f)

        mapExpirationPaint.color = Color.BLACK
        mapExpirationPaint.style = Paint.Style.FILL_AND_STROKE
        mapExpirationPaint.textSize = convertSdipToPixels(20f)
        mapExpirationPaint.textAlign = Paint.Align.LEFT
        mapExpirationPaint.strokeWidth = convertDipToPixels(1f)
        mapExpirationPaint.setShadowLayer(10f, 0f, 0f, Color.RED)

        currentLocationCenterPaint.color = Color.BLUE
        currentLocationCenterPaint.style = Paint.Style.FILL
        currentLocationCenterPaint.strokeWidth = convertDipToPixels(6f)

        currentLocationCenterOldPaint.color = Color.GRAY
        currentLocationCenterOldPaint.style = Paint.Style.FILL
        currentLocationCenterOldPaint.strokeWidth = convertDipToPixels(6f)

        currentLocationEdgePaint.color = Color.WHITE
        currentLocationEdgePaint.style = Paint.Style.FILL
        currentLocationEdgePaint.strokeWidth = convertDipToPixels(2f) + currentLocationCenterPaint.strokeWidth

        currentLocationBorderPaint.color = Color.BLACK
        currentLocationBorderPaint.style = Paint.Style.STROKE
        currentLocationBorderPaint.strokeWidth = convertDipToPixels(1f)

        currentLocationCenterRect = RectF(-currentLocationCenterPaint.strokeWidth, -currentLocationCenterPaint.strokeWidth, currentLocationCenterPaint.strokeWidth, currentLocationCenterPaint.strokeWidth)
        currentLocationEdgeRect = RectF(-currentLocationEdgePaint.strokeWidth, -currentLocationEdgePaint.strokeWidth, currentLocationEdgePaint.strokeWidth, currentLocationEdgePaint.strokeWidth)

        // Set all constant and derived defaults
        this.dataProvider = DataProvider(this)

        if ( this.scaleDriver > this.maxScaleDriver ) {
            this.scaleDriver = this.maxScaleDriver;
        } else if (this.scaleDriver < 0) {
            this.scaleDriver = 0.0F
        }
        this.scale = 0.0
        this.updateScale()

        this.mouseDownClient = Point2d()
        this.mouseDownOrigin2d = Point2d()

        this.isDragging = false
        this.pinchClientPoint1 = Point2d()
        this.pinchClientPoint2 = Point2d()
        this.pinchOriginalScale = 0.0

        // Startup the map
        this.retrieveConfiguration()
    }

    fun updateCurrentLocation(location: Location) {
        currentLocationTime = location.time

        val loc = PointGeo(longitude = location.longitude, latitude = location.latitude)
        when (val rectangularAreaBounds = this.rectangularAreaBounds) {
            is RectangularAreaLcc -> {
                this.currentLocation2d = loc.convertToPointLcc(rectangularAreaBounds.projectionDescription).convertToPoint2d()
                this.requestRedraw()
            }
            is RectangularAreaWebMercator -> {
                this.currentLocation2d = loc.convertToPointWebMercator().convertToPoint2d()
                this.requestRedraw()
            }
            null -> {
            }
            else -> {
                Log.error(null, "Unknown rectangular area type encountered when trying to move map to current location")
            }
        }
    }

    fun setOverlayType(type: OverlayTypes): Boolean {
        Log.overlaySelection(type)
        activeOverlayType = type

        var success: Boolean
        val rectangularAreaBounds = this.rectangularAreaBounds
        if (type != OverlayTypes.None && rectangularAreaBounds != null) {
            val dataView = MapDataView(this.dataProvider, type, this)
            val extent = dataView.initialize(rectangularAreaBounds)
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
        this.tileProvider?.clearQueue()
        this.dataProvider.clearQueue()
    }

    private fun updateScale() {
        this.scale = 1 / (2.0.pow(this.scaleDriver.toDouble()));
    }

    private fun setupBackgroundShadow(configuration: MapConfiguration) {
        // Only setup the background if the map config contains web mercator maps
        val shadowTileProvider = ShadowProvider(context)
        this.shadowTileProvider = shadowTileProvider

        val firstKey = configuration.data.maps.keys.firstOrNull()
        if ( firstKey != null ) {
            val firstConfig = configuration.data.maps[firstKey]
            val firstVersionKey = firstConfig?.versions?.keys?.firstOrNull()
            if ( firstVersionKey != null ) {
                val firstVersion = firstConfig.versions[firstVersionKey]
                if ( firstVersion?.projectionWebMercator?.extents != null ) {
                    val topLeft = PointGeo(PointGeo.minLongitude, PointGeo.maxLatitude)
                    val bottomRight = PointGeo(PointGeo.maxLongitude, PointGeo.minLatitude)
                    val topLeftMercator = topLeft.convertToPointWebMercator()
                    val bottomRightMercator = bottomRight.convertToPointWebMercator()
                    val projectionWebMercator = ProjectionWebMercatorModel(ExtentModel(
                        topLeftMercator.x, topLeftMercator.y, bottomRightMercator.x, bottomRightMercator.y
                    ))

                    val worldShadowView = MapTileView(shadowTileProvider, this);
                    val worldShadowMercatorExtents = RectangularAreaWebMercator.maxMercator
                    val shadowMapModel = SubMapModel(
                        tileWidth = 256,
                        maxZoom = 0,
                        imageHeight = 256.0,
                        imageWidth = 256.0,
                        version = "1",
                        changeSet = null,
                        effectiveDate = null,
                        expirationDate = null,
                        projectionLcc = null,
                        projectionWebMercator = projectionWebMercator
                    )
                    worldShadowView.initialize("world-shadow", shadowMapModel)
                    this.mapBackground = SubMapPosition(worldShadowView, worldShadowMercatorExtents.convertToBox2d())
                }
            }
        }
    }

    private fun initializeMapModel(group: Inventory.Group, configuration: MapConfiguration) {
        // Add the world  VFR charts
        var firstMap = true
        var drawnBounds: Box2d? = null
        var rectangularAreaBounds: RectangularArea? = null
        var mercatorMap = false
        val mapViewList = LinkedList<SubMapPosition>()
        isExpired = false
        for (mapName in configuration.mapList) {
            val mapData = configuration.getCurrentVersion(mapName, true)
            if ( mapData != null ) {
                if ( firstMap ) {
                    this.tileProvider = TileProvider(this, group)
                    firstMap = false
                    mercatorMap = mapData.projectionWebMercator?.extents != null
                }

                val tileProvider = this.tileProvider
                if ( tileProvider != null ) {
                    val subMapView = MapTileView(tileProvider, this)
                    val fileExtent = subMapView.initialize(mapName, mapData) ?: continue
                    mapViewList.add(SubMapPosition(subMapView, fileExtent));
                    drawnBounds = drawnBounds?.union(fileExtent) ?: fileExtent
                    val rectangularAreaExtents = subMapView.fileExtent
                    if ( rectangularAreaExtents != null ) {
                        rectangularAreaBounds = if (rectangularAreaBounds != null) {
                            rectangularAreaExtents.union(rectangularAreaBounds)
                        } else {
                            rectangularAreaExtents
                        }
                    }
                }
                isExpired = isExpired || mapData.isExpired
            }
        }

        if (drawnBounds != null) {
            val tempOrigin = this.origin2d
            if (tempOrigin == null ||
                tempOrigin.x < drawnBounds.upperLeft.x || tempOrigin.y < drawnBounds.upperLeft.y ||
                tempOrigin.x > drawnBounds.lowerRight.x || tempOrigin.y > drawnBounds.lowerRight.y
            ) {
                this.origin2d = Point2d(
                    (drawnBounds.upperLeft.x + drawnBounds.lowerRight.x) / 2,
                    (drawnBounds.upperLeft.y + drawnBounds.lowerRight.y) / 2
                )
                this.scaleDriver = 0F
            }
        }

        if ( mercatorMap ) {
            setupBackgroundShadow(configuration)
        }

        this.updateScale()
        this.drawnBounds = drawnBounds
        this.rectangularAreaBounds = rectangularAreaBounds
        this.mapViews = mapViewList
    }

    private fun retrieveConfiguration() {
        GlobalScope.launch {
            val mapGroupId = Preferences.instance.getIntValue(Preferences.propertyNameDisplayedMapGroupId, Preferences.defaultValueDisplayedMapGroupId)
            val mapGroup = Inventory.instance.findGroupById(mapGroupId)?: return@launch
            var config = mapGroup.getConfiguration() ?: return@launch
            if (!config.displayAll) {
                val subMapId = Preferences.instance.getStringValue(
                    Preferences.propertyNameDisplayedSubMapId,
                    Preferences.defaultValueDisplayedSubMapId
                )
                config = config.filterForSubMap(subMapId) ?: return@launch
                this@NavigableMap2d.mapPositionPropertyName = Preferences.propertyTemplateMapPosition(config.groupId, subMapId)
                Log.mapSelection("${config.groupId}.$subMapId")
            } else {
                this@NavigableMap2d.mapPositionPropertyName = Preferences.propertyTemplateMapPosition(config.groupId, "all")
                Log.mapSelection("${config.groupId}.all")
            }
            val mapPositionString = Preferences.instance.getStringValue(this@NavigableMap2d.mapPositionPropertyName, Preferences.defaultValueMapPosition)

            this@NavigableMap2d.origin2d = null
            if ( Preferences.defaultValueMapPosition != mapPositionString ) {
                val parts = mapPositionString.split("|")
                if (parts.size == 3) {
                    this@NavigableMap2d.origin2d = Point2d(parts[1].toDouble(), parts[2].toDouble())
                    this@NavigableMap2d.scaleDriver = parts[0].toFloat()
                }
            }
            this@NavigableMap2d.updateScale()
            this@NavigableMap2d.initializeMapModel(mapGroup, config)
            this@NavigableMap2d.postInvalidate()
        }
    }

    override fun onDetachedFromWindow() {
        this.tileProvider?.clearQueue()
        this.dataProvider.clearQueue()
        this.tileProvider?.map = null
        this.dataProvider.map = null
        this.mapViews = null

        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val viewportMercator = this.calculateViewport()?: return

        // Draw the solid background color
        val paint = Paint()
        paint.color = Color.parseColor("#323232")
        canvas.drawRect(Rect(0, 0, this.width, this.height), paint)

        // Put the origin in the center of the screen
        val originCenterRestoreCount = canvas.save()
        canvas.translate(this.width / 2.0F, this.height / 2.0F);

        // Draw the shadow
        if ( this.mapBackground != null ) {
            val restoreCount = canvas.save()
            this.drawSubMap(this.mapBackground!!, viewportMercator, canvas)
            canvas.restoreToCount(restoreCount)
        }

        // Draw each sub map
        if ( this.mapViews != null ) {
            val mapViews = this.mapViews!!
            for (submap in mapViews) {
                val restoreCount = canvas.save()
                this.drawSubMap(submap, viewportMercator, canvas)
                canvas.restoreToCount(restoreCount)
            }
        }

        // Draw any data overlays
        val dataOverlayView = this.dataOverlayView
        if ( dataOverlayView != null ) {
            val dataOverlay = dataOverlayView.subMapView

            dataOverlay.resetRequestedInformationAgeRecord()
            canvas.save()
            this.drawSubMap(dataOverlayView, viewportMercator, canvas)
            canvas.restore()
        }

        // Draw the current location
        this.drawCurrentLocation(viewportMercator, canvas)

        // Draw the date indicating the oldest data displayed
        canvas.restoreToCount(originCenterRestoreCount)
        if ( dataOverlayView != null) {
            val dataOverlay = dataOverlayView.subMapView
            val restoreCount = canvas.save()

            if (this.dataProvider.isWaitingForErrors()) {
                val informationAge = dataOverlay.getRequestedInformationAgeSeconds()
                if (informationAge != null) {
                    val informationAgeLabel = this.getInformationAgeLabel(informationAge)
                    this.renderInformationAgeBox(informationAgeLabel, canvas)
                } else {
                    this.renderInformationAgeBox(
                        listOf(
                            SpannableString("Information stale")
                        ), canvas
                    )
                }
            } else {
                if (this.dataProvider.isLoading()) {
                    this.renderInformationAgeBox(
                        listOf(SpannableString("Loading weather data...")),
                        canvas
                    )
                } else {
                    val informationAge = dataOverlay.getRequestedInformationAgeSeconds()
                    if (informationAge != null) {
                        val informationAgeLabel = this.getInformationAgeLabel(informationAge)
                        this.renderInformationAgeBox(informationAgeLabel, canvas)
                    }
                }
            }

            canvas.restoreToCount(restoreCount)
        }

        // Draw the expired map indicator
        if (isExpired) {
            val xBuffer = convertDipToPixels(6f)
            val yBuffer = convertSdipToPixels(6f) + convertDipToPixels(2f)
            canvas.drawText("Map expired", xBuffer, height.toFloat() - yBuffer, mapExpirationPaint)
        }
    }

    private fun drawCurrentLocation(viewport: Box2d, canvas: Canvas) {
        val currentLocation = this.currentLocation2d ?: return
        val origin2d = this.origin2d ?: return
        val age = Date().time - currentLocationTime
        val currentLocationIsOld = age > currentLocationOldAgeThresholdMs

        val saveCount = canvas.save()

        val percentageX = (currentLocation.x - origin2d.x) / viewport.width
        val percentageY = (currentLocation.y - origin2d.y) / viewport.height
        canvas.translate((width * percentageX).toFloat(), (height * percentageY).toFloat())

        canvas.drawArc(currentLocationEdgeRect, 0f, 360f, true, this.currentLocationEdgePaint)
        canvas.drawArc(currentLocationEdgeRect, 0f, 360f, true, this.currentLocationBorderPaint)
        if (currentLocationIsOld) {
            canvas.drawArc(
                currentLocationCenterRect,
                0f,
                360f,
                true,
                this.currentLocationCenterOldPaint
            )
        } else {
            canvas.drawArc(
                currentLocationCenterRect,
                0f,
                360f,
                true,
                this.currentLocationCenterPaint
            )
        }

        canvas.restoreToCount(saveCount)
    }

    private fun drawSubMap(submap: SubMapPosition, viewport2d: Box2d, canvas: Canvas) {
        val origin2d = this.origin2d?: return

        // Calculate the viewport from the perspective of the un modified sub map
        val viewportOverlap2d = submap.location.intersection(viewport2d)
        if (viewportOverlap2d != null) {
            // Figure out what we need to scale the viewport by to fit it on the screen
            val scale = this.width / viewport2d.width
            val drawArea = viewportOverlap2d.shift(origin2d, reverse=true).scale(scale)

            // Draw the submap
            submap.subMapView.render(canvas, viewportOverlap2d, drawArea)
        } else {
            submap.subMapView.moveOffscreen();
        }
    }

    private fun renderInformationAgeBox(informationAgeLabel: List<SpannableString>, canvas: Canvas) {
        val lineHeight = this.textHeight
        val lineSpace = lineHeight * 1f
        val textDimensions = this.itemTextPaint.measureText(informationAgeLabel.toString())
        val heightBuffer = this.convertDipToPixels(14f)
        val widthBuffer = this.convertDipToPixels(10f)
        val margin = this.convertDipToPixels(5f)

        canvas.save()
        canvas.translate(((textDimensions + widthBuffer) / 2.0f) + margin, ((lineHeight + heightBuffer) / 2.0f) + margin)
        canvas.translate(-textDimensions / 2F, lineHeight / 2f)
        for (str in informationAgeLabel) {
            val oldWidth = itemTextPaint.strokeWidth
            val oldColor = itemTextPaint.color
            itemTextPaint.strokeWidth *= 4
            itemTextPaint.color = Color.WHITE
            itemTextPaint.setShadowLayer(20f, 0f, 0f, Color.argb(255, 255, 255, 255))
            drawSpannableString(str, canvas, itemTextPaint)
            itemTextPaint.strokeWidth = oldWidth
            itemTextPaint.color = oldColor
            itemTextPaint.setShadowLayer(0f, 0f, 0f, Color.argb(255, 255, 255, 255))
            drawSpannableString(str, canvas, itemTextPaint)

            canvas.translate(0F, lineHeight + lineSpace)
        }
        canvas.restore()
    }

    private fun drawSpannableString(spannableString: SpannableString, canvas: Canvas, paint: Paint) {
        var next: Int
        var xStart = 0f
        var xEnd: Float
        var i = 0
        while (i < spannableString.length) {
            // find the next span transition
            next = spannableString.nextSpanTransition(i, spannableString.length, CharacterStyle::class.java)

            // measure the length of the span
            xEnd = xStart + paint.measureText(spannableString, i, next)

            // draw the highlight (background color) first
            val bgSpans: Array<BackgroundColorSpan> = spannableString.getSpans(i, next, BackgroundColorSpan::class.java)
            if (bgSpans.isNotEmpty()) {
                val oldColor = this.textBackgroundPaint.color
                this.textBackgroundPaint.color = bgSpans[0].backgroundColor
                canvas.drawRect(xStart, paint.fontMetrics.top, xEnd, paint.fontMetrics.bottom, this.textBackgroundPaint )
                this.textBackgroundPaint.color = oldColor
            }

            // draw the text with an optional foreground color
            val fgSpans: Array<ForegroundColorSpan> = spannableString.getSpans(i, next, ForegroundColorSpan::class.java)
            if (fgSpans.isNotEmpty()) {
                val oldColor: Int = paint.color
                paint.color = fgSpans[0].foregroundColor
                canvas.drawText(spannableString.toString(), i, next, xStart, 0F, paint)
                paint.color = oldColor
            } else {
                canvas.drawText(spannableString.toString(), i, next, xStart, 0F, paint)
            }
            xStart = xEnd
            i = next
        }
    }

    private fun getInformationAgeLabel(ageSeconds: LongRange): List<SpannableString> {
        val sb0: SpannableString
        val sb1: SpannableString
        val sb2: SpannableString

        val sb0String: String = when (activeOverlayType) {
            OverlayTypes.Visibility -> "Visibility"
            OverlayTypes.Temperature -> "Temperature"
            OverlayTypes.SurfaceWind -> "Wind"
            OverlayTypes.DewPointSpread -> "Dew Point Spread"
            OverlayTypes.CloudCover -> "Cloud Cover"
            OverlayTypes.Ceiling -> "Ceiling"
            OverlayTypes.Category -> "Flight Category"
            else -> activeOverlayType.name
        }
        sb0 = SpannableString(sb0String)

        if ( ageSeconds.last < 60 ) {
            sb1 = SpannableString("Issued 1")
            sb2 = SpannableString("minute ago")
        } else if ( ageSeconds.last - ageSeconds.first <= MapDataView.maxNoRangeDisplayDiff ) {
            sb1 = SpannableString("Issued ${ceil(ageSeconds.last / 60.0).toInt()}")
            sb2 = SpannableString("minutes ago")
        } else {
            val age1String = " ${ceil(ageSeconds.first / 60.0).toInt()} "
            val age2String = " ${ceil(ageSeconds.last / 60.0).toInt()} "
            val wordIssued = "Issued "
            val wordTo = " to "
            sb1 = SpannableString("$wordIssued$age1String$wordTo$age2String")
            sb2 = SpannableString("minutes ago")
            sb1.setSpan(
                BackgroundColorSpan(MapDataView.newInformationColor),
                wordIssued.length,
                age1String.length + wordIssued.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )  //For text coloring change BackgroundColorSpan to ForegroudColorSpan
            sb1.setSpan(
                BackgroundColorSpan(MapDataView.oldInformationColor),
                wordIssued.length + age1String.length + wordTo.length,
                wordIssued.length + age1String.length + wordTo.length + age2String.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        return if (dataProvider.isWaitingForErrors()) {
            listOf(sb0, sb1, sb2, SpannableString("Information stale"))
        } else {
            listOf(sb0, sb1, sb2)
        }
    }

    /**
     * Returns the viewport in unscaled coordinates.
     */
    private fun calculateViewport(): Box2d? {
        val viewportDimensions = this.getViewportDimensions();
        return origin2d?.createBoxAround(viewportDimensions)
    }

    private fun getViewportDimensions(): Point2d {
        val drawnBounds = this.drawnBounds ?: return Point2d(0.0, 0.0)

        return if ( this.width < this.height ) {
            Point2d(drawnBounds.width * scale,
                drawnBounds.width * scale * (this.height / this.width.toDouble()))
        } else {
            Point2d(
                drawnBounds.height * scale * (this.width / this.height.toDouble()),
                drawnBounds.height * scale)
        }
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

            updatePositionRecord()
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
        this.mouseDownOrigin2d = this.origin2d?.clone()?: return false
        this.mouseDownClient = Point2d(offsetX.toDouble(), offsetY.toDouble())
        this.isDragging = true

        return true
    }

    private fun mouseMoveHelper(offsetX: Float, offsetY: Float) {
        if (this.isDragging) {
            this.trackCurrentLocation = false
            Preferences.instance.setPreference(Preferences.propertyNameMapTrackLocation, false)
            val xDifference = offsetX - this.mouseDownClient.x
            val yDifference = offsetY - this.mouseDownClient.y
            val viewport = this.calculateViewport()?: return
            val viewportDimensions = Point2d(viewport.width, viewport.height)

            val percentageClientTraverseX = xDifference / this.width
            val percentageClientTraverseY = yDifference / this.height

            this.origin2d =
                Point2d(this.mouseDownOrigin2d.x - viewportDimensions.x * percentageClientTraverseX,
                    this.mouseDownOrigin2d.y - viewportDimensions.y * percentageClientTraverseY)

            updatePositionRecord()
            this.viewportChanged()
            this.requestRedraw()
        }
    }

    private fun mouseUpHelper() {
        isDragging = false
    }

    private fun updatePositionRecord() {
        if (this.origin2d != null) {
            if (!positionUpdatePending.getAndSet(true)) {
                GlobalScope.launch {
                    delay(2000L /*2 seconds*/)
                    positionUpdatePending.set(false)
                    val origin = this@NavigableMap2d.origin2d ?: return@launch
                    Preferences.instance.setPreference(
                        this@NavigableMap2d.mapPositionPropertyName,
                        "${this@NavigableMap2d.scaleDriver}|${origin.x}|${origin.y}"
                    )
                }
            }
        }
    }
}
