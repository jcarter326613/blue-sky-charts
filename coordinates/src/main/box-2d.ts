import { Point2d } from './point-2d'

/**
 * Display coordinates.  Upper left is (0,0)
 */
export class Box2d {
    private upperLeft: Point2d;
    private lowerRight: Point2d;

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

    /**
     * Returns the intersection of the two boxes.  An real number range is allowed.
     * @param o 
     */
    public intersection(o: Box2d): Box2d | null {
        let newBox = new Box2d();
        newBox.upperLeft.x = Math.max(this.upperLeft.x, o.upperLeft.x);
        newBox.upperLeft.y = Math.max(this.upperLeft.y, o.upperLeft.y);
        newBox.lowerRight.x = Math.min(this.lowerRight.x, o.lowerRight.x);
        newBox.lowerRight.y = Math.min(this.lowerRight.y, o.lowerRight.y);

        if (newBox.lowerRight.x < newBox.upperLeft.x || newBox.lowerRight.y < newBox.upperLeft.y) {
            return null;
        }

        return newBox;
    }

    public getUpperLeft(): Point2d {
        return this.upperLeft;
    }

    public getLowerRight(): Point2d {
        return this.lowerRight;
    }

    public getWidth(): number {
        return this.lowerRight.x - this.upperLeft.x
    }

    public getHeight(): number {
        return this.lowerRight.y - this.upperLeft.y
    }

    public getDimensions(): Point2d {
        return new Point2d(this.getWidth(), this.getHeight());
    }
}