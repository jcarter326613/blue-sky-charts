package com.blueskycharts.app.map.view

import android.graphics.*
import android.util.Log
import com.blueskycharts.app.coordinates.*
import com.blueskycharts.app.map.models.SubMapModel
import com.blueskycharts.app.map.models.WeatherCondition
import com.blueskycharts.app.map.models.WeatherConditionResponse
import com.blueskycharts.app.map.resources.DataProvider
import com.blueskycharts.app.map.resources.DataReceiver
import kotlin.math.PI
import kotlin.math.sin

class MapDataView(private val dataProvider: DataProvider, private val overlayType: OverlayTypes, private val map: Map) : SubMapView, DataReceiver {
    // Metadata
    private var dataAgeSeconds: Int? = null
    override val originalWidth: Int = PointWebMercator.MAX_X_MERCATOR
    override val originalHeight: Int = PointWebMercator.MAX_Y_MERCATOR

    // Rendering
    private var contextScale: Double? = null
    private var isDisposed: Boolean = false

    override fun initialize(model: SubMapModel?): BoxWebMercator? {
        if ( this.overlayType == OverlayTypes.None ) {
            return null;
        }

        return BoxWebMercator(0.0, 0.0, PointWebMercator.MAX_X_MERCATOR.toDouble(), PointWebMercator.MAX_Y_MERCATOR.toDouble())
    }

    override fun dispose() {
        this.isDisposed = true;
    }

    override fun resetRequestedInformationAgeRecord() {
        this.dataAgeSeconds = null
    }

    override fun getRequestedInformationAgeSeconds(): Int? {
        return this.dataAgeSeconds;
    }

    override fun receiveData(location: PointWebMercator, data: WeatherCondition, dataAgeSeconds: Int, immediate: Boolean, canvas: Canvas?) {
        val contextScale = this.contextScale
        if ( this.isDisposed || contextScale == null ) {
            return;
        }

        if ( !immediate || canvas == null ) {
            this.map.requestRedraw();
            return;
        }

        val dataAgeSeconds = this.dataAgeSeconds
        if ( dataAgeSeconds == null || dataAgeSeconds < dataAgeSeconds ) {
            this.dataAgeSeconds = dataAgeSeconds
        }

        val restoreTo = canvas.save();
        canvas.translate((location.x * contextScale).toFloat(), (location.y * contextScale).toFloat())
        Log.d("circleTest", "receiveData translate(${(location.x * contextScale).toFloat()}, ${(location.y * contextScale).toFloat()})")
        when ( this.overlayType ) {
            OverlayTypes.Ceiling -> this.renderCeiling(location, data, canvas)
            OverlayTypes.Category -> this.renderCategory(location, data, canvas)
            OverlayTypes.DewPointSpread -> this.renderDewpointSpread(location, data, canvas)
            OverlayTypes.Temperature -> this.renderTemperature(location, data, canvas)
            OverlayTypes.Visibility -> this.renderVisibility(location, data, canvas)
            OverlayTypes.SurfaceWind -> this.renderWind(location, data, canvas)
            OverlayTypes.CloudCover -> this.renderCloudCover(location, data, canvas)
            else -> Log.e(null, "Request to render unknown type.")
        }
        canvas.restoreToCount(restoreTo);
    }

