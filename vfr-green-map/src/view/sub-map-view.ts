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
        
        let currentTransform = this.context.getTransform();
        this.context.setTransform(this.contextTransform);
        this.context.drawImage(tile, this.originalMapWidth / 2, this.originalMapHeight / 2);
        this.context.setTransform(currentTransform);

        //console.log(`Tile received (${location.x}, ${location.y}.  Version ${data as number}.  src ${tile.src})`);
    }

    /**
     * 
     * @param context 
     * @param region The region of the map to draw relative to the original size of the map image
     * @param scale The scale to draw the map at.  Point (0,0) is the center of the map.
     */
    public render(context: CanvasRenderingContext2D, region: Box2d, scale: number) {
        // Validate the region
        if ( region.upperLeft.x < 0 )
            region.upperLeft.x = 0;
        else if ( region.upperLeft.x >= this.originalMapWidth )
            region.upperLeft.x = this.originalMapWidth;
        if ( region.upperLeft.y < 0 )
            region.upperLeft.y = 0;
        else if ( region.upperLeft.y >= this.originalMapHeight )
            region.upperLeft.y = this.originalMapHeight;

        // Increase the render version to prevent old requests from rendering
        this.renderVersion++
        this.context = context;
        this.contextTransform = context.getTransform();

        // Figure out the size of what we are drawing
        let m = this.originalMapWidth * scale
        let zoomLevel = Math.ceil(Math.log(m / this.tileWidth) / Math.log(2))
        let numTilesAcross = 2 ** zoomLevel;
        while ( numTilesAcross > this.originalMapWidth && zoomLevel > 0 ) {
            zoomLevel--;
            numTilesAcross = 2 ** zoomLevel;
        }
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

        /*
        context.fillStyle = "rgb(200, 0, 0)"
        context.fillRect(0, 0, this.originalMapWidth / 2, this.originalMapHeight / 2);
        context.fillStyle = "rgb(0, 0, 0)"
        context.fillRect(this.originalMapWidth / 2 - 25, this.originalMapHeight / 2 - 25, 50, 50);        
        */
    }
}
