import { BoxWebMercator } from "coordinates"

export class SubMapDescription {
    public name: string
    public extent: BoxWebMercator
    public maxZoom: number
    public tileWidth: number

    constructor(name: string, extent: BoxWebMercator, maxZoom: number, tileWidth: number) {
        this.name = name
        this.extent = extent
        this.maxZoom = maxZoom
        this.tileWidth = tileWidth
    }

    public getZoomForResolution(pixelsPerMercator: number): number {
        // Get the zoom level that means we are equal or greater resolution
        if ( this.extent.getHeight() > this.extent.getWidth() ) {
            let extentHeight = this.extent.getHeight()
            return Math.ceil(Math.log2(extentHeight * pixelsPerMercator / this.tileWidth))
        } else {
            let extentWidth = this.extent.getWidth()
            return Math.ceil(Math.log2(extentWidth * pixelsPerMercator / this.tileWidth))
        }
    }
}