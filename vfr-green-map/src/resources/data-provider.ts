import * as $ from 'jquery'
import { Point2d, PointWebMercator, PointGeo, CoordinateConversion, BoxGeo } from "coordinates"
import { IDataReceiver } from "./i-data-receiver";

export class DataProvider {
    private dataCache: Record<string, DataRequest>;
    private usedBuckets: Record<string, Array<BoxGeo>>; //<resolution as string, box geos>
    private static readonly AREA_BUCKET_MULTIPLIER = 2;

    constructor() {
        this.dataCache = {};
        this.usedBuckets = {};
    }

    public retrieveTile(area: BoxGeo, resolution: PointGeo, receiver: IDataReceiver): void {
        let resolutionBucket = this.createResolutionBucket(resolution);
        let areaBucket = this.createAreaBucket(area, resolutionBucket);
        let cachedData = this.getDataFromCache(areaBucket, resolutionBucket);
        cachedData.setReceiver(receiver);
    }

    private getDataFromCache( area: BoxGeo, resolution: PointGeo ): DataRequest {
        let key = this.getCacheKey(area, resolution);
        if ( key in this.dataCache ) {
            return this.dataCache[key];
        }

        let dataRequest = new DataRequest(area, resolution);
        this.dataCache[key] = dataRequest;
        return dataRequest;
    }

    private createResolutionBucket(resolution: PointGeo): PointGeo {
        let x = Math.log(resolution.longitude) / Math.log(2)
        x = Math.ceil(x / 2) * 2
        let y = Math.log(resolution.latitude) / Math.log(2)
        y = Math.ceil(y / 2) * 2

        return new PointGeo(2 ** x, 2 ** y);
    }

    private createAreaBucket(area: BoxGeo, resolution: PointGeo): BoxGeo {
        // Check if the area exists in a used bucket
        let resolutionKey = this.getResolutionKey(resolution);
        if ( !(resolutionKey in this.usedBuckets) ) {
            this.usedBuckets[resolutionKey] = new Array<BoxGeo>();
        }
        let areaTl = area.getTopLeft();
        let areaBr = area.getBottomRight();
        for ( let obj of this.usedBuckets[resolutionKey] ) {
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
        this.usedBuckets[resolutionKey].push(bucket);
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

    private getCacheKey(area: BoxGeo, resolution: PointGeo): string {
        let tl = area.getTopLeft();
        let br = area.getBottomRight();
        return `${tl.longitude}|${tl.latitude}|${br.longitude}|${br.latitude}|${resolution.longitude}|${resolution.latitude}`
    }
}

class DataRequest {
    private loaded: boolean;
    private data: Array<any> | undefined;
    private receiver: IDataReceiver | undefined;
    private static readonly urlBase = "https://n3aigsadrc.execute-api.us-east-1.amazonaws.com/dev/getConditions";

    constructor(area: BoxGeo, resolution: PointGeo) {
        this.loaded = false;
        let tl = area.getTopLeft();
        let br = area.getBottomRight();
        let url = `${DataRequest.urlBase}?startLongitude=${tl.longitude}&endLongitude=${br.longitude}&startLatitude=${br.latitude}&endLatitude=${tl.latitude}&bufferLongitude=${resolution.longitude}&bufferLatitude=${resolution.latitude}`;

        $.ajax({
            url: url,
            type: "GET",
            dataType: "json",
            crossDomain: true,
            success: (data) => {
                try {
                    this.data = data;
                    this.loaded = true;
                    if (this.receiver != null) {
                        this.broadcastData(this.receiver);
                    }
                } catch {
                    console.error(`Could not parse data from ${url}`);
                }
            },
            error: (response) => {
                console.error(`Could not load data from ${url}`);
            }});
    }

    public setReceiver(receiver: IDataReceiver) {
        if ( this.loaded ) {
            this.broadcastData(receiver);
        } else {
            this.receiver = receiver;
        }
    }

    private broadcastData(receiver: IDataReceiver) {
        if ( this.data === undefined ) {
            return;
        }

        for ( let data of this.data ) {
            if (data.longitude === undefined || data.latitude === undefined) {
                continue;
            }
            let geoLocation = new PointGeo(data.longitude, data.latitude);
            let mercatorLocation = CoordinateConversion.convertToWebMercator(geoLocation);

            receiver.receiveData(mercatorLocation, data);
        }
    }
}
