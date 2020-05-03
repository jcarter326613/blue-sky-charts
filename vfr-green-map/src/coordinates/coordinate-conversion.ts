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
}