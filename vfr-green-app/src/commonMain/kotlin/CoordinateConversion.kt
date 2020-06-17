import PointWebMercatorStatic.MAX_Y_MERCATOR
import kotlin.js.JsName
import kotlin.math.*

object CoordinateConversion {
    /**
     * Converts 2d points to radial points.  A radial point with angle 0 and radius 1 is along the
     * positive y axis on the 2d point and positivity goes towards the positive x axis.
     * @param pointRadial
     */
    @JsName("convertPointRadialToPoint2d")
    fun convertPointRadialToPoint2d(pointRadial: PointRadial): Point2d {
        val retVal = Point2d();
        retVal.y = cos(pointRadial.getAngleRadians()) * pointRadial.radius;
        retVal.x = sin(pointRadial.getAngleRadians()) * pointRadial.radius;
        return retVal;
    }

    /**
     * Converts 2d points to radial points.  A radial point with angle 0 and radius 1 is along the
     * positive y axis on the 2d point.
     * @param pointRadial
     */
    @JsName("convertPoint2dToPointRadial")
    fun convertPoint2dToPointRadial(point2d: Point2d): PointRadial {
        val retVal = PointRadial();
        var radians = atan(point2d.x / point2d.y);
        if (point2d.y < 0)
            radians += PI
        retVal.setAngleRadians(radians);
        retVal.radius = sqrt(point2d.x.pow(2) + point2d.y.pow(2));
        return retVal;
    }

    /**
     * Returns a 2 dimensional point with respect to an observer at the origin with the negative y axis
     * pointing to the radial point (0,0).
     * @param point The point to convert to 2d
     * @param origin The location of the observer
     */
    @JsName("getRelativePoint2d")
    fun getRelativePoint2d(point: PointRadial, origin: PointRadial): Point2d {
        var angleDiff = point.getAngleRadians() - origin.getAngleRadians();
        val retVal = Point2d();

        // Get the divide by zero cases
        when (angleDiff) {
            0.0 -> {
                retVal.x = 0.0;
                retVal.y = point.radius - origin.radius;
                return retVal;
            }
            PI -> {
                retVal.x = 0.0;
                retVal.y = -(point.radius + origin.radius);
                return retVal;
            }
            (PI / 2) -> {
                retVal.x = -point.radius;
                retVal.y = -origin.radius;
            }
            (3 * PI / 2) -> {
                retVal.x = point.radius;
                retVal.y = -origin.radius;
            }
        }

        // Calculate the typical case
        if (angleDiff > PI) {
            angleDiff = -(1 - angleDiff)
        }

        retVal.x = point.radius * sin(angleDiff)
        retVal.y = (point.radius * cos(angleDiff)) - origin.radius

        return retVal;
    }

    /**
     * Converts the given longitude and latitude to a Web Mercator projection where the upper left is (0,0) and the lower right is (256, 256)
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     * @param geoPoint
     */
    @JsName("convertToWebMercator")
    fun convertToWebMercator(geoPoint: PointGeo): PointWebMercator {
        val newPoint = PointWebMercator();

        val longitude = geoPoint.longitude * 2 * PI / 360
        newPoint.x = (256 / (2 * PI)) * (longitude + PI)

        when {
            geoPoint.latitude > 89 -> {
                newPoint.y = 0.0;
            }
            geoPoint.latitude < -89 -> {
                newPoint.y = PointWebMercatorStatic.MAX_Y_MERCATOR.toDouble();
            }
            else -> {
                val latitude = geoPoint.latitude * 2 * PI / 360
                newPoint.y = (256 / (2 * PI)) * (PI - ln(tan((PI / 4) + (latitude / 2))))
            }
        }

        if (newPoint.x < 0) {
            newPoint.x = 0.0;
        } else if (newPoint.x > PointWebMercatorStatic.MAX_X_MERCATOR) {
            newPoint.x = PointWebMercatorStatic.MAX_X_MERCATOR.toDouble();
        }

        if (newPoint.y < 0) {
            newPoint.y = 0.0;
        } else if (newPoint.y > PointWebMercatorStatic.MAX_Y_MERCATOR) {
            newPoint.y = PointWebMercatorStatic.MAX_Y_MERCATOR.toDouble();
        }

        return newPoint;
    }

    /**
     * Converts the given x,y coordinates to latitude and longitude.  Min x and y are (0,0) and max is (256, 256).
     * @param point2d
     */
    @JsName("convertFromWebMercator")
    fun convertFromWebMercator(point2d: PointWebMercator): PointGeo {
        val newPoint = PointGeo();

        newPoint.longitude = point2d.x * 2 * PI / 256 - PI;
        newPoint.latitude = 2 * atan(exp(PI - ((point2d.y * 2 * PI) / 256))) - PI / 2;

        newPoint.longitude = newPoint.longitude * 360 / (2 * PI)
        newPoint.latitude = newPoint.latitude * 360 / (2 * PI)

        return newPoint;
    }

    @JsName("convertBoxMercatorToBoxGeo")
    fun convertBoxMercatorToBoxGeo(boxMercator: BoxWebMercator): BoxGeo {
        val boxGeo = BoxGeo(
            convertFromWebMercator(boxMercator.topLeft),
            convertFromWebMercator(boxMercator.bottomRight)
        );
        return boxGeo;
    }

    /**
     * Assumes the 2d box is actually web mercator coordinates
     * @param boxMercator
     */
    @JsName("convertBox2dToBoxGeo")
    fun convertBox2dToBoxGeo(box2d: Box2d): BoxGeo {
        val ul = box2d.upperLeft;
        val lr = box2d.lowerRight;
        val boxMercator = BoxWebMercator(ul.x, ul.y, lr.x, lr.y);
        return convertBoxMercatorToBoxGeo(boxMercator);
    }

    @JsName("convertBoxGeoToBoxMercator")
    fun convertBoxGeoToBoxMercator(boxGeo: BoxGeo): BoxWebMercator {
        val topLeft = convertToWebMercator(boxGeo.topLeft);
        val bottomRight =
            convertToWebMercator(boxGeo.bottomRight);
        val boxMercator = BoxWebMercator(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
        return boxMercator;
    }
}