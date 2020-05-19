import { Point2d } from "coordinates"
import { IDataReceiver } from "./i-data-receiver";

export class DataProvider {
    private dataCache: Record<string, DataRequest>;

    constructor() {
        this.dataCache = {};
    }

    public retrieveTile(dataName: string, zoomLevel: number, location: Point2d, receiver: IDataReceiver): void {
        let key = this.createKey(dataName, zoomLevel, location);
        let dataRequest: DataRequest;

        if ( key in this.dataCache ) {
            dataRequest = this.dataCache[key];

            if (dataRequest.isLoaded())
                receiver.receiveData(location, dataRequest.getData());
            else
                dataRequest.setReceiver(receiver);
        } else {
            let url = `/${dataName}/${zoomLevel}/${location.x}_${location.y}.json`;
            dataRequest = new DataRequest(receiver, location, url);
            this.dataCache[key] = dataRequest
        }
    }

    private createKey(mapName: string, zoomLevel: number, location: Point2d): string {
        return `${mapName}|${zoomLevel}|${location.x}_${location.y}`;
    }
}

class DataRequest {
    private loaded: boolean;
    private data: any | null;
    private receiver: IDataReceiver | null;
    private location: Point2d | null;

    constructor(receiver: IDataReceiver, location: Point2d, url: string) {
        this.loaded = false;
        this.receiver = receiver;
        this.location = location;

        let thisObj = this;

        this.data = null;
        $.getJSON(url)
            .done((data) => {
                thisObj.loaded = true;
                if (thisObj.receiver != null && thisObj.location != null) {
                    thisObj.receiver.receiveData(thisObj.location, thisObj.data);
                    thisObj.receiver = null;
                    thisObj.location = null;
                    thisObj.data = null;
                }
            })
            .fail((response) => {
                console.error(`Could not load data from ${url}`);
            });
    }

    public setReceiver(receiver: IDataReceiver) {
        this.receiver = receiver;
    }

    public isLoaded(): boolean {
        return this.loaded;
    }

    public getData(): any {
        return this.data;
    }
}
