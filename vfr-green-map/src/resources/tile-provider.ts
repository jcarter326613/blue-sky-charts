import { Point2d } from "coordinates"
import { ITileReceiver } from "./i-tile-receiver";

export class TileProvider {
    private static readonly MAX_ACTIVE_REQUESTS = 2;
    private tileCache: Record<string, TileRequest>; //Need to add ageoff, causing memory leak
    private mapRoot: string;
    private numActiveRequests: number;
    private requestQueue: Record<string, TileRequest>;

    constructor(mapRoot: string) {
        this.tileCache = {};
        this.mapRoot = mapRoot;
        this.numActiveRequests = 0;
        this.requestQueue = {};
    }

    public retrieveTile(mapName: string, mapVersion: string, zoomLevel: number, location: Point2d, 
            receiver: ITileReceiver, data: any): void {
        let key = this.createKey(mapName, zoomLevel, location);
        let tileRequest: TileRequest;

        if ( key in this.tileCache ) {
            tileRequest = this.tileCache[key];

            if (tileRequest.isLoaded()) {
                let image = tileRequest.getImage();
                if ( image != null ) {
                    receiver.receiveTile(location, image, data);
                }
            } else {
                tileRequest.setReceiver(receiver, data);
            }
        } else {
            let url = `${this.mapRoot}/${mapName}_SEC_${mapVersion}/${zoomLevel}/${location.x}_${location.y}.png`;
            tileRequest = new TileRequest(this, receiver, location, data, url);
            this.addRequestToQueue(key, tileRequest);
        }
    }

    public clearQueue(): void {
        this.requestQueue = {}
    }

    private createKey(mapName: string, zoomLevel: number, location: Point2d): string {
        return `${mapName}|${zoomLevel}|${location.x}_${location.y}`;
    }

    private addRequestToQueue(key: string, request: TileRequest): void {
        if ( this.numActiveRequests < TileProvider.MAX_ACTIVE_REQUESTS ) {
            this.tileCache[key] = request;
            request.sendRequest();
            this.numActiveRequests++;
        } else {
            this.requestQueue[key] = request;
        }
    }

    /**
     * Only to be called by TileRequest class to signify a downoad has completed.
     */
    public completeRequest(): void {
        for ( let i in this.requestQueue ) {
            let request = this.requestQueue[i];
            this.tileCache[i] = request;
            request.sendRequest();
            delete this.requestQueue[i]
            return;
        }

        this.numActiveRequests--;
    }
}

class TileRequest {
    private loaded: boolean;
    private tileImage: HTMLImageElement | null;
    private receiver: ITileReceiver | null;
    private provider: TileProvider;
    private location: Point2d | null;
    private data: any | null;
    private url: string;

    constructor(provider: TileProvider, receiver: ITileReceiver, location: Point2d, data: any, url: string) {
        this.loaded = false;
        this.tileImage = null;
        this.receiver = receiver;
        this.provider = provider;
        this.location = location;
        this.data = data;
        this.url = url;
    }

    public sendRequest(): void {
        this.tileImage = new Image();
        this.tileImage.addEventListener("load", () => {
            this.provider.completeRequest();
            this.loaded = true;
            if (this.receiver != null && this.location != null && this.tileImage != null) {
                this.receiver.receiveTile(this.location, this.tileImage, this.data);
                this.receiver = null;
                this.location = null;
                this.data = null;
            }
        });
        this.tileImage.src = this.url;
    }

    public setReceiver(receiver: ITileReceiver, data: any) {
        this.receiver = receiver;
        this.data = data;
    }

    public isLoaded(): boolean {
        return this.loaded;
    }

    public getImage(): HTMLImageElement | null {
        return this.tileImage;
    }
}
