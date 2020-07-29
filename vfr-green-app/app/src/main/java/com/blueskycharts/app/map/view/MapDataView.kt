package com.blueskycharts.app.map.view

import android.graphics.*
import com.blueskycharts.app.coordinates.*
import com.blueskycharts.app.map.models.WeatherCondition
import com.blueskycharts.app.map.resources.DataProvider
import com.blueskycharts.app.map.resources.DataReceiver
import com.blueskycharts.app.utility.Log
import kotlin.math.*

class MapDataView(private val dataProvider: DataProvider, private val overlayType: OverlayTypes, private val map: Map) : SubMapView, DataReceiver {
    private val maxVfrElevation = 18000

    // Metadata
    private var minDataAgeSeconds: Long? = null
    private var maxDataAgeSeconds: Long? = null
    override var fileExtent: RectangularArea? = null
        private set

    // Rendering
    private var isDisposed: Boolean = false
    private val itemLightBackgroundPaint = Paint()
    private val itemDarkBackgroundPaint = Paint()
    private val itemStrokePaint = Paint()
    private val itemTextPaint = Paint()
    private val itemFillPaint = Paint()
    private val expectedBuffer = Rect()

    init {
        itemLightBackgroundPaint.color = Color.WHITE
        itemLightBackgroundPaint.style = Paint.Style.FILL

        itemDarkBackgroundPaint.color = Color.BLACK
        itemDarkBackgroundPaint.style = Paint.Style.FILL

        itemStrokePaint.color = Color.BLACK
        itemStrokePaint.style = Paint.Style.STROKE
        itemStrokePaint.strokeWidth = map.convertDipToPixels(4f)

        itemTextPaint.color = Color.BLACK
        itemTextPaint.style = Paint.Style.FILL_AND_STROKE
        itemTextPaint.strokeWidth = map.convertDipToPixels(1f)
        itemTextPaint.textSize = map.convertDipToPixels(20f)
        itemTextPaint.textAlign = Paint.Align.CENTER
        itemTextPaint.getTextBounds("00000", 0, 5, this.expectedBuffer)

        itemFillPaint.color = Color.BLACK
        itemFillPaint.style = Paint.Style.FILL
    }

    fun initialize(fileExtent: RectangularArea): Box2d? {
        if ( this.overlayType == OverlayTypes.None ) {
            return null;
        }

        this.fileExtent = fileExtent
        return fileExtent.convertToBox2d()
    }

    override fun dispose() {
        this.isDisposed = true;
    }

    override fun resetRequestedInformationAgeRecord() {
        this.minDataAgeSeconds = null
        this.maxDataAgeSeconds = null
    }

    override fun getRequestedInformationAgeSeconds(): LongRange? {
        val x1 = this.minDataAgeSeconds?: return null
        val x2 = this.maxDataAgeSeconds?: return null
        return x1..x2
    }

