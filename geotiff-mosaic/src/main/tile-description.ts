import { Box2d, BoxWebMercator, PointWebMercator, Point2d, CoordinateConversion } from "coordinates";
import { SubMapDescription } from './sub-map-description'
import { TileCache } from "./tile-cache";

export class TileDescription {
    public static readonly TILE_DIMENSIONS_PIXELS = 1024

    public tileCache: TileCache
    public tileExtent: BoxWebMercator
    public widthPixels: number = 0
    public heightPixels: number = 0
    public zoom: number
    public x: number
    public y: number

    private overlaps: Array<SubMapDescription> | undefined
    private subMapNames: string[] | undefined
    private subMapTileDimensions: Array<Point2d> | undefined
    private subMapExtents: BoxWebMercator[] | undefined
    private subMapImagePaths: Array<string[]> | undefined
    private subMapImageExtents: Array<Array<BoxWebMercator>> | undefined

    constructor(tileExtent: BoxWebMercator, tileCache: TileCache, zoom: number, x: number, y: number) {
        this.tileCache = tileCache
        this.tileExtent = tileExtent
        this.zoom = zoom
        this.x = x
        this.y = y

        this.widthPixels = TileDescription.TILE_DIMENSIONS_PIXELS
        this.heightPixels = TileDescription.TILE_DIMENSIONS_PIXELS
        if ( tileExtent.getHeight() > tileExtent.getWidth() ) {
            this.widthPixels = this.widthPixels * tileExtent.getWidth() / tileExtent.getHeight()
        } else {
            this.heightPixels = this.heightPixels * tileExtent.getHeight() / tileExtent.getWidth()
        }
    }

    public setSubTileOverlaps(overlaps: Array<SubMapDescription>): void {
        this.overlaps = overlaps
        this.subMapNames = []
        this.subMapExtents = []
        this.subMapTileDimensions = []
        for ( let map of overlaps ) {
            this.subMapNames.push(map.name)
            this.subMapExtents.push(map.extent)

            let width = map.tileWidth
            let height = map.tileWidth
            if ( map.extent.getHeight() > map.extent.getWidth() ) {
                width = width * map.extent.getWidth() / map.extent.getHeight()
            } else {
                height = height * map.extent.getHeight() / map.extent.getWidth()
            }
            this.subMapTileDimensions.push(new Point2d(width, height))
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
            retList.push(this.convertMercatorToRelative2d(subMap, this.tileExtent, new Point2d(this.widthPixels, this.heightPixels)))
        }
        return retList
    }

    private convertMercatorToRelative2d(mercator: BoxWebMercator, relativeToMercator: BoxWebMercator, relativeToPixels: Point2d): Box2d {
        let pixels = CoordinateConversion.convertBoxMercatorToBox2d(mercator)
        let relativeTo = CoordinateConversion.convertBoxMercatorToBox2d(relativeToMercator)

        let startX = relativeToPixels.x * ((pixels.getUpperLeft().x - relativeTo.getUpperLeft().x) / relativeTo.getWidth())
        let endX = relativeToPixels.x * ((pixels.getLowerRight().x - relativeTo.getUpperLeft().x) / relativeTo.getWidth())
        let startY = relativeToPixels.y * ((pixels.getUpperLeft().y - relativeTo.getUpperLeft().y) / relativeTo.getHeight())
        let endY = relativeToPixels.y * ((pixels.getLowerRight().y - relativeTo.getUpperLeft().y) / relativeTo.getHeight())

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
                pixelsPerMercator = TileDescription.TILE_DIMENSIONS_PIXELS / targetDimensionExtent
            } else {
                targetDimensionExtent = this.tileExtent.getWidth()
                pixelsPerMercator = TileDescription.TILE_DIMENSIONS_PIXELS / targetDimensionExtent
            }

            // Get the zoom for the submap
            let subMapDescription = this.overlaps[subMapNumber]
            let targetZoom = subMapDescription.getZoomForResolution(pixelsPerMercator)
            let maxZoom = subMapDescription.maxZoom
            if ( maxZoom < targetZoom ) {
                targetZoom = maxZoom
            }

            // For the given zoom, get the needed tiles
            let extentPerTile = new PointWebMercator( 
                subMapDescription.extent.getWidth() / (2 ** targetZoom),
                subMapDescription.extent.getHeight() / (2 ** targetZoom))
            let extentOverlap = subMapDescription.extent.intersection(this.tileExtent)
            if ( extentOverlap === undefined || extentOverlap === null ) {
                this.subMapImagePaths.push([])
                this.subMapImageExtents.push([])
                continue
            }
            let startX = Math.floor((extentOverlap.getTopLeft().x - subMapDescription.extent.getTopLeft().x) / extentPerTile.x)
            let endX = Math.ceil((extentOverlap.getBottomRight().x - subMapDescription.extent.getTopLeft().x) / extentPerTile.x) - 1
            let startY = Math.floor((subMapDescription.extent.getTopLeft().y - extentOverlap.getTopLeft().y) / extentPerTile.y)
            let endY = Math.ceil((subMapDescription.extent.getTopLeft().y - extentOverlap.getBottomRight().y) / extentPerTile.y) - 1

            // Generate the actual paths and extents
            let imagePathList: string[] = []
            let imageExtentList: BoxWebMercator[] = []
            for ( let x = startX; x <= endX; x++ ) {
                for ( let y = startY; y <= endY; y++ ) {
                    let path = this.tileCache.getPathForTile(subMapDescription, targetZoom, x, y)
                    imagePathList.push(path)
                    imageExtentList.push(new BoxWebMercator(
                        subMapDescription.extent.getTopLeft().x + extentPerTile.x * x, 
                        subMapDescription.extent.getTopLeft().y - extentPerTile.y * y,
                        subMapDescription.extent.getTopLeft().x + extentPerTile.x * (x+1),
                        subMapDescription.extent.getTopLeft().y - extentPerTile.y * (y+1)))
                }
            }

            this.subMapImagePaths.push(imagePathList)
            this.subMapImageExtents.push(imageExtentList)
        }
    }
}