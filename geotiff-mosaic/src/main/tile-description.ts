import { Box2d, BoxWebMercator, PointWebMercator } from "coordinates";
import { SubMapDescription } from './sub-map-description'
import { TileCache } from "./tile-cache";
import { exit } from "process";

export class TileDescription {
    private TILE_DIMENSIONS_PIXELS = 1024

    public tileCache: TileCache
    public tileExtent: BoxWebMercator
    public pieces: Array<[string, BoxWebMercator, BoxWebMercator]> = []  // [map name, map extents, desired portion]
    public widthPixels: number = 0
    public heightPixels: number = 0
    public zoom: number
    public x: number
    public y: number

    private overlaps: Array<SubMapDescription> | undefined
    private subMapNames: string[] | undefined
    private subMapExtents: BoxWebMercator[] | undefined
    private subMapImagePaths: Array<string[]> | undefined
    private subMapImageExtents: Array<Array<BoxWebMercator>> | undefined

    constructor(tileExtent: BoxWebMercator, tileCache: TileCache, zoom: number, x: number, y: number) {
        this.tileCache = tileCache
        this.tileExtent = tileExtent
        this.zoom = zoom
        this.x = x
        this.y = y
    }

    public setSubTileOverlaps(overlaps: Array<SubMapDescription>): void {
        this.overlaps = overlaps
        this.subMapNames = []
        this.subMapExtents = []
        for ( let map of overlaps ) {
            this.subMapNames.push(map.name)
            this.subMapExtents.push(map.extent)
        }

        this.prepareSubMapData()
    }
    
    public getDependencyList(): string[] {
        if ( this.subMapNames === undefined ) {
            return []
        }
        return this.subMapNames
    }

    public getSubTileDestinations2d(subMapNumber: number): Array<Box2d> {
        if ( this.subMapImageExtents === undefined ) {
            throw new Error("this.subMapImageExtents undefined")
        }

        let thisSubMapList = this.subMapImageExtents[subMapNumber]
        let retList: Array<Box2d> = []
        for ( let subMap of thisSubMapList ) {
            retList.push(this.convertMercatorToRelative2d(subMap, this.tileExtent))
        }
        return retList
    }

    public getSubTileSources2d(subMapNumber: number): Array<Box2d> {
        if ( this.subMapImageExtents === undefined || this.subMapExtents === undefined ) {
            throw new Error("this.subMapImageExtents or subMapExtents undefined")
        }
        
        let thisSubMapList = this.subMapImageExtents[subMapNumber]
        let retList: Array<Box2d> = []
        for ( let subMap of thisSubMapList ) {
            retList.push(this.convertMercatorToRelative2d(subMap, this.subMapExtents[subMapNumber]))
        }
        return retList
    }

    private convertMercatorToRelative2d(mercator: BoxWebMercator, relativeTo: BoxWebMercator): Box2d {
        let startX = this.widthPixels * ((relativeTo.getTopLeft().x - this.tileExtent.getTopLeft().x) / this.tileExtent.getWidth())
        let endX = this.widthPixels * ((relativeTo.getBottomRight().x - this.tileExtent.getTopLeft().x) / this.tileExtent.getWidth())
        let startY = this.heightPixels * ((relativeTo.getTopLeft().x - this.tileExtent.getTopLeft().x) / this.tileExtent.getWidth())
        let endY = this.heightPixels * ((relativeTo.getBottomRight().x - this.tileExtent.getTopLeft().x) / this.tileExtent.getWidth())

        return new Box2d(startX, startY, endX, endY)
    }

    public getNumSubMaps(): number {
        if ( this.subMapNames === undefined ) {
            return 0
        }
        return this.subMapNames.length
    }

    public getImagePaths(subMapNumber: number): string[] {
        if ( this.subMapImagePaths === undefined ) {
            return []
        }
        return this.subMapImagePaths[subMapNumber]
    }

    private prepareSubMapData(): void {
        if ( this.overlaps === undefined ) {
            return
        }

        this.subMapImagePaths = []
        this.subMapImageExtents = []

        for ( let subMapNumber = 0; subMapNumber < this.getNumSubMaps(); subMapNumber++ ) {
            // Get the pixel resolution needed for the submap
            let pixelsPerMercator: number
            let targetDimensionExtent: number
            if ( this.tileExtent.getHeight() > this.tileExtent.getWidth() ) {
                targetDimensionExtent = this.tileExtent.getHeight()
                pixelsPerMercator = this.TILE_DIMENSIONS_PIXELS / targetDimensionExtent
            } else {
                targetDimensionExtent = this.tileExtent.getWidth()
                pixelsPerMercator = this.TILE_DIMENSIONS_PIXELS / targetDimensionExtent
            }

            // Get the zoom for the submap
            let subMapDescription = this.overlaps[subMapNumber]
            let targetZoom = subMapDescription.getZoomForResolution(pixelsPerMercator)
            let maxZoom = subMapDescription.maxZoom
            if ( maxZoom < targetZoom ) {
                targetZoom = maxZoom
            }

            // For the given zoom, get the needed tiles
            let extentPerZoom = new PointWebMercator( 
                subMapDescription.extent.getWidth() / (2 ** targetZoom),
                subMapDescription.extent.getHeight() / (2 ** targetZoom))
            let extentOverlap = subMapDescription.extent.union(this.tileExtent)
            if ( extentOverlap === undefined || extentOverlap === null ) {
                this.subMapImagePaths.push([])
                this.subMapImageExtents.push([])
                continue
            }
            let startX = Math.floor((extentOverlap.getTopLeft().x - subMapDescription.extent.getTopLeft().x) / extentPerZoom.x)
            let endX = Math.ceil((extentOverlap.getBottomRight().x - subMapDescription.extent.getTopLeft().x) / extentPerZoom.x) - 1
            let startY = Math.floor((extentOverlap.getTopLeft().y - subMapDescription.extent.getTopLeft().y) / extentPerZoom.y)
            let endY = Math.ceil((extentOverlap.getBottomRight().y - subMapDescription.extent.getTopLeft().y) / extentPerZoom.y) - 1

            // Generate the actual paths and extents
            let imagePathList: string[] = []
            let imageExtentList: BoxWebMercator[] = []
            for ( let x = startX; x <= endX; x++ ) {
                for ( let y = startY; y <= endY; y++ ) {
                    let path = this.tileCache.getPathForTile(subMapDescription, targetZoom, x, y)
                    imagePathList.push(path)
                    imageExtentList.push(new BoxWebMercator(
                        subMapDescription.extent.getTopLeft().x + extentPerZoom.x * x, 
                        subMapDescription.extent.getTopLeft().y + extentPerZoom.y * y,
                        subMapDescription.extent.getTopLeft().x + extentPerZoom.x * (x+1),
                        subMapDescription.extent.getTopLeft().y + extentPerZoom.y * (y+1)))
                }
            }

            this.subMapImagePaths.push(imagePathList)
            this.subMapImageExtents.push(imageExtentList)
        }
    }
}