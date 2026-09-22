import { BoxGeoModel } from '../models/box-geo-model'
import { Box2d, BoxWebMercator, CoordinateConversion, Point2d } from 'coordinates'
import { ISubMapView } from './i-sub-map-view'
import { SubMapModel } from '../models/sub-map-model'
import { TileProvider } from '../resources/tile-provider'
import { ITileReceiver } from '../resources/i-tile-receiver';
import { IMap } from './i-map'

export class MapTileView implements ISubMapView, ITileReceiver {
    // Metadata
    private map: IMap;
    private originalMapWidth: number;
    private originalMapHeight: number;
    private imageWidthScale: number;
    private imageHeightScale: number;
    private tileWidth: number;
    private tileHeight: number;
    private tileDimensionPercentage: Point2d;
    private mapName: string;
    private mapVersion: string;
    private maxZoom: number;

    // Rendering
    private tileProvider: TileProvider;
    private renderVersion: number;
    private context: CanvasRenderingContext2D | null;
    private contextTransform: DOMMatrix | null;
    private scaledTileWidth: number;
    private isDisposed: boolean;
    
    constructor(tileProvider: TileProvider, map: IMap) {
        this.map = map;
        this.originalMapWidth = 0;
        this.originalMapHeight = 0;
        this.imageWidthScale = 1;
        this.imageHeightScale = 1;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.tileDimensionPercentage = new Point2d();
        this.mapName = "";
        this.mapVersion = "0";
        this.maxZoom = 0;
        this.tileProvider = tileProvider;
        this.renderVersion = 0;
        this.context = null;
        this.contextTransform = null;
        this.scaledTileWidth = 0;
        this.isDisposed = false;
    }

    public initialize(model: SubMapModel, name: string): BoxWebMercator | undefined {
        if (model.imageHeight == null || model.imageWidth == null || model.tileWidth == null || 
            model.version == null || model.fileExtent == null || model.maxZoom == null)
            return undefined;

        if ( model.imageWidthScale !== undefined ) {
            this.imageWidthScale = model.imageWidthScale;
        }
        if ( model.imageHeightScale !== undefined ) {
            this.imageHeightScale = model.imageHeightScale;
        }

        this.originalMapWidth = model.imageWidth;
        this.originalMapHeight = model.imageHeight;
        this.tileWidth = model.tileWidth;
        this.tileHeight = model.tileWidth;
        this.tileDimensionPercentage = new Point2d(1, 1);
        this.mapName = name;
        this.mapVersion = model.version;
        this.maxZoom = model.maxZoom;

        if (this.originalMapWidth > this.originalMapHeight)
            this.tileHeight = Math.round(this.tileWidth * this.originalMapHeight / this.originalMapWidth)
        else
            this.tileWidth = Math.round(this.tileHeight * this.originalMapWidth / this.originalMapHeight)

        let fileExtent = BoxGeoModel.createBoxGeoFromModel(model.fileExtent);
        let fileExtent2dUpperLeft = CoordinateConversion.convertToWebMercator(fileExtent.getTopLeft());
        let fileExtent2dLowerRight = CoordinateConversion.convertToWebMercator(fileExtent.getBottomRight());
        let fileExtent2d = new BoxWebMercator(fileExtent2dUpperLeft.x, fileExtent2dUpperLeft.y, fileExtent2dLowerRight.x, fileExtent2dLowerRight.y);
        return fileExtent2d
    }

    public dispose(): void {
        this.isDisposed = true;
    }

    public getOriginalWidth(): number {
        return this.originalMapWidth;
    }

    public getOriginalHeight(): number {
        return this.originalMapHeight;
    }

    public resetRequestedInformationAgeRecord(): void {
    }

    public getRequestedInformationAgeSeconds(): number | undefined {
        return undefined;
    }