    private fun renderCloudCover(location: PointWebMercator, data: WeatherCondition, canvas: Canvas) {
        if ( data.cloudCover == null || this.contextScale == null ) {
            return;
        }

        // Center the coordinates on the location the indicator should be
        val circleRadius = 100       //TODO convert this to something that's dependent on pixel density
        val strokeLineWidth = 4
        var drawIndicator = false
        var drawX = false
        var angle = 0.0

        when ( data.cloudCover ) {
            "CLR" -> drawIndicator = true
            "FEW" -> {
                drawIndicator = true
                angle = PI / 2.0
            }
            "SCT" -> {
                drawIndicator = true
                angle = PI
            }
            "BKN" -> {
                drawIndicator = true
                angle = 3 * PI / 2.0
            }
            "OVC" -> {
                drawIndicator = true
                angle = 2 * Math.PI
            }
            "OVX" -> {
                drawIndicator = true
                drawX = true
            }
        }

        if ( drawIndicator ) {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            paint.strokeWidth = strokeLineWidth.toFloat()
            canvas.drawArc(
                RectF(-circleRadius.toFloat(), -circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat()),
                0F, 360F, true, paint)

            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawArc(
                RectF(-circleRadius.toFloat(), -circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat()),
                0F, 360F, true, paint)

            /*
            if ( drawX ) {
                val offset = sin(PI / 4.0) * circleRadius
                canvas.beginPath()
                canvas.moveTo(-offset, -offset)
                canvas.lineTo(offset, offset)
                canvas.moveTo(offset, -offset)
                canvas.lineTo(-offset, offset)
                canvas.stroke()
            } else {
                canvas.rotate(-180 / 2F)
                canvas.fillStyle = "rgb(0,0,0)"
                canvas.beginPath()
                canvas.arc(0, 0, circleRadius, 0, angle)
                canvas.lineTo(0,0)
                canvas.fill()
            }
             */
        }
    }

