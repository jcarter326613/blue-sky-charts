import { Point2d } from './point-2d'
import { PointRadial } from './point-radial'

export class CoordinateConverstion {
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
}