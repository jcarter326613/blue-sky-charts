import { PointGeo } from './point-geo';

export class BoxGeo {
    private topLeft: PointGeo;
    private topRight: PointGeo;
    private bottomLeft: PointGeo;
    private bottomRight: PointGeo;

    constructor(topLeft: PointGeo = new PointGeo(), bottomRight: PointGeo = new PointGeo(), 
        topRight: PointGeo | null = null, bottomLeft: PointGeo | null = null) {

        this.topLeft = topLeft;
        this.bottomRight = bottomRight;

        if ( topRight == null ) {
            this.topRight = new PointGeo(bottomRight.longitude, topLeft.latitude);
        } else {
            this.topRight = topRight;
        }

        if ( bottomLeft == null ) {
            this.bottomLeft = new PointGeo(topLeft.longitude, bottomRight.latitude);
        } else {
            this.bottomLeft = bottomLeft;
        }
    }

    /**
     * Returns the intersection of two boxes.  There is an assumption that this box and the other box are both non wrapping.
     * topLeft Longitude < bottomRight Longitude
     * @param o 
     */
    public intersection(o: BoxGeo): BoxGeo | null {
        let upperLeft = new PointGeo();
        let lowerRight = new PointGeo();
        upperLeft.latitude = Math.min(this.topLeft.latitude, o.topLeft.latitude)
        upperLeft.longitude = Math.max(this.topLeft.longitude, o.topLeft.longitude)
        lowerRight.latitude = Math.max(this.bottomRight.latitude, o.bottomRight.latitude)
        lowerRight.longitude = Math.min(this.bottomRight.longitude, o.bottomRight.longitude)

        if (upperLeft.latitude < lowerRight.latitude || upperLeft.longitude > lowerRight.longitude) {
            return null;
        }

        return new BoxGeo(upperLeft, lowerRight);
    }

    public setTopLeft(tp: PointGeo): void {
        this.topLeft = tp;
        this.topRight.latitude = tp.latitude;
        this.bottomLeft.longitude = tp.longitude;
    }

    public getTopLeft(): PointGeo {
        return this.topLeft;
    }

    public setBottomRight(br: PointGeo): void {
        this.bottomRight = br;
        this.topRight.longitude = br.longitude;
        this.bottomLeft.latitude = br.latitude;
    }

    public getBottomRight(): PointGeo {
        return this.bottomRight;
    }

    public getTopRight(): PointGeo {
        return this.topRight;
    }

    public getBottomLeft(): PointGeo {
        return this.bottomLeft;
    }

    public getDimensions(): PointGeo {
        return new PointGeo(this.bottomRight.longitude - this.topLeft.longitude,
            this.topLeft.latitude - this.bottomRight.latitude);
    }
}