    /**
     * Graphic wind barb key: https://www.weather.gov/hfo/windbarbinfo.  We are not rounding to the nearest 5 here.  We are rounding up.
     */
    private fun renderWind(location: PointWebMercator, data: WeatherCondition, canvas: Canvas) {
        /*
        if ( data.windSpeed === undefined || data.windDirection === undefined ||
            this.context === undefined || this.contextScale === undefined ) {
            return;
        }

        // If the wind is variable, draw that.
        this.context.lineWidth = 1;
        if ( data.windDirection == "VRB" ) {
            let circleRadius = 20;
            this.context.strokeStyle = "rgb(0,0,0)";
            this.context.fillStyle = "rgb(0,0,0)";
            this.context.beginPath();
            this.context.arc(0, 0, circleRadius, 0, 2 * Math.PI);
            this.context.fill();
            this.context.strokeStyle = "rgb(255,255,255)";
            this.context.fillStyle = "rgb(255,255,255)";
            this.context.beginPath();
            this.context.arc(0, 0, circleRadius * 2 / 3, 0, 2 * Math.PI);
            this.context.fill();
            this.context.strokeStyle = "rgb(0,0,0)";
            this.context.fillStyle = "rgb(0,0,0)";
            this.context.beginPath();
            this.context.arc(0, 0, circleRadius * 1 / 3, 0, 2 * Math.PI);
            this.context.fill();
        } else {
            // Otherwise, get the angle and speed of the wind
            let windAngle = Math.round(parseFloat(data.windDirection));
            let speedToDraw = Math.ceil(parseFloat(data.windSpeed));
            if ( data.windGust !== undefined ) {
                speedToDraw = Math.ceil(parseFloat(data.windGust));
            }

            if ( speedToDraw != 0 ) {
                // Figure out the configuration of wind barbs
                let numShort = 0;
                let numLong = 0;
                let numPenants = 0;
                while ( speedToDraw > 45 ) {
                    speedToDraw -= 50;
                    numPenants++;
                }
                while ( speedToDraw > 5 ) {
                    speedToDraw -= 10;
                    numLong++;
                }
                if ( speedToDraw > 0 ) {
                    numShort = 1;
                }

                // Figure out how tall the wind barb needs to be
                let poleWidth = 4;
                let poleBallRadius = poleWidth;
                let barbWidth = poleWidth;
                let maxBarbLength = 20;
                let minBarbLength = maxBarbLength / 2;
                let penantWidth = maxBarbLength * 2 / 3;
                let barbAngleRadians = Math.acos((penantWidth / 2) / maxBarbLength)
                let penantDepth = Math.sin(barbAngleRadians) * maxBarbLength;
                let minPoleLength = 20;
                let minPoleTail = 6;

                let indicatorBlankSpaceHeight = barbWidth * (numShort + numLong + numPenants - 1)
                let indicatorHeight = barbWidth * (numShort + numLong) + penantWidth * numPenants;
                let poleLength = indicatorBlankSpaceHeight + indicatorHeight + minPoleTail
                if ( poleLength < minPoleLength ) {
                    poleLength = minPoleLength
                }

                // Draw the pole
                this.context.rotate(-Math.PI / 2);
                this.context.rotate(windAngle * 2 * Math.PI / 360);
                this.context.translate(-poleLength / 2, 0);
                this.context.fillRect(0, -poleWidth / 2, poleLength, poleWidth);
                this.context.beginPath();
                this.context.moveTo(0,0);
                this.context.arc(0, 0, poleBallRadius, 0, Math.PI * 2);
                this.context.fill();

                // If there is only one short barb, draw that at center
                if ( numPenants == 0 && numLong == 0 && numShort == 1 ) {
                    this.context.translate(poleLength / 2, 0);
                    this.context.save();
                    this.context.rotate(barbAngleRadians);
                    this.context.fillRect(0, -barbWidth / 2, minBarbLength, barbWidth);
                    this.context.restore();
                } else {
                    this.context.translate(poleLength, 0);

                    // Draw each penant
                    let penantDrawn = false;
                    while ( numPenants > 0 ) {
                        this.context.beginPath()
                        this.context.moveTo(0,0);
                        this.context.lineTo(-penantWidth / 2, penantDepth);
                        this.context.lineTo(-penantWidth, 0);
                        this.context.fill();
                        this.context.translate(-penantWidth, 0);

                        numPenants--;
                        penantDrawn = true;
                    }
                    if ( penantDrawn ) {
                        this.context.translate(-barbWidth, 0);
                    }

                    // Draw each long barb
                    while ( numLong > 0 ) {
                        this.context.save();
                        this.context.rotate(barbAngleRadians);
                        this.context.fillRect(0, 0, maxBarbLength, barbWidth);
                        this.context.restore();
                        this.context.translate(-(barbWidth * 2), 0);

                        numLong--;
                    }

                    // Draw the short barb
                    if ( numShort > 0 ) {
                        this.context.save();
                        this.context.rotate(barbAngleRadians);
                        this.context.fillRect(0, 0, minBarbLength, barbWidth);
                        this.context.restore();
                    }
                }
            } else {
                // Draw no wind circle
                let circleRadius = 50;
                this.context.strokeStyle = "rgb(0,0,0)";
                this.context.fillStyle = "rgb(0,0,0)";
                this.context.beginPath();
                this.context.arc(0, 0, circleRadius, 0, 2 * Math.PI);
                this.context.stroke();
                this.context.fill();
                this.context.strokeStyle = "rgb(255,255,255)";
                this.context.fillStyle = "rgb(255,255,255)";
                this.context.beginPath();
                this.context.arc(0, 0, circleRadius * 2 / 3, 0, 2 * Math.PI);
                this.context.stroke();
                this.context.fill();
            }
        }
         */
    }

    private fun renderCeiling(location: PointWebMercator, data: WeatherCondition, canvas: Canvas) {
        /*
        if ( data.ceiling === undefined ) {
            return;
        }

        this.renderBoxText(location, (parseInt(data.ceiling) / 100).toString());
         */
    }

    private fun renderCategory(location: PointWebMercator, data: WeatherCondition, canvas: Canvas) {
        /*
        if ( data.flightCategory === undefined ) {
            return;
        }

        this.renderBoxText(location, data.flightCategory);

         */
    }

    private fun renderDewpointSpread(location: PointWebMercator, data: WeatherCondition, canvas: Canvas) {
        /*
        if ( data.dewpointSpreadCelcius === undefined ) {
            return;
        }

        this.renderBoxText(location, data.dewpointSpreadCelcius.toString());

         */
    }

    private fun renderTemperature(location: PointWebMercator, data: WeatherCondition, canvas: Canvas) {
        /*
        if ( data.temperatureCelcius === undefined ) {
            return;
        }

        this.renderBoxText(location, data.temperatureCelcius.toString());

         */
    }

