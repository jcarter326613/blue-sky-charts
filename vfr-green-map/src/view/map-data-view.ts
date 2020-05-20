import { BoxGeoModel } from "../models/box-geo-model"
import { Box2d, BoxGeo, BoxWebMercator, Point2d, PointWebMercator, CoordinateConversion } from "coordinates"
import { ISubMapView } from "./i-sub-map-view"
import { IDataReceiver } from "../resources/i-data-receiver"
import { SubMapModel } from "../models/sub-map-model"
import { DataProvider } from "../resources/data-provider"

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

    constructor(dataProvider: DataProvider) {
        this.dataProvider = dataProvider;
        this.tileWidth = 0;
        this.tileHeight = 0;
    }

    public initialize(): BoxGeo | undefined {
        let tilesAcross = 2 ** 2;
        this.tileWidth = PointWebMercator.MAX_X_MERCATOR / tilesAcross;
        this.tileHeight = PointWebMercator.MAX_Y_MERCATOR / tilesAcross;

        let boxMercator = new BoxWebMercator(0, 0, PointWebMercator.MAX_X_MERCATOR, PointWebMercator.MAX_Y_MERCATOR);

        return CoordinateConversion.convertBoxMercatorToBoxGeo(boxMercator);
    }

    public getOriginalWidth(): number {
        return PointWebMercator.MAX_X_MERCATOR;
    }

    public getOriginalHeight(): number {
        return PointWebMercator.MAX_Y_MERCATOR;
    }

    public receiveData(location: PointWebMercator, data: any): void {
        if ( this.context === undefined || this.contextTransform === undefined || this.contextRegion === undefined ||
            this.contextScale === undefined ) {
            return;
        }

        if ( location.x < this.contextRegion.getUpperLeft().x || location.x > this.contextRegion.getLowerRight().x ||
            location.y < this.contextRegion.getUpperLeft().y || location.y > this.contextRegion.getLowerRight().y ) {
            return;
        }

        if ( data.ceiling === undefined ) {
            return;
        }

        let currentTransform = this.context.getTransform();
        this.context.setTransform(this.contextTransform);

        //Write out the ceiling
        let lineHeight = this.context.measureText('M').width * 1.2;
        let textDimensions = this.context.measureText(data.ceiling);
        let textRect = new Box2d(location.x * this.contextScale - textDimensions.width / 2, location.y * this.contextScale - lineHeight / 2,
            location.x * this.contextScale + textDimensions.width / 2, location.y * this.contextScale + lineHeight / 2);
        this.context.strokeStyle = "rgb(0,0,0)";
        this.context.fillStyle = "rgb(255,255,255)";
        this.context.fillRect(textRect.getUpperLeft().x - 3, textRect.getUpperLeft().y - 3, 
            textRect.getDimensions().x + 6, textRect.getDimensions().y + 6);
        this.context.strokeRect(textRect.getUpperLeft().x - 3, textRect.getUpperLeft().y - 3, 
            textRect.getDimensions().x + 6, textRect.getDimensions().y + 6);

        this.context.strokeText(data.ceiling, textRect.getUpperLeft().x, textRect.getLowerRight().y);
        
        this.context.setTransform(currentTransform);
    }

    public render(context: CanvasRenderingContext2D, region: Box2d, scale: number): void {       
        this.context = context;
        this.contextTransform = context.getTransform();
        this.contextScale = scale;
        this.contextRegion = region;

        this.dataProvider.retrieveTile("metar", new Point2d(0, 0), this);
    }
}