    override fun receiveData(location: PointGeo, data: WeatherCondition, dataAgeSeconds: Long, immediate: Boolean, canvas: Canvas?, receiverData: Any?) {
        if ( this.isDisposed ) {
            return
        }

        if ( !immediate || canvas == null || receiverData == null || receiverData !is RenderData) {
            this.map.requestRedraw()
            return
        }

        // Figure out where the canvas should be translated to
        var locationPercentage: Point2d = when (receiverData.region) {
            is RectangularAreaWebMercator -> {
                receiverData.region.positionPercentageUpperLeft(location.convertToPointWebMercator())
            }
            is RectangularAreaLcc -> {
                receiverData.region.positionPercentageUpperLeft(location.convertToPointLcc(receiverData.region.projectionDescription))
            }
            else -> {
                return
            }
        }
        if (locationPercentage.x < 0 || locationPercentage.x > 1 || locationPercentage.y < 0 || locationPercentage.y > 1) {
            //The data element is off screen, skip it
            return
        }

        //Update the data age if needed
        val x1 = this.minDataAgeSeconds
        val x2 = this.maxDataAgeSeconds
        if ( x1 == null || x1 > dataAgeSeconds ) {
            this.minDataAgeSeconds = dataAgeSeconds
        }
        if ( x2 == null || x2 < dataAgeSeconds ) {
            this.maxDataAgeSeconds = dataAgeSeconds
        }

        // Draw the information on the screen
        if ( receiverData.skipDraw ) {
            return
        }
        val drawLocationX = receiverData.destination.upperLeft.x + receiverData.destination.width * locationPercentage.x
        val drawLocationY = receiverData.destination.upperLeft.y + receiverData.destination.height * locationPercentage.y
        val restoreTo = canvas.save()
        canvas.translate(drawLocationX.toFloat(), drawLocationY.toFloat())
        when ( this.overlayType ) {
            OverlayTypes.Ceiling -> this.renderCeiling(data, dataAgeSeconds, canvas)
            OverlayTypes.Category -> this.renderCategory(data, dataAgeSeconds, canvas)
            OverlayTypes.DewPointSpread -> this.renderDewpointSpread(data, dataAgeSeconds, canvas)
            OverlayTypes.Temperature -> this.renderTemperature(data, dataAgeSeconds, canvas)
            OverlayTypes.Visibility -> this.renderVisibility(data, dataAgeSeconds, canvas)
            OverlayTypes.SurfaceWind -> this.renderWind(data, dataAgeSeconds, canvas)
            OverlayTypes.CloudCover -> this.renderCloudCover(data, dataAgeSeconds, canvas)
            else -> Log.error(null, "Request to render unknown type.")
        }
        canvas.restoreToCount(restoreTo)
    }

    private fun getColorForAge(ageSeconds: Long): Int? {
        val minDataAgeSeconds = this.minDataAgeSeconds
        val maxDataAgeSeconds = this.maxDataAgeSeconds
        if (minDataAgeSeconds == null || maxDataAgeSeconds == null || maxDataAgeSeconds - minDataAgeSeconds <= maxNoRangeDisplayDiff) {
            return null
        }
        val percentage = (ageSeconds - minDataAgeSeconds) / (maxDataAgeSeconds - minDataAgeSeconds).toDouble()
        return Color.rgb(
            ceil((newInformationColorRed - (newInformationColorRed - oldInformationColorRed) * percentage)).toInt(),
            ceil((newInformationColorGreen - (newInformationColorGreen - oldInformationColorGreen) * percentage)).toInt(),
            ceil((newInformationColorBlue - (newInformationColorBlue - oldInformationColorBlue) * percentage)).toInt())
        /*
        return Color.argb(
            ceil(255 * percentage).toInt(),
            oldInformationColorRed,
            oldInformationColorGreen,
            oldInformationColorBlue)
         */
    }

    private fun renderCloudCover(data: WeatherCondition, dataAgeSeconds: Long, canvas: Canvas) {
        if ( data.cloudCover == null ) {
            return;
        }

        // Center the coordinates on the location the indicator should be
        val circleRadius = map.convertDipToPixels(20f)
        val ageRadius = map.convertDipToPixels(20f + standardAgeBorderDp)
        var drawIndicator = false
        var drawX = false
        var angle = 0f

        when ( data.cloudCover ) {
            "CLR" -> drawIndicator = true
            "FEW" -> {
                drawIndicator = true
                angle = 90f
            }
            "SCT" -> {
                drawIndicator = true
                angle = 180f
            }
            "BKN" -> {
                drawIndicator = true
                angle = 270f
            }
            "OVC" -> {
                drawIndicator = true
                angle = 360f
            }
            "OVX" -> {
                drawIndicator = true
                drawX = true
            }
        }

        if ( drawIndicator ) {
            val ageColor = getColorForAge(dataAgeSeconds)
            if ( ageColor != null ) {
                val oldColor = itemLightBackgroundPaint.color
                itemLightBackgroundPaint.color = ageColor
                canvas.drawArc(
                    RectF(
                        -ageRadius.toFloat(),
                        -ageRadius.toFloat(),
                        ageRadius.toFloat(),
                        ageRadius.toFloat()
                    ),
                    0F, 360F, true, this.itemLightBackgroundPaint
                )
                itemLightBackgroundPaint.color = oldColor
            }

            canvas.drawArc(
                RectF(-circleRadius.toFloat(), -circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat()),
                0F, 360F, true, this.itemLightBackgroundPaint)

            canvas.drawArc(
                RectF(-circleRadius.toFloat(), -circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat()),
                0F, 360F, true, this.itemStrokePaint)

            if ( drawX ) {
                val offset = (sin(PI / 4.0) * circleRadius).toFloat()
                canvas.drawLine(-offset, -offset, offset, offset, this.itemStrokePaint)
                canvas.drawLine(offset, -offset, -offset, offset, this.itemStrokePaint)
            } else {
                canvas.rotate(-180 / 2F)
                canvas.drawArc(
                    RectF(-circleRadius.toFloat(), -circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat()),
                    0F, angle, true, this.itemFillPaint)
            }
        }
    }