    public receiveTile(location: Point2d, subsection: Box2d, tile: HTMLImageElement, data: any, immediate: boolean): void {
        if ( this.isDisposed || data as number != this.renderVersion )
            return;

        if ( !immediate ) {
            this.map.requestRedraw();
            return;
        }

        if ( this.contextTransform == null || this.context == null )
            return;

        let tileX = location.x * this.scaledTileWidth * this.imageWidthScale;
        let tileWidth = this.scaledTileWidth * this.imageWidthScale;
        let scaledTileHeight = this.scaledTileWidth * this.tileHeight / this.tileWidth
        let tileY = location.y * scaledTileHeight * this.imageHeightScale;
        let tileHeight = scaledTileHeight * this.imageHeightScale;
        
        let currentTransform = this.context.getTransform();
        this.context.setTransform(this.contextTransform);
        let upperLeft = subsection.getUpperLeft();
        let dimensions = subsection.getDimensions();
        this.context.drawImage(tile, upperLeft.x * tile.width, upperLeft.y * tile.height, dimensions.x * tile.width, dimensions.y * tile.height,
            tileX, tileY, tileWidth, tileHeight);
        this.context.setTransform(currentTransform);
    }

    /**
     * 
     * @param context 
     * @param region The region of the map to draw relative to the original size of the map image
     * @param scale The scale to draw the map at.  Point (0,0) is the center of the map.
     */
    public render(context: CanvasRenderingContext2D, region: Box2d, scale: number) {
        if (this.isDisposed) {
            return;
        }

        // Validate the region
        region = region.clone();
        if ( region.getUpperLeft().x < 0 )
            region.getUpperLeft().x = 0;
        else if ( region.getUpperLeft().x >= this.originalMapWidth )
            region.getUpperLeft().x = this.originalMapWidth;
        if ( region.getUpperLeft().y < 0 )
            region.getUpperLeft().y = 0;
        else if ( region.getUpperLeft().y >= this.originalMapHeight )
            region.getUpperLeft().y = this.originalMapHeight;

        if ( region.getLowerRight().x < 0 )
            region.getLowerRight().x = 0;
        else if ( region.getLowerRight().x >= this.originalMapWidth )
            region.getLowerRight().x = this.originalMapWidth;
        if ( region.getLowerRight().y < 0 )
            region.getLowerRight().y = 0;
        else if ( region.getLowerRight().y >= this.originalMapHeight )
            region.getLowerRight().y = this.originalMapHeight;

        // Increase the render version to prevent old requests from rendering
        this.renderVersion++
        this.context = context;
        this.contextTransform = context.getTransform();

        // Figure out the size of what we are drawing
        let m = this.originalMapWidth * scale
        let zoomLevel = Math.ceil(Math.log(m / this.tileWidth) / Math.log(2))
        if (zoomLevel < 0)
            zoomLevel = 0;
        if (zoomLevel > this.maxZoom)
            zoomLevel = this.maxZoom;
        let numTilesAcross = 2 ** zoomLevel;
        this.scaledTileWidth = m / numTilesAcross;
        let originalImageTileWidth = this.originalMapWidth / numTilesAcross
        let originalImageTileHeight = this.originalMapHeight / numTilesAcross

        // Ensure we aren't looping too much
        let startX = Math.floor(region.getUpperLeft().x / originalImageTileWidth);
        let endX = Math.ceil(region.getLowerRight().x / originalImageTileWidth);
        let startY = Math.floor(region.getUpperLeft().y / originalImageTileHeight);
        let endY = Math.ceil(region.getLowerRight().y / originalImageTileHeight)

        if (endX - startX > 20 || endY - startY > 20)
            return;

        // Get all the images to draw
        for (let x = startX; x < endX; x++ ) {
            for (let y = startY; y < endY; y++ ) {
                // Get the tile image and request it be drawn
                this.tileProvider.retrieveTile(this.mapName, this.mapVersion, zoomLevel, new Point2d(x,y), 
                    this.tileDimensionPercentage, this, this.renderVersion);
            }
        }
    }

    public moveOffscreen(): void {
        this.context = null;
    }
}
