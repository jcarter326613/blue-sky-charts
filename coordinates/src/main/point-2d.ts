export class Point2d {
    public x: number;
    public y: number;

    constructor(x: number = 0, y: number = 0) {
        this.x = x;
        this.y = y;
    }

    public clone(): Point2d {
        let retVal = new Point2d();
        retVal.x = this.x;
        retVal.y = this.y;
        return retVal;
    }

    public calculateDistance(o: Point2d): number {
        return Math.sqrt((this.x - o.x) ** 2 + (this.y - o.y) ** 2)
    }
}