    /**
     * Graphic wind barb key: https://www.weather.gov/hfo/windbarbinfo.  We are not rounding to the nearest 5 here.  We are rounding up.
     */
    private fun renderWind(data: WeatherCondition, dataAgeSeconds: Long, canvas: Canvas) {
        val windSpeed = data.windSpeed
        val windDirection = data.windDirection

        if ( windSpeed == null || windDirection == null ) {
            return;
        }

        // If the wind is variable, draw that.
        val windCircleRadius = 20f
        if ( data.windDirection == "VRB" ) {
            var circleRadius = map.convertDipToPixels(windCircleRadius)

            // Draw age shadow
            val ageColor = getColorForAge(dataAgeSeconds)
            if (ageColor != null) {
                val shadowRadius = map.convertDipToPixels(standardAgeBorderDp) + circleRadius
                val oldColor = this.itemLightBackgroundPaint.color
                this.itemLightBackgroundPaint.color = ageColor
                canvas.drawArc(RectF(-shadowRadius, -shadowRadius, shadowRadius, shadowRadius), 0f, 360f, true, this.itemLightBackgroundPaint)
                this.itemLightBackgroundPaint.color = oldColor
            }

            // Draw the variable circle
            canvas.drawArc(RectF(-circleRadius, -circleRadius, circleRadius, circleRadius), 0f, 360f, true, this.itemDarkBackgroundPaint)
            circleRadius *= 2 / 3f
            canvas.drawArc(RectF(-circleRadius, -circleRadius, circleRadius, circleRadius), 0f, 360f, true, this.itemLightBackgroundPaint)
            circleRadius *= 1 / 2f
            canvas.drawArc(RectF(-circleRadius, -circleRadius, circleRadius, circleRadius), 0f, 360f, true, this.itemDarkBackgroundPaint)
        } else {
            // Otherwise, get the angle and speed of the wind
            val windAngle = windDirection.toFloat()
            var speedToDraw = ceil(windSpeed.toFloat())
            val windGust = data.windGust
            if ( windGust != null ) {
                speedToDraw = ceil(windGust.toFloat())
            }

            if ( speedToDraw > 0 ) {
                val oldColor = this.itemStrokePaint.color

                val ageColor = getColorForAge(dataAgeSeconds)
                if (ageColor != null) {
                    val oldWidth = this.itemStrokePaint.strokeWidth
                    this.itemStrokePaint.strokeWidth = map.convertDipToPixels(standardAgeBorderDp) * 2 + oldWidth
                    this.itemStrokePaint.color = ageColor
                    drawWindBarb(speedToDraw, windAngle, canvas, this.itemStrokePaint)
                    this.itemStrokePaint.strokeWidth = oldWidth
                }

                this.itemStrokePaint.color = Color.WHITE
                drawWindBarb(speedToDraw, windAngle, canvas, this.itemStrokePaint)
                this.itemStrokePaint.color = oldColor
                drawWindBarb(speedToDraw, windAngle, canvas, this.itemDarkBackgroundPaint)
            } else {
                var circleRadius = map.convertDipToPixels(windCircleRadius)

                // Draw age shadow
                val ageColor = getColorForAge(dataAgeSeconds)
                if (ageColor != null) {
                    val shadowRadius = map.convertDipToPixels(standardAgeBorderDp) + circleRadius
                    val oldColor = this.itemLightBackgroundPaint.color
                    this.itemLightBackgroundPaint.color = ageColor
                    canvas.drawArc(RectF(-shadowRadius, -shadowRadius, shadowRadius, shadowRadius), 0f, 360f, true, this.itemLightBackgroundPaint)
                    this.itemLightBackgroundPaint.color = oldColor
                }

                // Draw no wind circle
                canvas.drawArc(RectF(-circleRadius, -circleRadius, circleRadius, circleRadius), 0f, 360f, true, this.itemDarkBackgroundPaint)
                circleRadius *= 2 / 3f
                canvas.drawArc(RectF(-circleRadius, -circleRadius, circleRadius, circleRadius), 0f, 360f, true, this.itemLightBackgroundPaint)
            }
        }
    }

