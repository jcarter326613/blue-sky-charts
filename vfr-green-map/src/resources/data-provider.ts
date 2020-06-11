import * as $ from 'jquery'
import { CachedProvider, CachedProviderRequest } from './cached-provider'
import { Point2d, PointWebMercator, PointGeo, CoordinateConversion, BoxGeo } from "coordinates"
import { IDataReceiver } from "./i-data-receiver";
import { OverlayTypes } from '../view/overlay-types'

export class DataProvider extends CachedProvider {
    private static readonly AREA_BUCKET_MULTIPLIER = 2;
    private static readonly REQUEST_DELAY_MILLISECONDS = 700;
    private usedBuckets: Record<OverlayTypes, Record<string, Array<BoxGeo>>>; //<overlay_type, <resolution as string, box geos>>

    constructor() {
        super(DataProvider.REQUEST_DELAY_MILLISECONDS);
        this.usedBuckets = {
            0: {},
            1: {},
            2: {}, 
            3: {},
            4: {},
            5: {},
            6: {},
            7: {}
        };
    }

    public retrieveTile(area: BoxGeo, resolution: PointGeo, type: OverlayTypes, receiver: IDataReceiver): void {
        let resolutionBucket = this.createResolutionBucket(resolution);
        let areaBucket = this.createAreaBucket(area, resolutionBucket, type);
        let key = this.getCacheKey(areaBucket, resolutionBucket, type);
        let tileRequest = this.getExistingRequest(key) as DataRequest;

        if ( tileRequest !== undefined && !tileRequest.isInError() && !tileRequest.isExpired() ) {
            let tileRequestScoped = tileRequest as DataRequest;
            if (tileRequest.isLoaded()) {
                tileRequestScoped.setReceiver(receiver);
                tileRequestScoped.broadcastData(true);
            } else {
                tileRequestScoped.setReceiver(receiver);
                //this.findTemporaryData(mapName, zoomLevel, location, receiver, data);
            }
        } else {
            let newRequest = new DataRequest(this, receiver, areaBucket, resolutionBucket, type);
            this.addRequestToQueue(key, newRequest);
            //this.findTemporaryData(mapName, zoomLevel, location, receiver, data);
        }
    }

    private createResolutionBucket(resolution: PointGeo): PointGeo {
        let x = Math.log(resolution.longitude) / Math.log(2)
        x = Math.ceil(x / 2) * 2
        let y = Math.log(resolution.latitude) / Math.log(2)
        y = Math.ceil(y / 2) * 2

        return new PointGeo(2 ** x, 2 ** y);
    }

    private createAreaBucket(area: BoxGeo, resolution: PointGeo, type: OverlayTypes): BoxGeo {
        // Check if the area exists in a used bucket
        let resolutionKey = this.getResolutionKey(resolution);
        if ( !(resolutionKey in this.usedBuckets[type]) ) {
            this.usedBuckets[type][resolutionKey] = new Array<BoxGeo>();
        }
        let areaTl = area.getTopLeft();
        let areaBr = area.getBottomRight();
        for ( let obj of this.usedBuckets[type][resolutionKey] ) {
            let tl = obj.getTopLeft();
            let br = obj.getBottomRight();
            if ( tl.longitude <= areaTl.longitude && tl.latitude >= areaTl.latitude &&
                br.longitude >= areaBr.longitude && br.latitude <= areaBr.latitude ) {
                return obj;
            }
        }

        // We need to create a new bucket
        let areaDimensions = area.getDimensions();
        let tlLongitude = areaTl.longitude - (areaDimensions.longitude * DataProvider.AREA_BUCKET_MULTIPLIER);
        let tlLatitude = areaTl.latitude + (areaDimensions.latitude * DataProvider.AREA_BUCKET_MULTIPLIER);
        let brLongitude = areaBr.longitude + (areaDimensions.longitude * DataProvider.AREA_BUCKET_MULTIPLIER);
        let brLatitude = areaBr.latitude - (areaDimensions.latitude * DataProvider.AREA_BUCKET_MULTIPLIER);
        let bucket = new BoxGeo(new PointGeo(this.forceValidLongitude(tlLongitude), this.forceValidLatitude(tlLatitude)),
            new PointGeo(this.forceValidLongitude(brLongitude), this.forceValidLatitude(brLatitude)));
        this.usedBuckets[type][resolutionKey].push(bucket);
        return bucket;
    }

    private forceValidLongitude(longitude: number): number {
        if ( longitude < -180 ) {
            return -180;
        }
        if ( longitude > 180 ) {
            return 180;
        }
        return longitude;
    }

