import { Point2d } from './point-2d'
import { PointRadial } from './point-radial'

export class CoordinateConverstion {
    public static convertPointRadialToPoint2d(pointRadial: PointRadial, observer: PointRadial): Point2d {
        let relativeAngle: number = pointRadial.getAngle() - observer.getAngle();
        let relativeY: number = 0;

        let retVal: Point2d = new Point2d();
        return retVal;
    }
}