    private fun drawWindBarb(speedToDrawIn: Float, windAngle: Float, canvas: Canvas, paint: Paint) {
        var speedToDraw = speedToDrawIn
        val restoreCount = canvas.save()

        // Figure out the configuration of wind barbs
        var numShort = 0
        var numLong = 0
        var numPenants = 0
        while ( speedToDraw > 45 ) {
            speedToDraw -= 50;
            numPenants++
        }
        while ( speedToDraw > 5 ) {
            speedToDraw -= 10;
            numLong++
        }
        if ( speedToDraw > 0 ) {
            numShort = 1
        }

        // Figure out how tall the wind barb needs to be
        val poleWidth = map.convertDipToPixels(4f)
        val poleBallRadius = poleWidth
        val barbWidth = poleWidth
        val maxBarbLength = map.convertDipToPixels(20f)
        val minBarbLength = maxBarbLength / 2.0
        val penantWidth = maxBarbLength * 2 / 3.0
        val barbAngleRadians = acos((penantWidth / 2.0) / maxBarbLength)
        val penantDepth = sin(barbAngleRadians) * maxBarbLength;
        val minPoleLength = map.convertDipToPixels(20f)
        val minPoleTail = map.convertDipToPixels(6f)

        val indicatorBlankSpaceHeight = barbWidth * (numShort + numLong + numPenants - 1)
        val indicatorHeight = barbWidth * (numShort + numLong) + penantWidth * numPenants
        var poleLength = indicatorBlankSpaceHeight + indicatorHeight + minPoleTail
        if ( poleLength < minPoleLength ) {
            poleLength = minPoleLength.toDouble()
        }

        // Draw the pole
        canvas.rotate(-90f)
        canvas.rotate(windAngle)
        canvas.translate((-poleLength / 2.0).toFloat(), 0f)
        canvas.drawRect(0f, -poleWidth / 2, poleLength.toFloat(), poleWidth, paint)
        canvas.drawArc(RectF(-poleBallRadius, -poleBallRadius, poleBallRadius, poleBallRadius), 0f, 360f, true, paint)

        // If there is only one short barb, draw that at center
        if ( numPenants == 0 && numLong == 0 && numShort == 1 ) {
            canvas.translate((poleLength / 2).toFloat(), 0f)
            canvas.save()
            canvas.rotate((barbAngleRadians * 360 / (2.0 * PI)).toFloat())
            canvas.drawRect(0f, -barbWidth / 2f, minBarbLength.toFloat(), barbWidth, paint)
            canvas.restore()
        } else {
            canvas.translate(poleLength.toFloat(), 0f)

            // Draw each penant
            var penantDrawn = false
            while ( numPenants > 0 ) {
                val path = Path()
                path.moveTo(0f,0f)
                path.lineTo((-penantWidth / 2.0).toFloat(), penantDepth.toFloat())
                path.lineTo(-penantWidth.toFloat(), 0f)
                canvas.drawPath(path, paint)
                canvas.translate(-penantWidth.toFloat(), 0f)

                numPenants--
                penantDrawn = true;
            }
            if ( penantDrawn ) {
                canvas.translate(-barbWidth, 0f)
            }

            // Draw each long barb
            while ( numLong > 0 ) {
                canvas.save()
                canvas.rotate((barbAngleRadians * 360 / (2.0 * PI)).toFloat())
                canvas.drawRect(RectF(0f, 0f, maxBarbLength.toFloat(), barbWidth), paint)
                canvas.restore()
                canvas.translate(-(barbWidth * 2), 0f)

                numLong--
            }

            // Draw the short barb
            if ( numShort > 0 ) {
                canvas.save()
                canvas.rotate((barbAngleRadians * 360 / (2.0 * PI)).toFloat())
                canvas.drawRect(RectF(0f, 0f, minBarbLength.toFloat(), barbWidth), paint)
                canvas.restore()
            }
        }
        canvas.restoreToCount(restoreCount)
    }

