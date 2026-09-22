import { Point2d } from "coordinates";
import { BoxGeoModel } from "./box-geo-model";

export class SubMapModel {
    public mapBounds: Array<Point2d> | undefined;
    public tileWidth: number | undefined;
    public version: string | undefined;
    public imageWidth: number | undefined;
    public imageHeight: number | undefined;
    public imageWidthScale: number | undefined;
    public imageHeightScale: number | undefined;
    public fileExtent: BoxGeoModel | undefined;
    public maxZoom: number | undefined;

    constructor() {
    }
};
