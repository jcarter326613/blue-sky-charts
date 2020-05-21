import { Point2d } from "coordinates";
import { BoxGeoModel } from "./box-geo-model";

export class SubMapModel {
    public mapBounds: Array<Point2d> | null;
    public tileWidth: number | null;
    public version: string | null;
    public imageWidth: number | null;
    public imageHeight: number | null;
    public fileExtent: BoxGeoModel | null;
    public maxZoom: number | null;

    constructor() {
        this.mapBounds = null;
        this.tileWidth = null;
        this.version = null;
        this.fileExtent = null;
        this.imageWidth = null;
        this.imageHeight = null;
        this.maxZoom = null;
    }
};