    private fun renderCeiling(data: WeatherCondition, dataAgeSeconds: Long, canvas: Canvas) {
        val ceiling = data.ceiling ?: return
        if (ceiling > maxVfrElevation) {
            this.renderBoxText(">${(maxVfrElevation / 100).toString()}", dataAgeSeconds, canvas)
        } else {
            this.renderBoxText((ceiling / 100).toString(), dataAgeSeconds, canvas)
        }
    }

    private fun renderCategory(data: WeatherCondition, dataAgeSeconds: Long, canvas: Canvas) {
        val category = data.flightCategory ?: return
        this.renderBoxText(category, dataAgeSeconds, canvas)
    }

    private fun renderDewpointSpread(data: WeatherCondition, dataAgeSeconds: Long, canvas: Canvas) {
        val spread = data.dewpointSpreadCelcius ?: return
        this.renderBoxText(spread.toString(), dataAgeSeconds, canvas)
    }

    private fun renderTemperature(data: WeatherCondition, dataAgeSeconds: Long, canvas: Canvas) {
        val temp = data.temperatureCelcius ?: return
        this.renderBoxText(temp.toString(), dataAgeSeconds, canvas)
    }

    private fun renderVisibility(data: WeatherCondition, dataAgeSeconds: Long, canvas: Canvas) {
        val visibility = data.visibility ?: return
        this.renderBoxText(visibility.toString(), dataAgeSeconds, canvas)
    }

    private fun renderBoxText(text: String, dataAgeSeconds: Long, canvas: Canvas) {
        val lineHeight = this.expectedBuffer.height()
        val textDimensions = this.itemTextPaint.measureText(text)
        val heightBuffer = map.convertDipToPixels(14f)
        val widthBuffer = map.convertDipToPixels(10f)
        val boxRect = Box2d(-(textDimensions + widthBuffer) / 2.0, -(lineHeight + heightBuffer) / 2.0,
            (textDimensions + widthBuffer) / 2.0, (lineHeight + heightBuffer) / 2.0)

        // Draw the age shadow
        val ageColor = getColorForAge(dataAgeSeconds)
        if (ageColor != null) {
            val ageBorder = map.convertDipToPixels(standardAgeBorderDp)
            val ageRect = Box2d(
                boxRect.upperLeft.x - ageBorder,
                boxRect.upperLeft.y - ageBorder,
                boxRect.lowerRight.x + ageBorder,
                boxRect.lowerRight.y + ageBorder)
            val agePath = createPathForBox(ageRect)
            val oldColor = itemLightBackgroundPaint.color
            itemLightBackgroundPaint.color = ageColor
            canvas.drawPath(agePath, itemLightBackgroundPaint)
            itemLightBackgroundPaint.color = oldColor
        }

        // Draw the box
        val path = createPathForBox(boxRect)
        canvas.drawPath(path, this.itemLightBackgroundPaint)
        canvas.drawPath(path, this.itemStrokePaint)

        // Draw the text
        canvas.drawText(text, 0f, lineHeight / 2f, this.itemTextPaint)
    }

