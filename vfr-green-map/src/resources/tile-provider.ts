import { CachedProvider, CachedProviderRequest } from './cached-provider'
import { Point2d, Box2d } from "coordinates"
import { ITileReceiver } from "./i-tile-receiver";

export class TileProvider extends CachedProvider {
    private mapRoot: string;

    constructor(mapRoot: string) {
        super();
        this.mapRoot = mapRoot;
    }

    public retrieveTile(mapName: string, mapVersion: string, zoomLevel: number, location: Point2d, tileDimensions: Point2d ,
            receiver: ITileReceiver, data: any): void {
        let key = this.createKey(mapName, zoomLevel, location);
        let tileRequest = this.getExistingRequest(key);
        
        if ( tileRequest !== undefined && !tileRequest.isInError() ) {
            let tileRequestScoped = tileRequest as TileRequest;
            if (tileRequest.isLoaded()) {
                let image = tileRequestScoped.getImage();
                if ( image != null ) {
                    let region = new Box2d(0, 0, tileDimensions.x, tileDimensions.y);
                    receiver.receiveTile(location, region, image, data, true);
                }
            } else {
                tileRequestScoped.setReceiver(receiver, data);
                this.findTemporaryTile(mapName, zoomLevel, location, receiver, data);
            }
        } else {
            let url = `${this.mapRoot}/${mapName}_SEC_${mapVersion}/${zoomLevel}/${location.x}_${location.y}.png`;
            let newRequest = new TileRequest(this, receiver, location, tileDimensions, data, url);
            this.addRequestToQueue(key, newRequest);
            this.findTemporaryTile(mapName, zoomLevel, location, receiver, data);
        }
    }

    private createKey(mapName: string, zoomLevel: number, location: Point2d): string {
        return `${mapName}|${zoomLevel}|${location.x}_${location.y}`;
    }

    private findTemporaryTile(mapName: string, zoomLevel: number, requestLocation: Point2d, receiver: ITileReceiver, data: any): void {
        let divisor = 1;
        for ( let i = zoomLevel - 1; i >= 0; i-- ) {
            // Get the new cell that we want in the new zoom level
            divisor *= 2;
            let zoomLocation: Point2d = new Point2d(Math.floor(requestLocation.x / divisor), Math.floor(requestLocation.y / divisor));

            // See if that cell is available and draw it if it is
            let key = this.createKey(mapName, i, zoomLocation);
            let cachedRequest = this.getExistingRequest(key);
            if ( cachedRequest != undefined && cachedRequest.isLoaded() ) {
                // Get the bounds of that cell in the original zoom cells
                let originalZoomZoomLocation = new Point2d(zoomLocation.x * divisor, zoomLocation.y * divisor);
                let subsectionX = (requestLocation.x - originalZoomZoomLocation.x) / divisor;
                let subsectionY = (requestLocation.y - originalZoomZoomLocation.y) / divisor;
                let subsection = new Box2d(subsectionX, subsectionY, subsectionX + 1 / divisor, subsectionY + 1 / divisor);

                // Get the image and draw it
                let image = (cachedRequest as TileRequest).getImage();
                if ( image != null ) {
                    receiver.receiveTile(requestLocation, subsection, image, data, true);
                    return;
                }
            }
            //requestLocation = zoomLocation;
        }

        return;
    }
}

class TileRequest extends CachedProviderRequest {
    private tileImage: HTMLImageElement | null;
    private receiver: ITileReceiver | null;
    private location: Point2d | null;
    private dimensions: Point2d | null;
    private data: any | null;
    private url: string;

    constructor(provider: TileProvider, receiver: ITileReceiver, location: Point2d, dimensions: Point2d, 
        data: any, url: string) {
        super(provider);
        this.tileImage = null;
        this.receiver = receiver;
        this.location = location;
        this.dimensions = dimensions;
        this.data = data;
        this.url = url;
    }

    public sendRequest(): void {
        this.tileImage = new Image();
        this.tileImage.addEventListener("load", (event) => {
            let success = this.tileImage !== null && this.tileImage.complete && this.tileImage.naturalWidth > 1;
            this.completeRequest(success);
        });
        this.tileImage.src = this.url;
    }

    public broadcastData(): void {
        if (this.isLoaded() && !this.isInError() && 
            this.receiver != null && this.location != null && this.tileImage != null && this.dimensions != null) {

            let region = new Box2d(0, 0, this.dimensions.x, this.dimensions.y);
            this.receiver.receiveTile(this.location, region, this.tileImage, this.data, false);
            this.receiver = null;
            this.location = null;
            this.data = null;
        }
    }

    public setReceiver(receiver: ITileReceiver, data: any) {
        this.receiver = receiver;
        this.data = data;
    }

    public getImage(): HTMLImageElement | null {
        return this.tileImage;
    }
}
