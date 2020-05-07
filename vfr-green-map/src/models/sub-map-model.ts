import { Point2d } from "coordinates/point-2d";
import { BoxGeo } from "coordinates/box-geo";

export class SubMapModel {
    public mapBounds: Array<Point2d> | null;
    public tileWidth: number | null;
    public version: string | null;
    public fileExtent: BoxGeo | null;

    constructor() {
        this.mapBounds = null;
        this.tileWidth = null;
        this.version = null;
        this.fileExtent = null;
    }
};