
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
    private allConditions: Heap<AirportInformation> | undefined;
    private returnConditionList: Array<Condition> | undefined;
    private buffer: PointGeo | undefined;
    private assigner: ((condition: Condition, info: AirportInformation) => boolean) | undefined;
    
    constructor(cache: Cache, category: string) {
        this.cache = cache;
        this.remainingFiles = 0;

        let comparitor = this.getComparitorForCategory(category);
        if ( comparitor !== undefined ) {
            this.allConditions = new Heap<AirportInformation>(comparitor);
        }

        this.assigner = this.getCorrectAssigner(category);
    }

    public compileConditions(region: BoxGeo, buffer: PointGeo): void {
        if ( this.allConditions === undefined || this.assigner === undefined ) {
            return;
        }

        let cells = this.getCellsForRegion(region);
        let cellsUpperLeft = cells.getUpperLeft();
        let cellsLowerRight = cells.getLowerRight();
        this.remainingFiles = (cellsLowerRight.x - cellsUpperLeft.x + 1) * (cellsLowerRight.y - cellsUpperLeft.y + 1);
        this.allConditions.clear();
        this.buffer = buffer;
        for ( let i = cellsUpperLeft.x; i <= cellsLowerRight.x; i++ ) {
            for ( let j = cellsUpperLeft.y; j <= cellsLowerRight.y; j++ ) {
                EasyAwait.instance.startThread("ConditionCompiler.compileConditions");
                this.cache.retrieveFile(`${i}_${j}.json`, (data: Array<AirportInformation>) => {
                    this.receiveFile(data, region);
                    EasyAwait.instance.endThread("ConditionCompiler.compileConditions");
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
        if ( this.allConditions === undefined || this.assigner === undefined ) {
            return;
        }

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
        if ( this.buffer === undefined || this.allConditions === undefined || this.assigner === undefined ) {
            return;
        }
        while ( !this.allConditions.isEmpty ) {
            let info = this.allConditions.pop();
            if ( info !== undefined && info.latitude !== undefined && info.longitude !== undefined ) {
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
                    if ( this.assigner(condition, info) ) {
                        this.returnConditionList.push(condition);
                    }
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

    /**
     * Heap Comparitors
     */

    private getComparitorForCategory(category: string): ((a: AirportInformation, b: AirportInformation) => number) | undefined {
        switch( category ) {
            case "ceiling": {
                return this.getCeilingComparitor();
            }
            case "visibility": {
                return this.getVisibilityComparitor();
            }
            case "cloudCover": {
                return this.getCloudCoverComparitor();
            }
            case "wind": {
                return this.getWindComparitor();
            }
            case "temperatureCelcius": {
                return this.getTemperatureCelciusComparitor();
            }
            case "dewpointCelcius": {
                return this.getDewpointCelciusComparitor();
            }
            case "flightCategory": {
                return this.getFlightCategoryComparitor();
            }
            default: {
                EasyAwait.instance.reportFatalError("Invalid category.  Not comparitor.");
                return undefined;
            }
        }
    }

    private getCeilingComparitor(): ((a: AirportInformation, b: AirportInformation) => number) {
        return (a: AirportInformation, b: AirportInformation) => {
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
        }
    }

    private getVisibilityComparitor(): ((a: AirportInformation, b: AirportInformation) => number) {
        return (a: AirportInformation, b: AirportInformation) => {
            if ( a.visibility === undefined  ) {
                if ( b.visibility === undefined ) {
                    return 0;
                } else {
                    return 1;
                }
            } else if ( b.visibility === undefined ) {
                return -1;
            }
            return a.visibility.valueOf() - b.visibility.valueOf();
        }
    }

    private getCloudCoverComparitor(): ((a: AirportInformation, b: AirportInformation) => number) {
        return (a: AirportInformation, b: AirportInformation) => {
            if ( a.cloudCover === undefined  ) {
                if ( b.cloudCover === undefined ) {
                    return 0;
                } else {
                    return 1;
                }
            } else if ( b.cloudCover === undefined ) {
                return -1;
            }
            return this.getSkyCoverId(b.cloudCover) - this.getSkyCoverId(a.cloudCover);
        }
    }

    private getWindComparitor(): ((a: AirportInformation, b: AirportInformation) => number) {
        return (a: AirportInformation, b: AirportInformation) => {
            if ( a.windSpeed === undefined  ) {
                if ( b.windSpeed === undefined ) {
                    return 0;
                } else {
                    return 1;
                }
            } else if ( b.windSpeed === undefined ) {
                return -1;
            }
            let aSpeed = a.windSpeed;
            let bSpeed = b.windSpeed;
            if ( a.windGust !== undefined ) {
                aSpeed = a.windGust;
            }
            if ( b.windGust !== undefined ) {
                bSpeed = b.windGust;
            }
            return bSpeed - aSpeed;
        }
    }

    private getTemperatureCelciusComparitor(): ((a: AirportInformation, b: AirportInformation) => number) {
        return (a: AirportInformation, b: AirportInformation) => {
            if ( a.temperatureCelcius === undefined  ) {
                if ( b.temperatureCelcius === undefined ) {
                    return 0;
                } else {
                    return 1;
                }
            } else if ( b.temperatureCelcius === undefined ) {
                return -1;
            }
            return a.temperatureCelcius.valueOf() - b.temperatureCelcius.valueOf();
        }
    }

    private getDewpointCelciusComparitor(): ((a: AirportInformation, b: AirportInformation) => number) {
        return (a: AirportInformation, b: AirportInformation) => {
            if ( a.dewpointCelcius === undefined  ) {
                if ( b.dewpointCelcius === undefined ) {
                    return 0;
                } else {
                    return 1;
                }
            } else if ( b.dewpointCelcius === undefined ) {
                return -1;
            }
            return a.dewpointCelcius.valueOf() - b.dewpointCelcius.valueOf();
        }
    }

    private getFlightCategoryComparitor(): ((a: AirportInformation, b: AirportInformation) => number) {
        return (a: AirportInformation, b: AirportInformation) => {
            if ( a.flightCategory === undefined  ) {
                if ( b.flightCategory === undefined ) {
                    return 0;
                } else {
                    return 1;
                }
            } else if ( b.flightCategory === undefined ) {
                return -1;
            }
            return this.getFlightCategoryId(b.flightCategory) - this.getFlightCategoryId(a.flightCategory);
        }
    }

    /**
     * Value assigners
     */
    private getCorrectAssigner(category: string): (((condition: Condition, info: AirportInformation) => boolean) | undefined) {
        switch ( category ) {
            case "ceiling": {
                return this.assignValueCeiling;
            }
            case "visibility": {
                return this.assignValueVisibility;
            }
            case "cloudCover": {
                return this.assignValueCloudCover;
            }
            case "wind": {
                return this.assignValueWind;
            }
            case "temperatureCelcius": {
                return this.assignValueTemperatureCelcius;
            }
            case "dewpointCelcius": {
                return this.assignValueDewpointCelcius;
            }
            case "flightCategory": {
                return this.assignValueFlightCategory;
            }
            default: {
                EasyAwait.instance.reportFatalError("Invalid category.  No assigner.");
                return undefined;
            }
        }
    }

    private assignValueCeiling(condition: Condition, info: AirportInformation): boolean {
        if ( info.ceiling === undefined ) {
            return false;
        }
        condition.ceiling = info.ceiling
        return true;
    }

    private assignValueVisibility(condition: Condition, info: AirportInformation): boolean {
        if ( info.visibility === undefined ) {
            return false;
        }
        condition.visibility = info.visibility
        return true;
    }

    private assignValueCloudCover(condition: Condition, info: AirportInformation): boolean {
        if ( info.cloudCover === undefined ) {
            return false;
        }
        condition.cloudCover = info.cloudCover
        return true;
    }

    private assignValueWind(condition: Condition, info: AirportInformation): boolean {
        if ( info.windDirection === undefined || info.windGust === undefined || info.windSpeed === undefined ) {
            return false;
        }
        condition.windDirection = info.windDirection
        condition.windGust = info.windGust
        condition.windSpeed = info.windSpeed
        return true;
    }

    private assignValueTemperatureCelcius(condition: Condition, info: AirportInformation): boolean {
        if ( info.temperatureCelcius === undefined ) {
            return false;
        }
        condition.temperatureCelcius = Math.min(info.temperatureCelcius);
        return true;
    }

    private assignValueDewpointCelcius(condition: Condition, info: AirportInformation): boolean {
        if ( info.dewpointCelcius === undefined ) {
            return false;
        }
        condition.dewpointCelcius = Math.min(info.dewpointCelcius);
        return true;
    }

    private assignValueFlightCategory(condition: Condition, info: AirportInformation): boolean {
        if ( info.flightCategory === undefined ) {
            return false;
        }
        condition.flightCategory = info.flightCategory
        return true;
    }

    /**
     * Other helper functions
     */

    private getSkyCoverId(a: string): number {
        if ( a == "CLR" ) {
            return 0
        } else if ( a == "FEW" ) {
            return 1
        } else if ( a == "SCT" ) {
            return 2
        } else if ( a == "BKN" ) {
            return 3
        } else if ( a == "OVC" ) {
            return 4
        } else if ( a == "OVX" ) {
            return 5
        } else {
            EasyAwait.instance.reportFatalError(`Found unknown sky cover: ${a}`);
            return 0;
        }
    }

    private getFlightCategoryId(a: string): number {
        if ( a == "VFR" ) {
            return 0
        } else if ( a == "MVFR" ) {
            return 1
        } else if ( a == "IFR" ) {
            return 2
        } else if ( a == "LIFR" ) {
            return 3
        } else {
            EasyAwait.instance.reportFatalError(`Found unknown flight category: ${a}`);
            return 0;
        }
    }
}