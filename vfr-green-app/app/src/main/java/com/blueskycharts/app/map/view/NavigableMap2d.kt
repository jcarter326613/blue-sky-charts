package com.blueskycharts.app.map.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.JsonReader
import android.util.JsonToken
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.content.res.getStringOrThrow
import androidx.core.graphics.drawable.toBitmap
import com.blueskycharts.app.R
import com.blueskycharts.app.coordinates.*
import com.blueskycharts.app.map.models.BoxGeoModel
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.resources.TileProvider
import java.net.URL
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlinx.coroutines.*
import java.io.InputStreamReader

class NavigableMap2d(context: Context, attributes: AttributeSet) : View(context, attributes), Map {
    private val tileProvider: TileProvider;
    private val shadowTileProvider: TileProvider;
    //private val dataProvider: DataProvider;

    // Map state variables
    //private context: CanvasRenderingContext2D | null;
    private var mapBackground: SubMapPosition? = null;
    private var mapViews: LinkedList<SubMapPosition>? = null
    private val dataOverlayView: SubMapPosition? = null;
    private var origin2d: PointWebMercator;
    private var scale: Double;
    private var scaleDriver: Float;
    private val maxScaleDriver: Float = 12F;

    // Mouse event variables
    private val isDragging: Boolean;
    private val mouseDownClient: Point2d;
    private val mouseDownOrigin2d: Point2d;
    private val touchMoveIdentifier: Int;
    private val pinchClientPoint1: Point2d;
    private val pinchClientPoint2: Point2d;
    private val pinchOriginalScale: Double;

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
        this.tileProvider = TileProvider("${mapRoot}/sectional");
        this.shadowTileProvider = TileProvider("${mapRoot}/world-shadow");
        /*
        this.dataProvider = DataProvider();
         */
        if ( this.scaleDriver > this.maxScaleDriver ) {
            this.scaleDriver = this.maxScaleDriver;
        } else if (this.scaleDriver < 0) {
            this.scaleDriver = 0.0F;
        }
        this.scale = 0.0;
        this.updateScale();

        this.mouseDownClient = Point2d();
        this.mouseDownOrigin2d = Point2d();
        this.touchMoveIdentifier = 0;

        this.isDragging = false;
        this.pinchClientPoint1 = Point2d();
        this.pinchClientPoint2 = Point2d();
        this.pinchOriginalScale = 0.0;

        // Startup the map
        this.setupBackgroundShadow();
        /*
        this.addEventListeners();
         */
        this.retrieveConfiguration(URL("${mapRoot}/metadata.json"));
    }

    override fun requestRedraw() {
        this.postInvalidate()
    }

    fun setOverlayType(type: OverlayTypes): Boolean {
        /*
        let success: boolean
                if (type != OverlayTypes.None) {
                    let dataView = new MapDataView(this.dataProvider, type, this);
                    let extent = dataView.initialize();
                    if ( extent !== undefined ) {
                        this.dataOverlayView = new SubMapPosition(dataView, extent);
                        success = true;
                    } else {
                        success = false;
                    }
                } else {
                    if ( this.dataOverlayView !== undefined ) {
                        this.dataOverlayView.getSubMapView().dispose();
                        this.dataOverlayView = undefined;
                    }
                    success = true;
                }

        this.dataProvider.clearQueue();
        this.render();
        return success;
         */
        return true;
    }

    private fun viewportChanged() {
        this.tileProvider.clearQueue();
        /*
        this.dataProvider.clearQueue();
         */
    }

    private fun updateScale() {
        this.scale = 1 / (2.0.pow(this.scaleDriver.toDouble()));
    }

    private fun addEventListeners() {
        /*
        this.containerDiv.mousedown((event: JQuery.Event) => this.mouseDown(event));
        this.containerDiv.mousemove((event: JQuery.Event) => this.mouseMove(event));
        this.containerDiv.mouseup((event: JQuery.Event) => this.mouseUp(event));
        this.containerDiv.mousewheel((event: JQueryMousewheelEventObject) => this.mouseScroll(event))
        this.containerDiv.on("touchstart", (event: JQuery.Event) => this.touchStart(event));
        this.containerDiv.on("touchmove", (event: JQuery.Event) => this.touchMove(event));
        this.containerDiv.on("touchend", (event: JQuery.Event) => this.touchEnd(event));
        this.containerDiv.on("touchcancel", (event: JQuery.Event) => this.touchEnd(event));

        // Setup the window resize listener
        $(window).resize(() => {
            requestAnimationFrame(() => {
                this.setSize();
                this.viewportChanged();
                this.render();
            })
        });
         */
    }

    private fun getClientOffset(): Point2d {
        /*
        let offset = this.canvasObjHtml.offset();
        return new Point2d(offset?.left, offset?.top);
         */
        return Point2d(0.0, 0.0)
    }

    private fun setupBackgroundShadow() {
        val worldShadowView = MapTileView(this.shadowTileProvider, this);
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
            val subMapView = MapTileView(this.tileProvider, this);
            val fileExtent = subMapView.initialize(subMapModel) ?: continue;
            mapViewList.add(SubMapPosition(subMapView, fileExtent));
        }
        this.mapViews = mapViewList
    }

    private fun retrieveConfiguration(mapConfigurationFile: URL) {
        GlobalScope.launch {
            try {
                val reader = JsonReader(InputStreamReader(mapConfigurationFile.openStream()));
                val mapPositions = SubMapModel.readFromJsonReader(reader);
                this@NavigableMap2d.initializeMapModel(mapPositions)
                this@NavigableMap2d.postInvalidate()
            } catch (e: Throwable) {
                print(e.message);
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

        //val drawable = context.resources.getDrawable( R.drawable.ic_launcher_foreground, null)
        //var bitmap = drawable.toBitmap(93, 93, null)
        //canvas.drawBitmap(bitmap, 0F, 0F, null)

        /*
        if ( this.dataOverlayView !== undefined ) {
            let dataOverlay = this.dataOverlayView.getSubMapView();

            dataOverlay.resetRequestedInformationAgeRecord();
            canvas.save();
            this.renderSubMap(this.dataOverlayView, viewport2d);
            canvas.restore();

            // Draw the date indicating the oldest data displayed
            if ( this.dataProvider.isLoading() ) {
                canvas.save();
                this.renderInformationAgeBox("Loading weather data...");
                canvas.restore();
            } else {
                let informationAge = dataOverlay.getRequestedInformationAgeSeconds();
                if ( informationAge !== undefined ) {
                    // Draw the information age
                    canvas.save();
                    let informationAgeLabel = this.getInformationAgeLabel(informationAge);
                    this.renderInformationAgeBox(informationAgeLabel);
                    canvas.restore();

                    // Trigger a refresh for when the information age needs to be updated
                    let secondsToSleep: number;
                    if ( informationAge == 60 ) {
                        secondsToSleep = 1;
                    } else if ( informationAge == 0 ) {
                        secondsToSleep = 61;
                    } else {
                        secondsToSleep = (60 - (informationAge % 60)) + 1;
                    }
                    setTimeout(() => {
                        this.requestRedraw();
                    }, secondsToSleep * 1000);
                }
            }
        }

         */
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
}
