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
    private var dataAgeSeconds: Long? = null
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
        this.dataAgeSeconds = null
    }

    override fun getRequestedInformationAgeSeconds(): Long? {
        return this.dataAgeSeconds;
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
        val drawLocationX = receiverData.destination.upperLeft.x + receiverData.destination.width * locationPercentage.x
        val drawLocationY = receiverData.destination.upperLeft.y + receiverData.destination.height * locationPercentage.y

        // Draw the information on the screen
        val restoreTo = canvas.save()
        canvas.translate(drawLocationX.toFloat(), drawLocationY.toFloat())
        when ( this.overlayType ) {
            OverlayTypes.Ceiling -> this.renderCeiling(data, canvas)
            OverlayTypes.Category -> this.renderCategory(data, canvas)
            OverlayTypes.DewPointSpread -> this.renderDewpointSpread(data, canvas)
            OverlayTypes.Temperature -> this.renderTemperature(data, canvas)
            OverlayTypes.Visibility -> this.renderVisibility(data, canvas)
            OverlayTypes.SurfaceWind -> this.renderWind(data, canvas)
            OverlayTypes.CloudCover -> this.renderCloudCover(data, canvas)
            else -> Log.e(null, "Request to render unknown type.")
        }
        canvas.restoreToCount(restoreTo)

        //Update the data age if needed
        val das = this.dataAgeSeconds
        if ( das == null || das < dataAgeSeconds ) {
            this.dataAgeSeconds = dataAgeSeconds
        }
    }

    private fun renderCloudCover(data: WeatherCondition, canvas: Canvas) {
        if ( data.cloudCover == null ) {
            return;
        }

        // Center the coordinates on the location the indicator should be
        val circleRadius = map.convertDipToPixels(20f)
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
    private fun renderWind(data: WeatherCondition, canvas: Canvas) {
        val windSpeed = data.windSpeed
        val windDirection = data.windDirection

        if ( windSpeed == null || windDirection == null ) {
            return;
        }

        // If the wind is variable, draw that.
        val windCircleRadius = 20f
        if ( data.windDirection == "VRB" ) {
            var circleRadius = map.convertDipToPixels(windCircleRadius)
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
                canvas.drawRect(0f, -poleWidth / 2, poleLength.toFloat(), poleWidth, this.itemDarkBackgroundPaint)
                canvas.drawArc(RectF(-poleBallRadius, -poleBallRadius, poleBallRadius, poleBallRadius), 0f, 360f, true, this.itemDarkBackgroundPaint)

                // If there is only one short barb, draw that at center
                if ( numPenants == 0 && numLong == 0 && numShort == 1 ) {
                    canvas.translate((poleLength / 2).toFloat(), 0f)
                    canvas.save()
                    canvas.rotate((barbAngleRadians * 360 / (2.0 * PI)).toFloat())
                    canvas.drawRect(0f, -barbWidth / 2f, minBarbLength.toFloat(), barbWidth, this.itemDarkBackgroundPaint)
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
                        canvas.drawPath(path, this.itemDarkBackgroundPaint)
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
                        canvas.drawRect(RectF(0f, 0f, maxBarbLength.toFloat(), barbWidth), this.itemDarkBackgroundPaint)
                        canvas.restore()
                        canvas.translate(-(barbWidth * 2), 0f)

                        numLong--
                    }

                    // Draw the short barb
                    if ( numShort > 0 ) {
                        canvas.save()
                        canvas.rotate((barbAngleRadians * 360 / (2.0 * PI)).toFloat())
                        canvas.drawRect(RectF(0f, 0f, minBarbLength.toFloat(), barbWidth), this.itemDarkBackgroundPaint)
                        canvas.restore()
                    }
                }
            } else {
                // Draw no wind circle
                var circleRadius = map.convertDipToPixels(windCircleRadius)
                canvas.drawArc(RectF(-circleRadius, -circleRadius, circleRadius, circleRadius), 0f, 360f, true, this.itemDarkBackgroundPaint)
                circleRadius *= 2 / 3f
                canvas.drawArc(RectF(-circleRadius, -circleRadius, circleRadius, circleRadius), 0f, 360f, true, this.itemLightBackgroundPaint)
            }
        }
    }

    private fun renderCeiling(data: WeatherCondition, canvas: Canvas) {
        val ceiling = data.ceiling ?: return
        if (ceiling > maxVfrElevation) {
            this.renderBoxText(">${(maxVfrElevation / 100).toString()}", canvas)
        } else {
            this.renderBoxText((ceiling / 100).toString(), canvas)
        }
    }

    private fun renderCategory(data: WeatherCondition, canvas: Canvas) {
        val category = data.flightCategory ?: return
        this.renderBoxText(category, canvas)
    }

    private fun renderDewpointSpread(data: WeatherCondition, canvas: Canvas) {
        val spread = data.dewpointSpreadCelcius ?: return
        this.renderBoxText(spread.toString(), canvas)
    }

    private fun renderTemperature(data: WeatherCondition, canvas: Canvas) {
        val temp = data.temperatureCelcius ?: return
        this.renderBoxText(temp.toString(), canvas)
    }

    private fun renderVisibility(data: WeatherCondition, canvas: Canvas) {
        val visibility = data.visibility ?: return
        this.renderBoxText(visibility.toString(), canvas)
    }

    private fun renderBoxText(text: String, canvas: Canvas) {
        val lineHeight = this.expectedBuffer.height()
        val textDimensions = this.itemTextPaint.measureText(text)
        val heightBuffer = map.convertDipToPixels(14f)
        val widthBuffer = map.convertDipToPixels(10f)
        val cornerRadius = map.convertDipToPixels(6f)
        //val textRect = Box2d(-textDimensions / 2.0, -lineHeight / 2.0, textDimensions / 2.0, lineHeight / 2.0)
        val boxRect = Box2d(-(textDimensions + widthBuffer) / 2.0, -(lineHeight + heightBuffer) / 2.0,
            (textDimensions + widthBuffer) / 2.0, (lineHeight + heightBuffer) / 2.0)
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
        canvas.drawPath(path, this.itemLightBackgroundPaint)
        canvas.drawPath(path, this.itemStrokePaint)

        canvas.drawText(text, 0f, lineHeight / 2f, this.itemTextPaint)
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
            this.overlayType, this, canvas, data=RenderData(rectangularAreaRegion, destination))
    }

    override fun moveOffscreen() {
    }

    private data class RenderData(
        val region: RectangularArea,
        val destination: Box2d
    )
}
