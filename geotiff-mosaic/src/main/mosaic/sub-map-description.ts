import { BoxWebMercator } from "coordinates"

export class SubMapDescription {
    public name: string
    public extent: BoxWebMercator
    public maxZoom: number
    public tileWidth: number
    public version: string

    constructor(name: string, extent: BoxWebMercator, maxZoom: number, tileWidth: number, version: string) {
        this.name = name
        this.extent = extent
        this.maxZoom = maxZoom
        this.tileWidth = tileWidth
        this.version = version
    }

    public getZoomForResolution(pixelsPerMercator: number): number {
        // Get the zoom level that means we are equal or greater resolution
        let zoom: number;
        if ( this.extent.getHeight() > this.extent.getWidth() ) {
            let extentHeight = this.extent.getHeight()
            zoom = Math.ceil(Math.log2(extentHeight * pixelsPerMercator / this.tileWidth))
        } else {
            let extentWidth = this.extent.getWidth()
            zoom = Math.ceil(Math.log2(extentWidth * pixelsPerMercator / this.tileWidth))
        }

        if ( zoom < 0 ) {
            zoom = 0
        }

        return zoom
    }
}