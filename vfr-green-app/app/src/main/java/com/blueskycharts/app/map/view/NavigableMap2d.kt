package com.blueskycharts.app.map.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.util.JsonReader
import android.util.JsonToken
import android.view.View
import androidx.annotation.RestrictTo
import androidx.core.content.res.getStringOrThrow
import com.blueskycharts.app.R
import com.blueskycharts.app.coordinates.CoordinateConversion
import com.blueskycharts.app.coordinates.Point2d
import com.blueskycharts.app.coordinates.PointGeo
import com.blueskycharts.app.coordinates.PointWebMercator
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
    private var mapViews: Collection<SubMapPosition>;
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

    // html element variables
    private var containerWidth: Int;
    private var containerHeight: Int;

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
        this.containerWidth = 0;
        this.containerHeight = 0;

        this.mouseDownClient = Point2d();
        this.mouseDownOrigin2d = Point2d();
        this.touchMoveIdentifier = 0;

        this.mapViews = LinkedList<SubMapPosition>();
        this.isDragging = false;
        this.pinchClientPoint1 = Point2d();
        this.pinchClientPoint2 = Point2d();
        this.pinchOriginalScale = 0.0;

        // Startup the map
        this.setSize();

        /*
        this.addEventListeners();
         */
        this.retrieveConfiguration(URL("${mapRoot}/metadata.json"));
    }

    private fun updateScale() {
        this.scale = 1 / (2.0.pow(this.scaleDriver.toDouble()));
    }

    private fun setSize() {
        this.containerWidth = width;
        this.containerHeight = height;
    }

    private fun retrieveConfiguration(mapConfigurationFile: URL) {
        GlobalScope.launch {
            try {
                //val configText = mapConfigurationFile.readText();
                val reader = JsonReader(InputStreamReader(mapConfigurationFile.openStream()));
                val mapPositions = SubMapModel.readFromJsonReader(reader);
            } catch (e: Throwable) {
                print(e.message);
            }
        }
        /*
        $.getJSON(mapConfigurationFile,
        function(data: Record<string, SubMapModel>) {
            thisObj.initializeMapModel(data);
            thisObj.render();
        });

         */
    }

    override fun requestRedraw() {
        /*
        requestAnimationFrame(() => {
            this.render();
        });
         */
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val paint = Paint()
        val color = Color.BLUE
        paint.color = color
        canvas?.drawRect(Rect(0, 0, 300, 100), paint)
    }
}