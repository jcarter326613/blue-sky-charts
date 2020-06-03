/**
 * Here's what we are going to do.  Create a lambda function which can respond to rest requests that returns a json similar to the one
 * being injested here.  This control will over request a region and not request a new region until we leave those bounds.  We will have to bucket
 * that request somehow and cache responses.  Bucketing is to allow cache hits.
 * The parent map will tell us when a mouse is "down."
 */

import { BoxGeoModel } from "../models/box-geo-model"
import { Box2d, BoxGeo, BoxWebMercator, Point2d, PointGeo, PointWebMercator, CoordinateConversion } from "coordinates"
import { ISubMapView } from "./i-sub-map-view"
import { IDataReceiver } from "../resources/i-data-receiver"
import { SubMapModel } from "../models/sub-map-model"
import { DataProvider } from "../resources/data-provider"
import { OverlayTypes } from "./overlay-types"

export class MapDataView implements ISubMapView, IDataReceiver {
    // Metadata
    private tileWidth: number;
    private tileHeight: number;

    // Rendering
    private dataProvider: DataProvider;
    private context: CanvasRenderingContext2D | undefined;
    private contextTransform: DOMMatrix | undefined;
    private contextScale: number | undefined;
    private contextRegion: Box2d | undefined;
    private isDisposed: boolean;
    private overlayType: OverlayTypes;

    constructor(dataProvider: DataProvider, type: OverlayTypes) {
        this.dataProvider = dataProvider;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.isDisposed = false;
        this.overlayType = type;
    }

    public initialize(): BoxWebMercator | undefined {
        if ( this.overlayType == OverlayTypes.None ) {
            return undefined;
        }

        let tilesAcross = 2 ** 2;
        this.tileWidth = PointWebMercator.MAX_X_MERCATOR / tilesAcross;
        this.tileHeight = PointWebMercator.MAX_Y_MERCATOR / tilesAcross;

        return new BoxWebMercator(0, 0, PointWebMercator.MAX_X_MERCATOR, PointWebMercator.MAX_Y_MERCATOR);
    }

    public dispose(): void {
        this.isDisposed = true;
    }

    public getOriginalWidth(): number {
        return PointWebMercator.MAX_X_MERCATOR;
    }

    public getOriginalHeight(): number {
        return PointWebMercator.MAX_Y_MERCATOR;
    }

    public receiveData(location: PointWebMercator, data: any): void {
        if ( this.isDisposed || this.context === undefined || this.contextTransform === undefined || this.contextRegion === undefined ) {
            return;
        }

        if ( location.x < this.contextRegion.getUpperLeft().x || location.x > this.contextRegion.getLowerRight().x ||
            location.y < this.contextRegion.getUpperLeft().y || location.y > this.contextRegion.getLowerRight().y ) {
            return;
        }

        let currentTransform = this.context.getTransform();
        this.context.setTransform(this.contextTransform);
        switch ( this.overlayType ) {
            case OverlayTypes.Ceiling: {
                this.renderCeiling(location, data);
                break;
            }
            default: {
                console.error("Request to render unknown type.");
            }
        }
        this.context.setTransform(currentTransform);
    }

    private renderCeiling(location: PointWebMercator, data: any): void {
        if ( this.context === undefined || this.contextScale === undefined || data.ceiling === undefined ) {
            return;
        }

        let visibleCeiling = (parseInt(data.ceiling) / 100).toString();
        let lineHeight = this.context.measureText('M').width * 1.2;
        let textDimensions = this.context.measureText(visibleCeiling);
        let textRect = new Box2d(location.x * this.contextScale - textDimensions.width / 2, location.y * this.contextScale - lineHeight / 2,
            location.x * this.contextScale + textDimensions.width / 2, location.y * this.contextScale + lineHeight / 2);
        this.context.strokeStyle = "rgb(0,0,0)";
        this.context.fillStyle = "rgb(255,255,255)";
        this.context.fillRect(textRect.getUpperLeft().x - 3, textRect.getUpperLeft().y - 3, 
            textRect.getDimensions().x + 6, textRect.getDimensions().y + 6);
        this.context.strokeRect(textRect.getUpperLeft().x - 3, textRect.getUpperLeft().y - 3, 
            textRect.getDimensions().x + 6, textRect.getDimensions().y + 6);

        this.context.strokeText(visibleCeiling, textRect.getUpperLeft().x, textRect.getLowerRight().y);
    }

    public render(context: CanvasRenderingContext2D, region: Box2d, scale: number): void {       
        if ( this.isDisposed ) {
            return;
        }
        this.context = context;
        this.contextTransform = context.getTransform();
        this.contextScale = scale;
        this.contextRegion = region;

        let pixelsAcross = region.getDimensions().x * scale;
        let longitudeAcross = 360 * region.getDimensions().x / this.getOriginalWidth();
        let pixelsAcrossBuffer = this.context.measureText('0').width * 5
        let longitudeBuffer = longitudeAcross * pixelsAcrossBuffer / pixelsAcross
        let latitudeBuffer = longitudeBuffer * 0.6
        
        this.dataProvider.retrieveTile(CoordinateConversion.convertBox2dToBoxGeo(region), new PointGeo(longitudeBuffer, latitudeBuffer), 
            this.overlayType, this);
    }

    public moveOffscreen(): void {
        this.context = undefined;
    }
}