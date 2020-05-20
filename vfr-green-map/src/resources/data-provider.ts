import * as $ from 'jquery'
import { Point2d, PointWebMercator, PointGeo, CoordinateConversion } from "coordinates"
import { IDataReceiver } from "./i-data-receiver";

export class DataProvider {
    private dataCache: Record<string, DataRequest>;
    private urlBase: string;

    constructor() {
        this.dataCache = {};
        this.urlBase = "https://vfr-green-artifacts-245819277863.s3.amazonaws.com";
    }

    public retrieveTile(dataName: string, location: Point2d, receiver: IDataReceiver): void {
        let key = this.createKey(dataName, location);
        let dataRequest: DataRequest;

        if ( key in this.dataCache ) {
            dataRequest = this.dataCache[key];
            dataRequest.setReceiver(receiver);
        } else {
            let url = `/${dataName}/${location.x}_${location.y}.json`;
            dataRequest = new DataRequest(receiver, this, location, url);
            this.dataCache[key] = dataRequest
        }
    }

    private createKey(mapName: string, location: Point2d): string {
        return `${mapName}|${location.x}_${location.y}`;
    }
}

class DataRequest {
    private loaded: boolean;
    private data: Array<any> | undefined;
    private receiver: IDataReceiver;

    constructor(receiver: IDataReceiver, provider: DataProvider, location: Point2d, url: string) {
        this.loaded = false;
        this.receiver = receiver;

        $.getJSON(url)
            .done((data) => {
                try {
                    this.data = data;
                    this.loaded = true;
                    if (this.receiver != null) {
                        this.broadcastData(this.receiver);
                    }
                } catch {
                    console.error(`Could not parse data from ${url}`);
                }
            })
            .fail((response) => {
                console.error(`Could not load data from ${url}`);
            });
    }

    public setReceiver(receiver: IDataReceiver) {
        if ( this.loaded ) {
            this.broadcastData(receiver);
        } else {
            this.receiver = receiver;
        }
    }

    public isLoaded(): boolean {
        return this.loaded;
    }

    public getData(): any {
        return this.data;
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
