import { Point2d } from './point-2d'

export class Box2d {
    public upperLeft: Point2d;
    public lowerRight: Point2d;

    constructor(upperLeftX: number = 0, upperLeftY: number = 0, lowerRightX: number = 0, lowerRightY: number = 0) {
        this.upperLeft = new Point2d(upperLeftX, upperLeftY);
        this.lowerRight = new Point2d(lowerRightX, lowerRightY);
    }

    public clone(): Box2d {
        let newRect = new Box2d();
        newRect.upperLeft = this.upperLeft.clone();
        newRect.lowerRight = this.lowerRight.clone();
        return newRect;
    }
}