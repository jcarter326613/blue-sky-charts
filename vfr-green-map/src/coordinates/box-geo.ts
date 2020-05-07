import { PointGeo } from './point-geo'

export class BoxGeo {
    public topLeft: PointGeo;
    public topRight: PointGeo;
    public bottomLeft: PointGeo;
    public bottomRight: PointGeo;

    constructor() {
        this.topLeft = new PointGeo();
        this.topRight = new PointGeo();
        this.bottomLeft = new PointGeo();
        this.bottomRight = new PointGeo();
    }
}