    private fun renderVisibility(location: PointWebMercator, data: WeatherCondition, canvas: Canvas) {
        /*
        if ( data.visibility === undefined ) {
            return;
        }

        this.renderBoxText(location, data.visibility.toString());

         */
    }

    private fun renderBoxText(location: PointWebMercator, text: String) {
        /*
        if ( this.context === undefined || this.contextScale === undefined ) {
            return;
        }
        let oldFont = this.context.font;
        this.context.font = "20px Arial";
        let lineHeight = this.context.measureText('M').width * 1.2;
        let textDimensions = this.context.measureText(text);
        let heightBuffer = 10;
        let widthBuffer = 6;
        let cornerRadius = 3;
        let textRect = new Box2d(-textDimensions.width / 2, -lineHeight / 2, textDimensions.width / 2, lineHeight / 2);
        let boxRect = new Box2d(-textDimensions.width / 2 - widthBuffer / 2, -lineHeight / 2 - heightBuffer / 2,
            textDimensions.width / 2 + widthBuffer / 2, lineHeight / 2 + heightBuffer / 10)
        this.context.lineWidth = 1;
        this.context.strokeStyle = "rgb(0,0,0)";
        this.context.fillStyle = "rgb(255,255,255)";
        this.context.beginPath();
        this.context.moveTo(boxRect.getUpperLeft().x + cornerRadius, boxRect.getUpperLeft().y);
        this.context.lineTo(boxRect.getLowerRight().x - cornerRadius, boxRect.getUpperLeft().y);
        this.context.arc(boxRect.getLowerRight().x - cornerRadius, boxRect.getUpperLeft().y + cornerRadius, cornerRadius,
            -Math.PI / 2, 0);
        this.context.lineTo(boxRect.getLowerRight().x, boxRect.getLowerRight().y - cornerRadius);
        this.context.arc(boxRect.getLowerRight().x - cornerRadius, boxRect.getLowerRight().y - cornerRadius, cornerRadius,
            0, Math.PI / 2);
        this.context.lineTo(boxRect.getUpperLeft().x + cornerRadius, boxRect.getLowerRight().y);
        this.context.arc(boxRect.getUpperLeft().x + cornerRadius, boxRect.getLowerRight().y - cornerRadius, cornerRadius,
            Math.PI / 2, Math.PI);
        this.context.lineTo(boxRect.getUpperLeft().x, boxRect.getUpperLeft().y + cornerRadius);
        this.context.arc(boxRect.getUpperLeft().x + cornerRadius, boxRect.getUpperLeft().y + cornerRadius, cornerRadius,
            Math.PI, 3 * Math.PI / 2);
        this.context.fill();
        this.context.stroke();

        this.context.fillStyle = "rgb(0,0,0)";
        let oldAlign = this.context.textAlign;
        this.context.textAlign = "center";
        this.context.fillText(text, 0, textRect.getLowerRight().y - 5);
        this.context.font = oldFont;
        this.context.textAlign = oldAlign;

         */
    }

    override fun render(canvas: Canvas, region: Box2d, scale: Double) {
        if ( this.isDisposed ) {
            return;
        }
        this.contextScale = scale;

        val pixelsAcross = region.getDimensions().x * scale;
        val longitudeAcross = 360 * region.getDimensions().x / this.originalWidth;

        val paint = Paint()
        val bounds = Rect()
        paint.textSize = 20F
        paint.getTextBounds("00000", 0, 5, bounds)
        val longitudeBuffer = longitudeAcross * bounds.width() / pixelsAcross
        val latitudeBuffer = longitudeBuffer * 0.6

        this.dataProvider.retrieveTile(CoordinateConversion.convertBox2dToBoxGeo(region), PointGeo(longitudeBuffer, latitudeBuffer),
            this.overlayType, this, canvas)
    }

    override fun moveOffscreen() {
    }
}
