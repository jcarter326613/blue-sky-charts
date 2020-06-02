import { PointWebMercator } from "./point-web-mercator"

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

    /**
     * Returns the union of the two boxes.  An real number range is allowed.
     * @param o 
     */
    public union(o: BoxWebMercator): BoxWebMercator | null {
        let newBox = new BoxWebMercator();
        newBox.topLeft.x = Math.max(this.topLeft.x, o.topLeft.x);
        newBox.topLeft.y = Math.max(this.topLeft.y, o.topLeft.y);
        newBox.bottomRight.x = Math.min(this.bottomRight.x, o.bottomRight.x);
        newBox.bottomRight.y = Math.min(this.bottomRight.y, o.bottomRight.y);

        if (newBox.bottomRight.x < newBox.topLeft.x || newBox.bottomRight.y < newBox.topLeft.y) {
            return null;
        }

        return newBox;
    }

}