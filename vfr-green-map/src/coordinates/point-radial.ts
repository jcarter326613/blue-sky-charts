export class PointRadial {
    private radius: number;
    private angle: number;

    constructor(angle: number = 0, radius: number = 0) {
        this.radius = 0;
        this.angle = 0;
        
        this.setAnglePercentage(angle);
        this.setRadius(radius);
    }

    public setAnglePercentage(angle: number): void {
        if (angle < 0) {
            angle += Math.ceil(-angle);
        } else if (angle >=1) {
            angle -= Math.floor(angle);
        }
        this.angle = angle;
    }

    public getAngle(): number {
        return this.angle;
    }

    public setRadius(radius: number): void {
        this.radius = radius;
    }

    public getRadius(): number {
        return this.radius;
    }
}