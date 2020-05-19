import { BoxGeo, PointGeo } from 'coordinates';

export class BoxGeoModel {
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

    public static createBoxGeoFromModel(model: BoxGeoModel): BoxGeo {
        return new BoxGeo(model.topLeft, model.bottomRight, model.topRight, model.bottomLeft);
    }
};
