import { Point2d } from './point-2d'
import { PointRadial } from './point-radial'

export class CoordinateConverstion {
    public static convertPointRadialToPoint2d(pointRadial: PointRadial): Point2d {
        let retVal = new Point2d();
        retVal.x = Math.cos(pointRadial.getAngleRadians()) * pointRadial.getRadius();
        retVal.y = Math.sin(pointRadial.getAngleRadians()) * pointRadial.getRadius();
        return retVal;
    }

    public static convertPoint2dToPointRadial(point2d: Point2d): PointRadial {
        let retVal = new PointRadial();
        let radians = Math.atan(point2d.y / point2d.x);
        if (point2d.x < 0)
            radians += Math.PI
        retVal.setAngleRadians(radians);
        retVal.setRadius(Math.sqrt(point2d.x ** 2 + point2d.y ** 2));
        return retVal;
    }
}