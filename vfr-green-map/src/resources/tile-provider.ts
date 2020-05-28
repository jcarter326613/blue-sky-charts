import { Point2d, Box2d } from "coordinates"
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

    public retrieveTile(mapName: string, mapVersion: string, zoomLevel: number, location: Point2d, tileDimensions: Point2d ,
            receiver: ITileReceiver, data: any): void {
        let key = this.createKey(mapName, zoomLevel, location);
        let tileRequest: TileRequest;

        if ( key in this.tileCache ) {
            tileRequest = this.tileCache[key];

            if (tileRequest.isLoaded()) {
                let image = tileRequest.getImage();
                if ( image != null ) {
                    let region = new Box2d(0, 0, tileDimensions.x, tileDimensions.y);
                    receiver.receiveTile(location, region, image, data);
                }
            } else {
                tileRequest.setReceiver(receiver, data);
                this.findTemporaryTile(tileRequest);
            }
        } else {
            let url = `${this.mapRoot}/${mapName}_SEC_${mapVersion}/${zoomLevel}/${location.x}_${location.y}.png`;
            tileRequest = new TileRequest(this, receiver, location, tileDimensions, zoomLevel, mapName, data, url);
            this.addRequestToQueue(key, tileRequest);
            this.findTemporaryTile(tileRequest);
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

    private findTemporaryTile(request: TileRequest): void {
        let requestLocationO = request.getLocation();
        let receiver = request.getReceiver();
        if ( requestLocationO == null || receiver == null ) {
            return;
        }
        let requestLocation = requestLocationO;
        let divisor = 1;
        for ( let i = request.getZoom() - 1; i >= 0; i-- ) {
            // Get the new cell that we want in the new zoom level
            let zoomLocation: Point2d = new Point2d(Math.floor(requestLocation.x / 2), Math.floor(requestLocation.y / 2));
            divisor *= 2;

            // See if that cell is available and draw it if it is
            let key = this.createKey(request.getMapName(), i, zoomLocation);
            if ( key in this.tileCache && this.tileCache[key].isLoaded() ) {
                // Get the bounds of that cell in the original zoom cells
                let originalZoomZoomLocation = new Point2d(zoomLocation.x * divisor, zoomLocation.y * divisor);
                let subsectionX = (requestLocationO.x - originalZoomZoomLocation.x) / divisor;
                let subsectionY = (requestLocationO.y - originalZoomZoomLocation.y) / divisor;
                let subsection = new Box2d(subsectionX, subsectionY, subsectionX + 1 / divisor, subsectionY + 1 / divisor);

                // Get the image and draw it
                let image = this.tileCache[key].getImage();
                if ( image != null ) {
                    receiver.receiveTile(requestLocationO, subsection, image, request.getData());
                    return;
                }
            }
            requestLocation = zoomLocation;
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
    private dimensions: Point2d | null;
    private zoom: number;
    private mapName: string;
    private data: any | null;
    private url: string;

    constructor(provider: TileProvider, receiver: ITileReceiver, location: Point2d, dimensions: Point2d, zoom: number, mapName: string, 
        data: any, url: string) {
        this.loaded = false;
        this.tileImage = null;
        this.receiver = receiver;
        this.provider = provider;
        this.location = location;
        this.dimensions = dimensions;
        this.zoom = zoom;
        this.mapName = mapName;
        this.data = data;
        this.url = url;
    }

    public getZoom(): number {
        return this.zoom;
    }

    public getLocation(): Point2d | null {
        return this.location;
    }

    public getMapName(): string {
        return this.mapName;
    }

    public getReceiver(): ITileReceiver | null {
        return this.receiver;
    }

    public getData(): any {
        return this.data;
    }

    public sendRequest(): void {
        this.tileImage = new Image();
        this.tileImage.addEventListener("load", () => {
            this.provider.completeRequest();
            this.loaded = true;
            if (this.receiver != null && this.location != null && this.tileImage != null && this.dimensions != null) {
                let region = new Box2d(0, 0, this.dimensions.x, this.dimensions.y);
                this.receiver.receiveTile(this.location, region, this.tileImage, this.data);
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
