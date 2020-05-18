
/**
 * Our radial coordinates are given with zero pointing along the positive x axis and 
 * 90 degrees pointing along the positive y axis.
 */
export class PointRadial {
    private radius: number;
    private angle: number;

    constructor(angle: number = 0, radius: number = 0) {
        this.radius = 0;
        this.angle = 0;

        this.setAnglePercentage(angle);
        this.setRadius(radius);
    }

    public clone(): PointRadial {
        let retVal = new PointRadial();
        retVal.radius = this.radius;
        retVal.angle = this.angle;
        return retVal;
    }

    public setAngleDegrees(angle: number): void {
        this.setAnglePercentage(angle / 360);
    }

    public getAngleDegrees(): number {
        return this.angle * 360;
    }

    public setAnglePercentage(angle: number): void {
        if (angle < 0) {
            angle += Math.ceil(-angle);
        } else if (angle >=1) {
            angle -= Math.floor(angle);
        }
        this.angle = angle;
    }

    public getAnglePercentage(): number {
        return this.angle;
    }

    public setAngleRadians(angle: number): void {
        this.setAnglePercentage(angle / (Math.PI * 2));
    }

    public getAngleRadians(): number {
        return this.angle * Math.PI * 2;
    }

    public setRadius(radius: number): void {
        this.radius = radius;
    }

    public getRadius(): number {
        return this.radius;
    }
}