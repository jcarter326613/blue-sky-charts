package com.blueskycharts.app.coordinates

import kotlin.math.max;
import kotlin.math.min;

class BoxGeo(topLeft: PointGeo = PointGeo(), bottomRight: PointGeo = PointGeo(),
             topRight: PointGeo? = null, bottomLeft: PointGeo? = null) {
    val width: Double
        get() = this.bottomRight.longitude - this.topLeft.longitude
    val height: Double
        get() = this.topLeft.latitude - this.bottomRight.latitude

    var topLeft: PointGeo = topLeft
        set(value) {
            field = value;
            this.topRight.latitude = value.latitude;
            this.bottomLeft.longitude = value.longitude;
        }
    var topRight: PointGeo
        private set;
    var bottomLeft: PointGeo
        private set;
    var bottomRight: PointGeo = bottomRight
        set(value) {
            field = value;
            this.topRight.longitude = value.longitude;
            this.bottomLeft.latitude = value.latitude;
        }

    init {
        if ( topRight == null ) {
            this.topRight = PointGeo(bottomRight.longitude, topLeft.latitude);
        } else {
            this.topRight = topRight;
        }

        if ( bottomLeft == null ) {
            this.bottomLeft = PointGeo(topLeft.longitude, bottomRight.latitude);
        } else {
            this.bottomLeft = bottomLeft;
        }
    }

    /**
     * Returns the intersection of two boxes.  There is an assumption that this box and the other box are both non wrapping.
     * topLeft Longitude < bottomRight Longitude
     * @param o
     */
    /*
    fun intersection(o: BoxGeo): BoxGeo? {
        val upperLeft = PointGeo();
        val lowerRight = PointGeo();
        upperLeft.latitude = min(this.topLeft.latitude, o.topLeft.latitude)
        upperLeft.longitude = max(this.topLeft.longitude, o.topLeft.longitude)
        lowerRight.latitude = max(this.bottomRight.latitude, o.bottomRight.latitude)
        lowerRight.longitude = min(this.bottomRight.longitude, o.bottomRight.longitude)

        if (upperLeft.latitude < lowerRight.latitude || upperLeft.longitude > lowerRight.longitude) {
            return null;
        }

        return BoxGeo(upperLeft, lowerRight);
    }
     */
}