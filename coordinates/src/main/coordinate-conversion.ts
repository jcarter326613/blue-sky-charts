import { BoxGeo } from './box-geo'
import { BoxWebMercator } from './box-web-mercator'
import { Point2d } from './point-2d'
import { PointGeo } from './point-geo'
import { PointRadial } from './point-radial'
import { PointWebMercator } from './point-web-mercator'

export class CoordinateConversion {
    /**
     * Converts 2d points to radial points.  A radial point with angle 0 and radius 1 is along the 
     * positive y axis on the 2d point and positivity goes towards the positive x axis.
     * @param pointRadial 
     */
    public static convertPointRadialToPoint2d(pointRadial: PointRadial): Point2d {
        let retVal = new Point2d();
        retVal.y = Math.cos(pointRadial.getAngleRadians()) * pointRadial.getRadius();
        retVal.x = Math.sin(pointRadial.getAngleRadians()) * pointRadial.getRadius();
        return retVal;
    }

    /**
     * Converts 2d points to radial points.  A radial point with angle 0 and radius 1 is along the 
     * positive y axis on the 2d point.
     * @param pointRadial 
     */
    public static convertPoint2dToPointRadial(point2d: Point2d): PointRadial {
        let retVal = new PointRadial();
        let radians = Math.atan(point2d.x / point2d.y);
        if (point2d.y < 0)
            radians += Math.PI
        retVal.setAngleRadians(radians);
        retVal.setRadius(Math.sqrt(point2d.x ** 2 + point2d.y ** 2));
        return retVal;
    }

    /**
     * Returns a 2 dimensional point with respect to an observer at the origin with the negative y axis
     * pointing to the radial point (0,0).
     * @param point The point to convert to 2d
     * @param origin The location of the observer
     */
    public static getRelativePoint2d(point: PointRadial, origin: PointRadial): Point2d {
        let angleDiff = point.getAngleRadians() - origin.getAngleRadians();
        let retVal = new Point2d();

        // Get the divide by zero cases
        if (angleDiff == 0) {
            retVal.x = 0;
            retVal.y = point.getRadius() - origin.getRadius();
            return retVal;
        } else if (angleDiff == Math.PI) {
            retVal.x = 0;
            retVal.y = -(point.getRadius() + origin.getRadius());
            return retVal;
        } else if (angleDiff == Math.PI / 2) {
            retVal.x = -point.getRadius();
            retVal.y = -origin.getRadius();
        } else if (angleDiff == 3 * Math.PI / 2) {
            retVal.x = point.getRadius();
            retVal.y = -origin.getRadius();
        }
 
        // Calculate the typical case
        if (angleDiff > Math.PI) {
            angleDiff = -(1 - angleDiff)
        }

        retVal.x = point.getRadius() * Math.sin(angleDiff)
        retVal.y = (point.getRadius() * Math.cos(angleDiff)) - origin.getRadius()

        return retVal;
    }
 
    /**
     * Converts the given longitude and latitude to a Web Mercator projection where the upper left is (0,0) and the lower right is (256, 256)
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     * @param geoPoint 
     */
    public static convertToWebMercator(geoPoint: PointGeo): PointWebMercator {
        let newPoint = new PointWebMercator();

        let longitude = geoPoint.longitude * 2 * Math.PI / 360
        let latitude = geoPoint.latitude * 2 * Math.PI / 360

        newPoint.x = (256 / (2 * Math.PI)) * (longitude + Math.PI)
        newPoint.y = (256 / (2 * Math.PI)) * (Math.PI - Math.log(Math.tan((Math.PI / 4) + (latitude / 2))))

        return newPoint;
    }

    /**
     * Converts the given x,y coordinates to latitude and longitude.  Min x and y are (0,0) and max is (256, 256).
     * @param point2d 
     */
    public static convertFromWebMercator(point2d: PointWebMercator): PointGeo {
        let newPoint = new PointGeo();

        newPoint.longitude = point2d.x * 2 * Math.PI / 256 - Math.PI;
        newPoint.latitude = 2 * Math.atan(Math.exp(Math.PI - ((point2d.y * 2 * Math.PI) / 256))) - Math.PI / 2;

        newPoint.longitude = newPoint.longitude * 360 / (2 * Math.PI)
        newPoint.latitude = newPoint.latitude * 360 / (2 * Math.PI)

        return newPoint;
    }

    public static convertBoxMercatorToBoxGeo(boxMercator: BoxWebMercator): BoxGeo {
        let boxGeo = new BoxGeo(CoordinateConversion.convertFromWebMercator(boxMercator.getTopLeft()),
            CoordinateConversion.convertFromWebMercator(boxMercator.getBottomRight()));
        return boxGeo;
    }
}
