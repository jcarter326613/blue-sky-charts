import { Box2d } from '../coordinates/box-2d'
import { Point2d } from '../coordinates/point-2d'
import { SubMapModel } from '../models/sub-map-model'
import { TileProvider } from './tile-provider'
import { TileReceiver } from './tile-receiver';

export class SubMapView implements TileReceiver {
    // Metadata
    private originalMapWidth: number;
    private originalMapHeight: number;
    private tileWidth: number;
    private tileHeight: number;
    private maxZoom: number;
    private mapName: string;
    private mapVersion: string;

    // Rendering
    private tileProvider: TileProvider;
    private renderVersion: number;
    private context: CanvasRenderingContext2D | null;
    private contextTransform: DOMMatrix | null;
    private scaledTileWidth: number;
    private renderRegion: Box2d;    //Delete this.  FOr debugging only
    
    constructor(tileProvider: TileProvider) {
        this.originalMapWidth = 0;
        this.originalMapHeight = 0;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.maxZoom = 0;
        this.mapName = "";
        this.mapVersion = "0";
        this.tileProvider = tileProvider;
        this.renderVersion = 0;
        this.context = null;
        this.contextTransform = null;
        this.scaledTileWidth = 0;
        this.renderRegion = new Box2d();
    }

    public initialize(model: SubMapModel, name: string): boolean {
        if (model.imageHeight == null || model.imageWidth == null || model.tileWidth == null || 
            model.version == null)
            return false;

        this.originalMapWidth = model.imageWidth;
        this.originalMapHeight = model.imageHeight;
        this.tileWidth = model.tileWidth;
        this.tileHeight = model.tileWidth;
        this.mapName = name;
        this.mapVersion = model.version;

        if (this.originalMapWidth > this.originalMapHeight)
            this.tileHeight = Math.round(this.tileWidth * this.originalMapHeight / this.originalMapWidth)
        else
            this.tileWidth = Math.round(this.tileHeight * this.originalMapWidth / this.originalMapHeight)

        return true;
    }

    public getOriginalWidth(): number {
        return this.originalMapWidth;
    }

    public getOriginalHeight(): number {
        return this.originalMapHeight;
    }

    public receiveTile(location: Point2d, tile: HTMLImageElement, data: any): void {
        if ( data as number != this.renderVersion )
            return;

        if ( this.contextTransform == null || this.context == null )
            return;

        let tileX = location.x * this.scaledTileWidth;
        let tileWidth = this.scaledTileWidth;
        let scaledTileHeight = this.scaledTileWidth * this.tileHeight / this.tileWidth
        let tileY = location.y * scaledTileHeight;
        let tileHeight = scaledTileHeight;
        
        let currentTransform = this.context.getTransform();
        this.context.setTransform(this.contextTransform);
        this.context.drawImage(tile, tileX, tileY, tileWidth, tileHeight);
        this.context.beginPath();
        this.context.moveTo(this.renderRegion.upperLeft.x, this.renderRegion.upperLeft.y)
        this.context.lineTo(this.renderRegion.lowerRight.x, this.renderRegion.lowerRight.y)
        this.context.stroke();
        this.context.beginPath();
        this.context.moveTo(this.renderRegion.lowerRight.x, this.renderRegion.upperLeft.y)
        this.context.lineTo(this.renderRegion.upperLeft.x, this.renderRegion.lowerRight.y)
        this.context.stroke();
        this.context.setTransform(currentTransform);
    }

    /**
     * 
     * @param context 
     * @param region The region of the map to draw relative to the original size of the map image
     * @param scale The scale to draw the map at.  Point (0,0) is the center of the map.
     */
    public render(context: CanvasRenderingContext2D, region: Box2d, scale: number) {
        // Validate the region
        region = region.clone();
        if ( region.upperLeft.x < 0 )
            region.upperLeft.x = 0;
        else if ( region.upperLeft.x >= this.originalMapWidth )
            region.upperLeft.x = this.originalMapWidth;
        if ( region.upperLeft.y < 0 )
            region.upperLeft.y = 0;
        else if ( region.upperLeft.y >= this.originalMapHeight )
            region.upperLeft.y = this.originalMapHeight;

        if ( region.lowerRight.x < 0 )
            region.lowerRight.x = 0;
        else if ( region.lowerRight.x >= this.originalMapWidth )
            region.lowerRight.x = this.originalMapWidth;
        if ( region.lowerRight.y < 0 )
            region.lowerRight.y = 0;
        else if ( region.lowerRight.y >= this.originalMapHeight )
            region.lowerRight.y = this.originalMapHeight;

        // Increase the render version to prevent old requests from rendering
        this.renderVersion++
        this.context = context;
        this.contextTransform = context.getTransform();
        this.renderRegion = region.clone();
        this.renderRegion.upperLeft.x *= scale;
        this.renderRegion.upperLeft.y *= scale;
        this.renderRegion.lowerRight.x *= scale;
        this.renderRegion.lowerRight.y *= scale;

        // Figure out the size of what we are drawing
        let m = this.originalMapWidth * scale
        let zoomLevel = Math.ceil(Math.log(m / this.tileWidth) / Math.log(2))
        let numTilesAcross = 2 ** zoomLevel;
        while ( numTilesAcross > this.originalMapWidth && zoomLevel > 0 ) {
            zoomLevel--;
            numTilesAcross = 2 ** zoomLevel;
        }
        this.scaledTileWidth = m / numTilesAcross;
        let originalImageTileWidth = this.originalMapWidth / numTilesAcross
        let originalImageTileHeight = this.originalMapHeight / numTilesAcross

        // Ensure we aren't looping too much
        let startX = Math.floor(region.upperLeft.x / originalImageTileWidth);
        let endX = Math.ceil(region.lowerRight.x / originalImageTileWidth);
        let startY = Math.floor(region.upperLeft.y / originalImageTileHeight);
        let endY = Math.ceil(region.lowerRight.y / originalImageTileHeight)

        if (endX - startX > 20 || endY - startY > 20)
            return;

        // Get all the images to draw
        for (let x = startX; x < endX; x++ ) {
            for (let y = startY; y < endY; y++ ) {
                // Get the tile image and request it be drawn
                this.tileProvider.retrieveTile(this.mapName, this.mapVersion, zoomLevel, new Point2d(x,y), 
                    this, this.renderVersion);
            }
        }
    }
}
