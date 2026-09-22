import { PointWebMercator } from "./point-web-mercator"

/**
 * The point (0,0) is in the lower right
 */
export class BoxWebMercator {
    private topLeft: PointWebMercator;
    private bottomRight: PointWebMercator;

    constructor(topLeftX: number = 0, topLeftY: number = 0, bottomRightX: number = 0, bottomRightY: number = 0) {
        this.topLeft = new PointWebMercator(topLeftX, topLeftY);
        this.bottomRight = new PointWebMercator(bottomRightX, bottomRightY);
    }

    public getTopLeft(): PointWebMercator {
        return this.topLeft;
    }

    public getBottomRight(): PointWebMercator {
        return this.bottomRight;
    }

    public getWidth(): number {
        return this.bottomRight.x - this.topLeft.x
    }

    public getHeight(): number {
        return this.topLeft.y - this.bottomRight.y
    }

    /**
     * Returns the intersection of the two boxes.  An real number range is allowed.
     * @param o 
     */
    public intersection(o: BoxWebMercator): BoxWebMercator | null {
        let newBox = new BoxWebMercator();
        newBox.topLeft.x = Math.max(this.topLeft.x, o.topLeft.x);
        newBox.topLeft.y = Math.min(this.topLeft.y, o.topLeft.y);
        newBox.bottomRight.x = Math.min(this.bottomRight.x, o.bottomRight.x);
        newBox.bottomRight.y = Math.max(this.bottomRight.y, o.bottomRight.y);

        if (newBox.bottomRight.x < newBox.topLeft.x || newBox.bottomRight.y > newBox.topLeft.y) {
            return null;
        }

        return newBox;
    }

}