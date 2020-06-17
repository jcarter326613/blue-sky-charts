package com.blueskycharts.app.coordinates

import kotlin.math.ceil;
import kotlin.math.floor;
import kotlin.math.PI;

/**
 * Our radial coordinates are given with zero pointing along the positive x axis and
 * 90 degrees pointing along the positive y axis.
 */
class PointRadial(private var angle: Double = 0.0, var radius: Double = 0.0) {
    init {
        setAnglePercentage(angle);
    }

    public fun setAngleDegrees(angle: Double) {
        this.setAnglePercentage(angle / 360);
    }

    public fun getAngleDegrees(): Double {
        return this.angle * 360;
    }

    public fun setAnglePercentage(angleIn: Double) {
        var angle = angleIn;
        if (angle < 0) {
            angle += ceil(-angle);
        } else if (angle >=1) {
            angle -= floor(angle);
        }
        this.angle = angle;
    }

    public fun getAnglePercentage(): Double {
        return this.angle;
    }

    public fun setAngleRadians(angle: Double) {
        this.setAnglePercentage(angle / (PI * 2));
    }

    public fun getAngleRadians(): Double {
        return this.angle * PI * 2;
    }
}