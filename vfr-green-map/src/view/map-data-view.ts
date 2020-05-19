import { BoxGeoModel } from "../models/box-geo-model"
import { Box2d, BoxGeo, Point2d, PointWebMercator } from "coordinates"
import { ISubMapView } from "./i-sub-map-view"
import { IDataReceiver } from "../resources/i-data-receiver"
import { SubMapModel } from "../models/sub-map-model"
import { DataProvider } from "../resources/data-provider"

export class MapDataView implements ISubMapView, IDataReceiver {
    private dataProvider: DataProvider;
    private tileWidth: number;
    private tileHeight: number;
    private name: string;

    constructor(dataProvider: DataProvider) {
        this.dataProvider = dataProvider;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.name = "";
    }

    public initialize(model: SubMapModel, name: string): BoxGeo | undefined {
        if (model.tileWidth == null || model.version == null || model.fileExtent == null)
            return undefined;

        this.tileWidth = model.tileWidth;
        this.tileHeight = model.tileWidth;
        this.name = name;

        return BoxGeoModel.createBoxGeoFromModel(model.fileExtent);
    }

    public getOriginalWidth(): number {
        return PointWebMercator.MAX_X_MERCATOR;
    }

    public getOriginalHeight(): number {
        return PointWebMercator.MAX_Y_MERCATOR;
    }

    public receiveData(location: Point2d, data: any): void {

    }

    public render(context: CanvasRenderingContext2D, region: Box2d, scale: number): void {
        context.strokeStyle = "rgb(0, 255, 0)";
        context.strokeText("Hello world", region.upperLeft.x * scale + 100, region.upperLeft.y * scale + 100);
    }
}