import { Cache } from "./airport-cache/cache"
import { Condition } from "./condition";
import { BoxGeo, CoordinateConversion, PointGeo, PointWebMercator, Box2d } from "coordinates";
import { AirportInformation } from "./airport-cache/airport-information";

export class ConditionCompiler {
    private static readonly airportInformationZoomLevel = 2;
    private cache: Cache;
    private remainingFiles: number;
    
    constructor(cache: Cache) {
        this.cache = cache;
        this.remainingFiles = 0;
    }

    public compileConditions(region: BoxGeo, buffer: PointGeo): void {
        let cells = this.getCellsForRegion(region);
        let cellsUpperLeft = cells.getUpperLeft();
        let cellsLowerRight = cells.getLowerRight();
        this.remainingFiles = (cellsLowerRight.x - cellsUpperLeft.x + 1) * (cellsLowerRight.y - cellsUpperLeft.y + 1);
        for ( let i = cellsUpperLeft.x; i < cellsLowerRight.x; i++ ) {
            for ( let j = cellsUpperLeft.y; j < cellsLowerRight.y; j++ ) {
                this.cache.retrieveFile(`${i}_${j}.json`, (data: Array<AirportInformation>) => {
                    this.receiveFile(data);
                });
            }
        }
    }

    public getConditions(): Array<Condition> {
        let retList = new Array<Condition>();
        return retList;
    }

    private receiveFile(data: any): void {
        this.remainingFiles--;

        if (this.remainingFiles == 0) {
            this.siftConditions();
        }
    }

    private siftConditions(): void {
        
    }

    private getCellsForRegion(region: BoxGeo): Box2d {
        let mercatorRegion = CoordinateConversion.convertBoxGeoToBoxMercator(region);
        let cellsAcross = 2 ** ConditionCompiler.airportInformationZoomLevel;

        let startCellX = Math.floor( cellsAcross * mercatorRegion.getTopLeft().x / PointWebMercator.MAX_X_MERCATOR )
        if ( startCellX >= cellsAcross ) {
            startCellX = cellsAcross - 1;
        }

        let startCellY = Math.floor( cellsAcross * mercatorRegion.getTopLeft().y / PointWebMercator.MAX_Y_MERCATOR )
        if ( startCellY >= cellsAcross ) {
            startCellY = cellsAcross - 1;
        }

        let endCellX = Math.floor( cellsAcross * mercatorRegion.getBottomRight().x / PointWebMercator.MAX_X_MERCATOR )
        if ( endCellX >= cellsAcross ) {
            endCellX = cellsAcross - 1;
        }

        let endCellY = Math.floor( cellsAcross * mercatorRegion.getBottomRight().y / PointWebMercator.MAX_Y_MERCATOR )
        if ( endCellY >= cellsAcross ) {
            endCellY = cellsAcross - 1;
        }

        return new Box2d(startCellX, startCellY, endCellX, endCellY);
    }

}