    private fun createPathForBox(boxRect: Box2d): Path {
        val cornerRadius = map.convertDipToPixels(6f)

        val path = Path()
        path.moveTo((boxRect.upperLeft.x + cornerRadius).toFloat(), boxRect.upperLeft.y.toFloat())
        path.lineTo((boxRect.lowerRight.x - cornerRadius).toFloat(), boxRect.upperLeft.y.toFloat())
        path.arcTo(RectF((boxRect.lowerRight.x - 2 * cornerRadius).toFloat(), boxRect.upperLeft.y.toFloat(), boxRect.lowerRight.x.toFloat(), (boxRect.upperLeft.y + 2 * cornerRadius).toFloat()),
            270f, 90f)
        path.lineTo(boxRect.lowerRight.x.toFloat(), (boxRect.lowerRight.y - cornerRadius).toFloat())
        path.arcTo(RectF((boxRect.lowerRight.x - 2 * cornerRadius).toFloat(), (boxRect.lowerRight.y - 2 * cornerRadius).toFloat(), boxRect.lowerRight.x.toFloat(), boxRect.lowerRight.y.toFloat()),
            0f, 90f)
        path.lineTo((boxRect.upperLeft.x + cornerRadius).toFloat(), boxRect.lowerRight.y.toFloat())
        path.arcTo(RectF(boxRect.upperLeft.x.toFloat(), (boxRect.lowerRight.y - 2 * cornerRadius).toFloat(), (boxRect.upperLeft.x + 2 * cornerRadius).toFloat(), boxRect.lowerRight.y.toFloat()),
            90f, 90f)
        path.lineTo(boxRect.upperLeft.x.toFloat(), (boxRect.upperLeft.y + cornerRadius).toFloat())
        path.arcTo(RectF(boxRect.upperLeft.x.toFloat(), boxRect.upperLeft.y.toFloat(), (boxRect.upperLeft.x + 2 * cornerRadius).toFloat(), (boxRect.upperLeft.y + 2 * cornerRadius).toFloat()),
            180f, 90f)

        return path
    }

    override fun render(canvas: Canvas, region: Box2d, destination: Box2d) {
        val fileExtent = this.fileExtent
        if ( this.isDisposed || fileExtent == null ) {
            return
        }

        val extent2d = fileExtent.convertToBox2d()
        val offsetPercentage = extent2d.overlapPercentageUpperLeft(region)
        val rectangularAreaRegion = fileExtent.cropPercentageUpperLeft(offsetPercentage)
        val geoArea = rectangularAreaRegion.getBoundingBoxGeo(RectangularArea.BoundingRules.Outside)

        val longitudeBuffer = geoArea.width * this.expectedBuffer.width() / destination.width
        val latitudeBuffer = longitudeBuffer * 0.6

        this.dataProvider.retrieveTile(geoArea, PointGeo(longitudeBuffer, latitudeBuffer),
            this.overlayType, this, canvas, data=RenderData(rectangularAreaRegion, destination, true))
        this.dataProvider.retrieveTile(geoArea, PointGeo(longitudeBuffer, latitudeBuffer),
            this.overlayType, this, canvas, data=RenderData(rectangularAreaRegion, destination, false))
    }

    override fun moveOffscreen() {
    }

    private data class RenderData(
        val region: RectangularArea,
        val destination: Box2d,
        val skipDraw: Boolean
    )

    companion object {
        val oldInformationColor
            get() = Color.rgb(oldInformationColorRed, oldInformationColorGreen, oldInformationColorBlue)
        val newInformationColor
            get() = Color.rgb(newInformationColorRed, newInformationColorGreen, newInformationColorBlue)
        private const val oldInformationColorRed = 100
        private const val oldInformationColorGreen = 100
        private const val oldInformationColorBlue = 200
        private const val newInformationColorRed = 200
        private const val newInformationColorGreen = 200
        private const val newInformationColorBlue = 255
        private const val standardAgeBorderDp = 6f
        const val maxNoRangeDisplayDiff = 5 * 60    /* 5 minutes */
    }
}