    private forceValidLatitude(latitude: number): number {
        if ( latitude < -90 ) {
            return -90;
        }
        if ( latitude > 90 ) {
            return 90;
        }
        return latitude;
    }

    private getResolutionKey(resolution: PointGeo): string {
        return `${resolution.longitude}|${resolution.latitude}`;
    }

    private getCacheKey(area: BoxGeo, resolution: PointGeo, type: OverlayTypes): string {
        let tl = area.getTopLeft();
        let br = area.getBottomRight();
        return `${type}|${tl.longitude}|${tl.latitude}|${br.longitude}|${br.latitude}|${resolution.longitude}|${resolution.latitude}`
    }
}

class DataRequest extends CachedProviderRequest {
    private data: any | undefined;
    private timeReceived: number;
    private oldestDataAgeAtRetrievalSeconds: number | undefined;
    private receiver: IDataReceiver | undefined;
    private area: BoxGeo;
    private resolution: PointGeo;
    private type: OverlayTypes;
    private static readonly URL_BASE = "https://api.blueskycharts.com/condition-v2/getConditions";
    private static readonly MAX_AGE_MILLISECONDS = 5 * 60 * 1000;

    constructor(provider: DataProvider, receiver: IDataReceiver, area: BoxGeo, resolution: PointGeo, type: OverlayTypes) {
        super(provider);
        this.receiver = receiver;
        this.area = area;
        this.resolution = resolution;
        this.type = type;
        this.timeReceived = 0;
    }

    public isExpired(): boolean {
        if ( this.isLoaded() ) {
            let now = (new Date()).getTime();
            return now - this.timeReceived > DataRequest.MAX_AGE_MILLISECONDS;
        } else {
            return false;
        }
    }
    
    public sendRequest(): void {
        let tl = this.area.getTopLeft();
        let br = this.area.getBottomRight();
        let url = `${DataRequest.URL_BASE}?startLongitude=${tl.longitude}&endLongitude=${br.longitude}&startLatitude=${br.latitude}&endLatitude=${tl.latitude}&bufferLongitude=${this.resolution.longitude}&bufferLatitude=${this.resolution.latitude}&information=${this.getInformationForType(this.type)}`;
        $.ajax({
            url: url,
            type: "GET",
            dataType: "json",
            crossDomain: true,
            cache: false,
            success: (data) => {
                try {
                    this.data = data;
                    this.timeReceived = (new Date()).getTime();
                    if ( this.data !== undefined && this.data.oldestDataAgeSeconds !== undefined ) {
                        this.oldestDataAgeAtRetrievalSeconds = this.data.oldestDataAgeSeconds
                    }
                    this.completeRequest(true);
                } catch {
                    this.completeRequest(false);
                    console.error(`Could not parse data from ${url}`);
                }
            },
            error: (response) => {
                this.completeRequest(false);
                console.error(`Could not load data from ${url}`);
            }});
    }

    public setReceiver(receiver: IDataReceiver) {
        this.receiver = receiver;
    }

    public broadcastData(immediate: boolean) {
        if ( this.data === undefined || this.oldestDataAgeAtRetrievalSeconds === undefined || this.data.conditions === undefined || 
            this.receiver == undefined ) {
            return;
        }

        let secondsSinceRequest = Math.ceil(((new Date()).getTime() - this.timeReceived) / 1000);
        let dataAgeSeconds = this.oldestDataAgeAtRetrievalSeconds + secondsSinceRequest;
        for ( let data of this.data.conditions ) {
            if (data.longitude === undefined || data.latitude === undefined) {
                continue;
            }
            let geoLocation = new PointGeo(data.longitude, data.latitude);
            let mercatorLocation = CoordinateConversion.convertToWebMercator(geoLocation);

            this.receiver.receiveData(mercatorLocation, data, dataAgeSeconds, immediate);
        }
    }
    
    private getInformationForType(type: OverlayTypes): string {
        switch( type ) {
            case OverlayTypes.Category: {
                return "flightCategory"
            }
            case OverlayTypes.Ceiling: {
                return "ceiling"
            }
            case OverlayTypes.CloudCover: {
                return "cloudCover"
            }
            case OverlayTypes.DewpointSpreadC: {
                return "dewpointSpreadCelcius"
            }
            case OverlayTypes.TempC: {
                return "temperatureCelcius"
            }
            case OverlayTypes.Visibility: {
                return "visibility"
            }
            case OverlayTypes.Wind: {
                return "wind"
            }
            default: {
                console.error("Invalid overlay type");
                return "";
            }
        }
    }
}
