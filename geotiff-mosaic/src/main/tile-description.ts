import { BoxWebMercator } from "coordinates";

export class TileDescription {
    public tileExtent: BoxWebMercator
    public pieces: Array<[string, BoxWebMercator, BoxWebMercator]> = []  // [map name, map extents, desired portion]
    public widthPixels: number = 0
    public heightPixels: number = 0

    constructor(tileExtent: BoxWebMercator) {
        this.tileExtent = tileExtent
    }
}