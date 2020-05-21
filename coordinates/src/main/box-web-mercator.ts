import { PointWebMercator } from "./point-web-mercator"

export class BoxWebMercator {
    private topLeft: PointWebMercator;
    private bottomRight: PointWebMercator;

    constructor(topLeftX: number, topLeftY: number, bottomRightX: number, bottomRightY: number) {
        this.topLeft = new PointWebMercator(topLeftX, topLeftY);
        this.bottomRight = new PointWebMercator(bottomRightX, bottomRightY);
    }

    public getTopLeft(): PointWebMercator {
        return this.topLeft;
    }

    public getBottomRight(): PointWebMercator {
        return this.bottomRight;
    }
}