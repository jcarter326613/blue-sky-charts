
import { AirportInformation } from "./airport-cache/airport-information";
import { Cache } from "./airport-cache/cache"
import { Condition } from "./condition";
import { BoxGeo, CoordinateConversion, PointGeo, PointWebMercator, Box2d } from "coordinates";
import { Heap } from 'ts-heap'
import { EasyAwait } from "./easy-await";

export class ConditionCompiler {
    private static readonly airportInformationZoomLevel = 2;
    private cache: Cache;
    private remainingFiles: number;
    private allConditions: Heap<AirportInformation>;
    private returnConditionList: Array<Condition> | undefined;
    private buffer: PointGeo | undefined;
    
    constructor(cache: Cache) {
        this.cache = cache;
        this.remainingFiles = 0;

        this.allConditions = new Heap<AirportInformation>((a, b) => {
            if ( a.ceiling === undefined  ) {
                if ( b.ceiling === undefined ) {
                    return 0;
                } else {
                    return 1;
                }
            } else if ( b.ceiling === undefined ) {
                return -1;
            }
            return a.ceiling.valueOf() - b.ceiling.valueOf();
        })
    }

    public compileConditions(region: BoxGeo, buffer: PointGeo): void {
        let cells = this.getCellsForRegion(region);
        let cellsUpperLeft = cells.getUpperLeft();
        let cellsLowerRight = cells.getLowerRight();
        this.remainingFiles = (cellsLowerRight.x - cellsUpperLeft.x + 1) * (cellsLowerRight.y - cellsUpperLeft.y + 1);
        this.allConditions.clear();
        this.buffer = buffer;
        for ( let i = cellsUpperLeft.x; i <= cellsLowerRight.x; i++ ) {
            for ( let j = cellsUpperLeft.y; j <= cellsLowerRight.y; j++ ) {
                EasyAwait.instance.startThread();
                this.cache.retrieveFile(`${i}_${j}.json`, (data: Array<AirportInformation>) => {
                    this.receiveFile(data, region);
                    EasyAwait.instance.endThread();
                });
            }
        } 
    }

    public getConditions(): Array<Condition> {
        if ( this.returnConditionList !== undefined ) {
            return this.returnConditionList;
        }
        return new Array<Condition>();
    }

    private receiveFile(data: Array<AirportInformation>, region: BoxGeo): void {
        this.remainingFiles--;
        for ( let info of data ) {
            if ( info.longitude === undefined || info.latitude === undefined ) {
                continue;
            }
            let tl = region.getTopLeft();
            let br = region.getBottomRight();
            if ( info.longitude.valueOf() >= tl.longitude.valueOf() && 
                info.longitude.valueOf() <= br.longitude.valueOf() &&
                info.latitude.valueOf() <= tl.latitude.valueOf() && 
                info.latitude.valueOf() >= br.latitude.valueOf() ) {
                this.allConditions.add(info);
            }
        }

        if (this.remainingFiles == 0) {
            this.siftConditions();
        }
    }

    private siftConditions(): void {
        this.returnConditionList = new Array<Condition>();
        if ( this.buffer === undefined ) {
            return;
        }
        while ( !this.allConditions.isEmpty ) {
            let info = this.allConditions.pop();
            if ( info !== undefined && info.latitude !== undefined && info.longitude !== undefined && 
                info.ceiling !== undefined ) {
                let conflictFound = false;
                for ( let priorCondition of this.returnConditionList ) {
                    if ( priorCondition.latitude - this.buffer.latitude < info.latitude && 
                        parseFloat(priorCondition.latitude.toString()) + parseFloat(this.buffer.latitude.toString()) > info.latitude &&
                        priorCondition.longitude - this.buffer.longitude < info.longitude &&
                        parseFloat(priorCondition.longitude.toString()) + parseFloat(this.buffer.longitude.toString()) > info.longitude ) {

                        conflictFound = true;
                        break;
                    }
                }

                if ( !conflictFound ) {
                    let condition = new Condition();
                    condition.latitude = info.latitude;
                    condition.longitude = info.longitude;
                    condition.value = info.ceiling.toString();
                    this.returnConditionList.push(condition);
                }
            }
        }
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